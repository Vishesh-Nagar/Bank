package com.example.bank;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class GatewayConfig {

    /**
     * Rate-limit key resolver: uses client IP address.
     * For authenticated endpoints this can be swapped to user-based key.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(
                        exchange.getRequest().getRemoteAddress()
                ).getAddress().getHostAddress()
        );
    }

    /** Rate-limit by authenticated user (X-Authenticated-User header injected by JwtAuthFilter) */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String user = exchange.getRequest().getHeaders().getFirst("X-Authenticated-User");
            return Mono.just(user != null ? user : "anonymous");
        };
    }

}
