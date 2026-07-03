package com.example.bank.service;

import com.example.bank.entity.RefreshToken;
import com.example.bank.entity.User;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.UserException;
import com.example.bank.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Manages refresh-token lifecycle: create, validate, revoke.
 * Each user gets at most one active refresh token (single-session policy).
 */
@Service
public class RefreshTokenService {

    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token for the given user.
     * Any previously issued tokens for this user are revoked first.
     */
    @Transactional
    public RefreshToken createFor(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(REFRESH_TTL));
        return refreshTokenRepository.save(rt);
    }

    /**
     * Validates and returns the refresh token entity.
     * Throws UserException if token is not found or has expired.
     */
    public RefreshToken validate(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UserException(ErrorCode.INVALID_TOKEN,
                        "Invalid refresh token."));
        if (rt.isExpired()) {
            refreshTokenRepository.delete(rt);
            throw new UserException(ErrorCode.TOKEN_EXPIRED,
                    "Refresh token has expired. Please log in again.");
        }
        return rt;
    }

    /** Revokes a single refresh token (used on logout). */
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }
}
