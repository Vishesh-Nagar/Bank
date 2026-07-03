package com.example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RateLimiterConfig {

    /**
     * Loads the Lua script into memory at startup.
     * Spring Data Redis will cache the script's SHA-1 digest on the Redis server
     * upon first execution and subsequently use EVALSHA to execute it, avoiding
     * the network overhead of sending the full script body on every request.
     */
    @Bean
    public DefaultRedisScript<Long> slidingWindowRateLimitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/sliding_window_rate_limit.lua")));
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
