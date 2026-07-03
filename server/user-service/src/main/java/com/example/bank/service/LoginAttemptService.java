package com.example.bank.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Tracks failed login attempts per IP address in Redis.
 * After MAX_ATTEMPTS failures the IP is locked for LOCK_DURATION.
 */
@Service
public class LoginAttemptService {

    private static final String PREFIX        = "login:fail:";
    private static final int    MAX_ATTEMPTS  = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;

    public LoginAttemptService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Increment failure counter; set TTL on first failure. */
    public void recordFailure(String ip) {
        String key = PREFIX + ip;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, LOCK_DURATION);
        }
    }

    /** Returns true if this IP has exceeded the failure threshold. */
    public boolean isLocked(String ip) {
        String val = redis.opsForValue().get(PREFIX + ip);
        return val != null && Integer.parseInt(val) >= MAX_ATTEMPTS;
    }

    /** Clear failure counter on successful login. */
    public void clearFailures(String ip) {
        redis.delete(PREFIX + ip);
    }
}
