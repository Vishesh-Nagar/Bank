package com.example.bank;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis-backed token blocklist.
 * Tokens are added with a TTL equal to their remaining validity (max 24h).
 * A blocked token causes the gateway to reject requests with 401 INVALID_TOKEN.
 */
@Service
public class TokenBlocklistService {

    private static final String PREFIX = "jwt:blocklist:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final ReactiveStringRedisTemplate redisTemplate;

    public TokenBlocklistService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add a token to the blocklist.
     * @param token  raw JWT string (without "Bearer " prefix)
     * @param ttl    remaining validity of the token
     */
    public Mono<Boolean> block(String token, Duration ttl) {
        return redisTemplate.opsForValue()
                .set(PREFIX + token, "1", ttl);
    }

    /** Convenience overload — uses 24-hour TTL. */
    public Mono<Boolean> block(String token) {
        return block(token, DEFAULT_TTL);
    }

    /**
     * Returns true if the token has been explicitly revoked.
     */
    public Mono<Boolean> isBlocked(String token) {
        return redisTemplate.hasKey(PREFIX + token);
    }
}
