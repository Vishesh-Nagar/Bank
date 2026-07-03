package com.example.bank;

import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.PaymentException;
import com.example.bank.service.TransferLimitService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test proving that {@link TransferLimitService} is race-free under parallel load.
 *
 * <h3>The Problem Being Tested</h3>
 * The naive implementation (get → compare → set) has a Time-Of-Check to Time-Of-Use (TOCTOU)
 * race condition. Two concurrent threads can both observe the limit as not exceeded, both
 * proceed to increment, and effectively allow 2× the intended daily limit.
 *
 * <h3>The Fix</h3>
 * We replaced the get/set pattern with an atomic Lua script executed server-side in Redis
 * via {@code EVAL}. Redis guarantees that no other command interleaves during script execution.
 *
 * <h3>How This Test Works</h3>
 * 10 concurrent threads all attempt to transfer $600 with a $5,000 daily limit.
 * Total attempted: $6,000. Only 8 transfers ($4,800) should succeed; the 9th ($5,400) exceeds
 * the limit. We verify:
 * <ul>
 *   <li>The number of allowed transfers is exactly correct (no under- or over-counting).</li>
 *   <li>The recorded Redis total never exceeds the daily limit.</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "payment.daily-limit=5000.00"
})
@Testcontainers
class TransferLimitConcurrencyTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("bank_payments")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("internal.service-secret", () -> "test-secret");
        registry.add("account-service.url", () -> "http://localhost:9998");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @MockBean
    com.example.bank.service.AccountServiceClient accountServiceClient;

    @MockBean
    com.example.bank.kafka.PaymentProducerService paymentProducerService;

    @Autowired
    TransferLimitService transferLimitService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("10 concurrent threads at $600 each → exactly 8 succeed, total ≤ $5,000")
    void concurrentTransfers_neverExceedDailyLimit() throws InterruptedException {
        final long accountId = 999L;
        final BigDecimal transferAmount = new BigDecimal("600.00");
        final int threadCount = 10;

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount  = new AtomicInteger(0);

        // CountDownLatch forces all threads to start simultaneously, maximising contention.
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate   = new CountDownLatch(threadCount);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();  // All threads wait until the gate opens
                    transferLimitService.checkAndRecordTransfer(accountId, transferAmount);
                    allowedCount.incrementAndGet();
                } catch (PaymentException e) {
                    if (e.getErrorCode() == ErrorCode.DAILY_LIMIT_EXCEEDED) {
                        deniedCount.incrementAndGet();
                    } else {
                        throw new RuntimeException("Unexpected PaymentException: " + e.getMessage(), e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            }));
        }

        startGate.countDown();  // Release all threads simultaneously
        endGate.await();        // Wait for all to finish
        pool.shutdown();

        // ── Assertions ───────────────────────────────────────────────────────

        // With a $5,000 limit and $600 per transfer:
        // floor(5000 / 600) = 8 transfers allowed → $4,800 total
        // The 9th transfer ($5,400) must be denied.
        assertThat(allowedCount.get())
                .as("Exactly 8 transfers of $600 fit within a $5,000 daily limit")
                .isEqualTo(8);

        assertThat(deniedCount.get())
                .as("The remaining 2 transfers must have been denied")
                .isEqualTo(2);

        // Verify the actual stored total in Redis is correct.
        String today = LocalDate.now().toString();
        String key   = "transfer_limit:" + accountId + ":" + today;
        String storedTotal = redisTemplate.opsForValue().get(key);

        assertThat(new BigDecimal(storedTotal))
                .as("Redis total must not exceed the $5,000 daily limit")
                .isLessThanOrEqualByComparingTo(new BigDecimal("5000.00"));

        assertThat(new BigDecimal(storedTotal))
                .as("Redis total must equal exactly 8 × $600 = $4,800")
                .isEqualByComparingTo(new BigDecimal("4800.00"));
    }
}
