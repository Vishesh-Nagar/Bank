package com.example.bank.service.impl;

import com.example.bank.dto.Login.LoginDto;
import com.example.bank.dto.Login.LoginResponseDto;
import com.example.bank.dto.User.UserCreateDto;
import com.example.bank.dto.User.UserDto;
import com.example.bank.dto.User.UserUpdateDto;
import com.example.bank.entity.RefreshToken;
import com.example.bank.entity.User;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.UserException;
import com.example.bank.mapper.UserMapper;
import com.example.bank.repository.UserRepository;
import com.example.bank.service.AuditEventProducer;
import com.example.bank.service.EmailVerificationService;
import com.example.bank.service.LoginAttemptService;
import com.example.bank.service.RefreshTokenService;
import com.example.bank.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    /** Access token validity: 15 minutes */
    private static final long ACCESS_TOKEN_MS = 15 * 60 * 1_000L;

    private final UserRepository          userRepository;
    private final BCryptPasswordEncoder   passwordEncoder;
    private final String                  jwtSecret;
    private final LoginAttemptService     loginAttemptService;
    private final RefreshTokenService     refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final AuditEventProducer      auditEventProducer;

    public UserServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           @Value("${jwt.secret}") String jwtSecret,
                           LoginAttemptService loginAttemptService,
                           RefreshTokenService refreshTokenService,
                           EmailVerificationService emailVerificationService,
                           AuditEventProducer auditEventProducer) {
        this.userRepository           = userRepository;
        this.passwordEncoder          = passwordEncoder;
        this.jwtSecret                = jwtSecret;
        this.loginAttemptService      = loginAttemptService;
        this.refreshTokenService      = refreshTokenService;
        this.emailVerificationService = emailVerificationService;
        this.auditEventProducer       = auditEventProducer;
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    @Override
    public UserDto createUser(UserCreateDto dto) {
        userRepository.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new UserException(ErrorCode.USERNAME_ALREADY_EXISTS,
                    "Username '" + dto.getUsername() + "' is already taken.");
        });
        userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email '" + dto.getEmail() + "' is already registered.");
        });

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setPasswordVersion((short) 2);
        user.setEmailVerified(true);
        user.setEnabled(true);

        User saved = userRepository.save(user);

        // Send verification email (non-blocking — failure logged, not thrown)
        try {
            emailVerificationService.sendVerificationEmail(saved);
        } catch (Exception e) {
            // Log only — don't fail registration if mail server is unreachable
        }

        auditEventProducer.publish("USER_REGISTERED", saved.getUsername(), "User",
                saved.getId().toString(), "CREATE", null);

        return UserMapper.mapToUserDto(saved);
    }

    // ─── Read ────────────────────────────────────────────────────────────────

    @Override
    public UserDto getUserById(Long id) {
        return UserMapper.mapToUserDto(findOrThrow(id));
    }

    @Override
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND,
                        "User not found with username: " + username));
        return UserMapper.mapToUserDto(user);
    }

    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::mapToUserDto);
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Override
    public UserDto updateUser(Long id, UserUpdateDto dto, String principalName) {
        User user = findOrThrow(id);

        if (!user.getUsername().equals(principalName)) {
            throw new UserException(ErrorCode.ACCESS_DENIED,
                    "You do not have permission to update this user.");
        }

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            userRepository.findByUsername(dto.getUsername()).ifPresent(u -> {
                throw new UserException(ErrorCode.USERNAME_ALREADY_EXISTS,
                        "Username '" + dto.getUsername() + "' is already taken.");
            });
            user.setUsername(dto.getUsername());
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
                throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS,
                        "Email '" + dto.getEmail() + "' is already registered.");
            });
            user.setEmail(dto.getEmail());
            // Require re-verification when email changes
            user.setEmailVerified(false);
            try {
                emailVerificationService.sendVerificationEmail(user);
            } catch (Exception ignored) {}
        }

        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isBlank()) {
                throw new UserException(ErrorCode.VALIDATION_FAILED,
                        "currentPassword is required when setting a new password.");
            }
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new UserException(ErrorCode.INVALID_CREDENTIALS,
                        "Current password is incorrect.");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            user.setPasswordVersion((short) (user.getPasswordVersion() + 1));
            auditEventProducer.publish("PASSWORD_CHANGED", principalName, "User",
                    user.getId().toString(), "UPDATE", null);
        }

        return UserMapper.mapToUserDto(userRepository.save(user));
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Override
    public void deleteUser(Long id) {
        User user = findOrThrow(id);
        userRepository.deleteById(id);
        auditEventProducer.publish("USER_DELETED", user.getUsername(), "User",
                user.getId().toString(), "DELETE", null);
    }

    // ─── Auth ────────────────────────────────────────────────────────────────

    @Override
    public LoginResponseDto login(LoginDto dto, String clientIp) {
        // 1) Brute-force lockout check
        if (loginAttemptService.isLocked(clientIp)) {
            throw new UserException(ErrorCode.RATE_LIMITED,
                    "Too many failed login attempts. Try again in 15 minutes.");
        }

        // 2) Lookup user
        User user = userRepository.findByUsername(dto.getUsername()).orElseGet(() -> {
            loginAttemptService.recordFailure(clientIp);
            auditEventProducer.publish("LOGIN_FAILED", dto.getUsername(), "User", null, "EXECUTE", "Invalid credentials");
            throw new UserException(ErrorCode.INVALID_CREDENTIALS,
                    "Invalid username or password.");
        });

        // 3) Password check
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(clientIp);
            auditEventProducer.publish("LOGIN_FAILED", dto.getUsername(), "User", user.getId().toString(), "EXECUTE", "Invalid credentials");
            throw new UserException(ErrorCode.INVALID_CREDENTIALS,
                    "Invalid username or password.");
        }

        // 4) Account state checks
        if (!user.isEnabled()) {
            throw new UserException(ErrorCode.ACCESS_DENIED,
                    "This account has been disabled. Please contact support.");
        }
        if (user.isAccountLocked()) {
            throw new UserException(ErrorCode.ACCESS_DENIED,
                    "This account is locked. Please contact support.");
        }
        if (!user.isEmailVerified()) {
            throw new UserException(ErrorCode.EMAIL_NOT_VERIFIED,
                    "Please verify your email address before logging in.");
        }

        // 5) Issue tokens
        loginAttemptService.clearFailures(clientIp);
        String accessToken  = generateAccessToken(user);
        RefreshToken rt     = refreshTokenService.createFor(user);

        auditEventProducer.publish("LOGIN_SUCCESS", user.getUsername(), "User",
                user.getId().toString(), "EXECUTE", Map.of("ip", clientIp));

        return new LoginResponseDto(UserMapper.mapToUserDto(user), accessToken, rt.getToken());
    }

    @Override
    public LoginResponseDto refresh(String refreshToken) {
        RefreshToken rt   = refreshTokenService.validate(refreshToken);
        User user         = rt.getUser();
        String newAccess  = generateAccessToken(user);
        // Re-use the same refresh token (don't rotate unless needed)
        return new LoginResponseDto(UserMapper.mapToUserDto(user), newAccess, refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    // ─── Admin ───────────────────────────────────────────────────────────────

    @Override
    public UserDto setAccountLocked(Long id, boolean locked) {
        User user = findOrThrow(id);
        user.setAccountLocked(locked);
        return UserMapper.mapToUserDto(userRepository.save(user));
    }

    // ─── Email Verification ──────────────────────────────────────────────────

    @Override
    public void verifyEmail(String token) {
        emailVerificationService.verifyEmail(token);
    }

    @Override
    public void resendVerificationEmail(String email) {
        emailVerificationService.resendVerificationEmail(email);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND,
                        "User with id " + id + " was not found."));
    }

    private String generateAccessToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_MS);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role",   user.getRole());

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
