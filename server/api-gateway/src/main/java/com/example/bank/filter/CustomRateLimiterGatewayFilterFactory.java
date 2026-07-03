package com.example.bank.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * A highly-performant distributed rate limiter using the Sliding Window Log algorithm.
 * 
 * This completely replaces the default Spring Cloud Gateway Token Bucket limiter.
 * It leverages a custom Lua script executing atomically on Redis via Sorted Sets (ZSET).
 */
@Component
public class CustomRateLimiterGatewayFilterFactory extends AbstractGatewayFilterFactory<CustomRateLimiterGatewayFilterFactory.Config> {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> script;
    private final ApplicationContext applicationContext;

    public CustomRateLimiterGatewayFilterFactory(ReactiveRedisTemplate<String, String> redisTemplate,
                                                 RedisScript<Long> slidingWindowRateLimitScript,
                                                 ApplicationContext applicationContext) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.script = slidingWindowRateLimitScript;
        this.applicationContext = applicationContext;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("windowSizeInSeconds", "maxRequests", "keyResolverBeanName");
    }

    @Override
    public GatewayFilter apply(Config config) {
        // Resolve the KeyResolver bean at initialization time rather than per-request
        KeyResolver resolver;
        if (StringUtils.hasText(config.getKeyResolverBeanName())) {
            resolver = applicationContext.getBean(config.getKeyResolverBeanName(), KeyResolver.class);
        } else {
            // Default to the primary KeyResolver (which is ipKeyResolver in our config)
            resolver = applicationContext.getBean(KeyResolver.class);
        }

        return (exchange, chain) -> resolver.resolve(exchange).flatMap(key -> {
            if (!StringUtils.hasText(key)) {
                // If the key can't be resolved (e.g. anonymous user), just pass it through
                return chain.filter(exchange);
            }

            // Group limits by the route ID to prevent cross-route interference
            String routeId = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (routeId == null) {
                routeId = "default";
            }

            String redisKey = "rate_limit:" + routeId + ":" + key;
            long nowMs = System.currentTimeMillis();
            long windowMs = config.getWindowSizeInSeconds() * 1000L;
            long maxRequests = config.getMaxRequests();

            // Execute the Lua script atomically
            return redisTemplate.execute(
                    script,
                    Collections.singletonList(redisKey),
                    List.of(String.valueOf(nowMs), String.valueOf(windowMs), String.valueOf(maxRequests))
            ).next().flatMap(result -> {
                if (result != null && result == 1L) {
                    // Allowed
                    return chain.filter(exchange);
                } else {
                    // Denied - Too Many Requests
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                }
            });
        });
    }

    public static class Config {
        private int windowSizeInSeconds = 60;
        private int maxRequests = 100;
        private String keyResolverBeanName;

        public int getWindowSizeInSeconds() {
            return windowSizeInSeconds;
        }

        public void setWindowSizeInSeconds(int windowSizeInSeconds) {
            this.windowSizeInSeconds = windowSizeInSeconds;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public String getKeyResolverBeanName() {
            return keyResolverBeanName;
        }

        public void setKeyResolverBeanName(String keyResolverBeanName) {
            this.keyResolverBeanName = keyResolverBeanName;
        }
    }
}
