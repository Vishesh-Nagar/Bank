package com.example.bank.service;

import com.example.bank.entity.User;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.UserException;
import com.example.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Handles the two-step forgot-password / reset-password flow.
 * Step 1 (forgotPassword): sends a reset link to the user's email.
 * Step 2 (resetPassword):  validates the token and sets the new password.
 */
@Service
public class PasswordResetService {

    private static final String   PREFIX = "pwd-reset:";
    private static final Duration TTL    = Duration.ofMinutes(15);

    private final UserRepository       userRepository;
    private final StringRedisTemplate  redis;
    private final JavaMailSender       mailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String               appBaseUrl;

    public PasswordResetService(UserRepository userRepository,
                                StringRedisTemplate redis,
                                JavaMailSender mailSender,
                                BCryptPasswordEncoder passwordEncoder,
                                @Value("${app.base-url:http://localhost}") String appBaseUrl) {
        this.userRepository  = userRepository;
        this.redis           = redis;
        this.mailSender      = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.appBaseUrl      = appBaseUrl;
    }

    /**
     * Step 1 — Request a password reset.
     * Always returns successfully to avoid leaking whether the email exists.
     */
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            redis.opsForValue().set(PREFIX + token, String.valueOf(user.getId()), TTL);

            String link = appBaseUrl + "/reset-password?token=" + token;

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("Password Reset Request — Bank App");
            msg.setText("Hello " + user.getUsername() + ",\n\n"
                    + "We received a request to reset your password.\n"
                    + "Click the link below to set a new password (valid for 15 minutes):\n\n"
                    + link
                    + "\n\nIf you did not request this, please ignore this email. "
                    + "Your password will not change.");
            mailSender.send(msg);
        });
    }

    /**
     * Step 2 — Consume the reset token and set the new password.
     * The token is deleted from Redis after use (single-use).
     */
    public void resetPassword(String token, String newPassword) {
        String key    = PREFIX + token;
        String userId = redis.opsForValue().get(key);
        if (userId == null) {
            throw new UserException(ErrorCode.INVALID_TOKEN,
                    "Password reset token is invalid or has expired.");
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND,
                        "User not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordVersion((short) (user.getPasswordVersion() + 1));
        userRepository.save(user);
        redis.delete(key);   // single-use — consumed
    }
}
