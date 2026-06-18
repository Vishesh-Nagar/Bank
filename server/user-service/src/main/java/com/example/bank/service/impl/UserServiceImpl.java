package com.example.bank.service.impl;

import com.example.bank.dto.Login.LoginDto;
import com.example.bank.dto.Login.LoginResponseDto;
import com.example.bank.dto.User.UserCreateDto;
import com.example.bank.dto.User.UserDto;
import com.example.bank.dto.User.UserUpdateDto;
import com.example.bank.entity.User;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.UserException;
import com.example.bank.mapper.UserMapper;
import com.example.bank.repository.UserRepository;
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

    private static final long EXPIRATION_MS = 86_400_000L; // 24 hours

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String jwtSecret;

    public UserServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           @Value("${jwt.secret}") String jwtSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public UserDto createUser(UserCreateDto dto) {
        userRepository.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new UserException(ErrorCode.USERNAME_ALREADY_EXISTS, "Username '" + dto.getUsername() + "' is already taken.");
        });
        userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email '" + dto.getEmail() + "' is already registered.");
        });

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setPasswordVersion((short) 2);

        return UserMapper.mapToUserDto(userRepository.save(user));
    }

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

    @Override
    public UserDto updateUser(Long id, UserUpdateDto dto, String principalName) {
        User user = findOrThrow(id);

        // Ownership check: principalName must match the user being updated (unless ADMIN handled at controller)
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
            user.setPasswordVersion((short) 2);
        }

        return UserMapper.mapToUserDto(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        findOrThrow(id);
        userRepository.deleteById(id);
    }

    @Override
    public LoginResponseDto login(LoginDto dto) {
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new UserException(ErrorCode.INVALID_CREDENTIALS,
                        "Invalid username or password."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new UserException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password.");
        }

        UserDto userDto = UserMapper.mapToUserDto(user);
        String token = generateToken(user);
        return new LoginResponseDto(userDto, token);
    }

    // --- private helpers ---

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND,
                        "User with id " + id + " was not found."));
    }

    private String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
