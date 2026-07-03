package com.example.bank.service;

import com.example.bank.entity.User;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.UserException;
import com.example.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Sends email-verification links and processes the verification token.
 * Token is stored in Redis with a 24-hour TTL; it is single-use.
 */
@Service
public class EmailVerificationService {

    private static final String   PREFIX = "email-verify:";
    private static final Duration TTL    = Duration.ofHours(24);

    private final UserRepository       userRepository;
    private final StringRedisTemplate  redis;
    private final JavaMailSender       mailSender;
    private final String               appBaseUrl;

    public EmailVerificationService(UserRepository userRepository,
                                    StringRedisTemplate redis,
                                    JavaMailSender mailSender,
                                    @Value("${app.base-url:http://localhost}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.redis          = redis;
        this.mailSender     = mailSender;
        this.appBaseUrl     = appBaseUrl;
    }

    /** Generate a token, persist to Redis, send verification email. */
    public void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(PREFIX + token, String.valueOf(user.getId()), TTL);

        String link = appBaseUrl + "/verify-email?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(user.getEmail());
        msg.setSubject("Verify your Bank account email");
        msg.setText("Hello " + user.getUsername() + ",\n\n"
                + "Please verify your email address by clicking the link below "
                + "(valid for 24 hours):\n\n" + link
                + "\n\nIf you did not register, you can safely ignore this email.");
        mailSender.send(msg);
    }

    /** Consume the token and mark the user's email as verified. */
    public void verifyEmail(String token) {
        String key    = PREFIX + token;
        String userId = redis.opsForValue().get(key);
        if (userId == null) {
            throw new UserException(ErrorCode.INVALID_TOKEN,
                    "Verification token is invalid or has expired.");
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND,
                        "User not found."));

        user.setEmailVerified(true);
        userRepository.save(user);
        redis.delete(key);  // single-use
    }

    /** Resend a fresh verification link (replaces any existing token). */
    public void resendVerificationEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                sendVerificationEmail(user);
            }
        });
        // Always return success — don't reveal whether email is registered.
    }
}
