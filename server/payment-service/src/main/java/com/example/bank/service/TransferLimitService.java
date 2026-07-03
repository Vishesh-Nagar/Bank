package com.example.bank.service;

import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.PaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Enforces per-account daily transfer limits using Redis.
 *
 * <h3>Concurrency Safety</h3>
 * The limit check and increment are performed together in a single atomic
 * Lua script executed server-side in Redis (via {@code EVAL}). This eliminates
 * the check-then-act race condition present in a plain get/set approach, where
 * two concurrent threads could both observe the limit as not exceeded and both
 * proceed, effectively doubling the allowed transfer amount.
 *
 * <p>A Lua script is the canonical Redis solution for this class of problem —
 * Redis guarantees that {@code EVAL} commands are executed atomically; no other
 * command can interleave during the script's execution.
 */
@Service
public class TransferLimitService {

    /** Lua script return value: transfer is within limit and has been recorded. */
    private static final long RESULT_ALLOWED = 0L;

    /** Lua script return value: transfer would exceed the daily limit. */
    private static final long RESULT_DENIED = 1L;

    /** Redis TTL for limit keys: 48 hours to cover day-boundary edge cases. */
    private static final long TTL_SECONDS = 48 * 60 * 60;

    private final StringRedisTemplate redisTemplate;
    private final BigDecimal dailyLimit;
    private final DefaultRedisScript<Long> transferLimitScript;

    public TransferLimitService(StringRedisTemplate redisTemplate,
                                @Value("${payment.daily-limit}") BigDecimal dailyLimit) {
        this.redisTemplate = redisTemplate;
        this.dailyLimit = dailyLimit;

        // Load and compile the Lua script once at startup.
        // Spring caches the SHA1 hash after the first EVALSHA call, so subsequent
        // invocations use EVALSHA instead of EVAL — no repeated script transmission.
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/transfer_limit.lua")));
        script.setResultType(Long.class);
        this.transferLimitScript = script;
    }

    /**
     * Atomically checks whether adding {@code amount} to the account's daily total
     * would exceed the configured limit. If within bounds, the total is incremented
     * in the same atomic operation.
     *
     * @param accountId the source account being charged
     * @param amount    the transfer amount
     * @throws PaymentException if the daily limit would be exceeded
     */
    public void checkAndRecordTransfer(Long accountId, BigDecimal amount) {
        String today = LocalDate.now().toString();
        String key = "transfer_limit:" + accountId + ":" + today;

        Long result = redisTemplate.execute(
                transferLimitScript,
                List.of(key),
                amount.toPlainString(),
                dailyLimit.toPlainString(),
                String.valueOf(TTL_SECONDS)
        );

        if (result == null || result == RESULT_DENIED) {
            throw new PaymentException(ErrorCode.DAILY_LIMIT_EXCEEDED,
                    "Transfer exceeds the daily limit of " + dailyLimit + " for this account.");
        }
    }
}
