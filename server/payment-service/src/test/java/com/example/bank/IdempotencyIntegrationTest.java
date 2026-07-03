package com.example.bank;

import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.dto.Payment.PaymentResponseDto;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.repository.PaymentRepository;
import com.example.bank.service.PaymentService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for payment idempotency using real Redis and MySQL containers.
 *
 * <p>This test proves that submitting two identical payment requests with the same
 * {@code Idempotency-Key} results in exactly ONE payment record in the database —
 * the second call returns the original response without creating a duplicate charge.
 *
 * <p>Testcontainers spins up real Docker instances of MySQL and Redis, so there
 * are no mocks of the infrastructure layer. This gives us high confidence that the
 * idempotency guarantee holds in production conditions.
 */
@SpringBootTest(properties = {
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9999",  // Kafka not needed for this test
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@Testcontainers
class IdempotencyIntegrationTest {

    // ── Infrastructure containers ────────────────────────────────────────────

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
        // Provide required secrets with test values
        registry.add("internal.service-secret", () -> "test-secret");
        registry.add("payment.daily-limit", () -> "100000.00");
        registry.add("account-service.url", () -> "http://localhost:9998");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    // ── Mock Feign clients so we don't need real downstream services ─────────

    @MockBean
    com.example.bank.service.AccountServiceClient accountServiceClient;

    @MockBean
    com.example.bank.kafka.PaymentProducerService paymentProducerService;

    // ── System under test ────────────────────────────────────────────────────

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanup() {
        paymentRepository.deleteAll();
        // Flush Redis between tests
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Same Idempotency-Key → returns original payment, no duplicate created")
    void sameIdempotencyKey_returnsOriginalPayment() {
        String idempotencyKey = UUID.randomUUID().toString();

        PaymentRequestDto request = new PaymentRequestDto();
        request.setSourceAccountId(1L);
        request.setTargetAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));

        // First call — creates a new PENDING payment
        PaymentResponseDto first = paymentService.initiatePayment(request, idempotencyKey);
        assertThat(first.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(first.getPaymentId()).isNotNull();

        // Second call — same key, same request body (simulating a network retry)
        PaymentResponseDto second = paymentService.initiatePayment(request, idempotencyKey);

        // The response must reference the EXACT SAME payment record
        assertThat(second.getPaymentId())
                .as("Retried request must return the original payment ID, not a new one")
                .isEqualTo(first.getPaymentId());

        // Only ONE payment record must exist in the database
        long count = paymentRepository.count();
        assertThat(count)
                .as("Exactly one Payment record must exist — no duplicate was created")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("Different Idempotency-Keys → creates two independent payments")
    void differentIdempotencyKeys_createsTwoPayments() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setSourceAccountId(1L);
        request.setTargetAccountId(2L);
        request.setAmount(new BigDecimal("50.00"));

        PaymentResponseDto first  = paymentService.initiatePayment(request, UUID.randomUUID().toString());
        PaymentResponseDto second = paymentService.initiatePayment(request, UUID.randomUUID().toString());

        assertThat(second.getPaymentId())
                .as("Different idempotency keys must produce distinct payment IDs")
                .isNotEqualTo(first.getPaymentId());

        assertThat(paymentRepository.count())
                .as("Two separate payments must exist in the database")
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("No Idempotency-Key → payment proceeds normally without idempotency protection")
    void noIdempotencyKey_proceedsNormally() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setSourceAccountId(1L);
        request.setTargetAccountId(2L);
        request.setAmount(new BigDecimal("75.00"));

        // Both calls omit the idempotency key — two distinct payments are created
        paymentService.initiatePayment(request, null);
        paymentService.initiatePayment(request, null);

        assertThat(paymentRepository.count())
                .as("Without an idempotency key, two separate payments are created")
                .isEqualTo(2L);
    }
}
