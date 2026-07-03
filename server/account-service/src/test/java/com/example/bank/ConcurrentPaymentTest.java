package com.example.bank;

import com.example.bank.entity.Account;
import com.example.bank.enums.AccountType;
import com.example.bank.repository.AccountRepository;
import com.example.bank.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test proving that concurrent payments between two accounts maintain
 * correct final balances — no money is created or lost under parallel load.
 *
 * <h3>Why This Matters</h3>
 * A naive balance update (read → subtract → save) without locking allows two concurrent
 * transactions to read the same stale balance, both decrement it, and then overwrite each
 * other's writes. The result is that transfers "disappear" — money is lost from the source
 * account but never credited to the target (or vice versa).
 *
 * <h3>The Protection Mechanism Being Tested</h3>
 * {@code AccountServiceImpl.applyPayment} uses {@code findByIdForUpdate} which issues a
 * {@code SELECT ... FOR UPDATE} (pessimistic write lock). This serializes concurrent
 * writes to the same account rows at the database level, ensuring correct ordering.
 * Accounts are always locked in consistent ID order (min first) to prevent deadlocks.
 *
 * <h3>What This Test Proves</h3>
 * 5 concurrent threads each apply a $100 transfer from Account A to Account B.
 * After all threads complete:
 * <ul>
 *   <li>Account A must have lost exactly $500 (5 × $100)</li>
 *   <li>Account B must have gained exactly $500</li>
 *   <li>Total money in the system is conserved (no creation or destruction)</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@Testcontainers
class ConcurrentPaymentTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("bank_accounts")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("internal.service-secret", () -> "test-secret");
        registry.add("user-service.url", () -> "http://localhost:9997");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    // Kafka and UserServiceClient are not needed for this test
    @MockBean
    com.example.bank.service.UserServiceClient userServiceClient;

    @MockBean
    com.example.bank.kafka.AccountEventProducer accountEventProducer;

    @Autowired
    AccountService accountService;

    @Autowired
    AccountRepository accountRepository;

    private Long accountAId;
    private Long accountBId;

    private static final BigDecimal INITIAL_BALANCE_A = new BigDecimal("1000.00");
    private static final BigDecimal INITIAL_BALANCE_B = new BigDecimal("500.00");

    @BeforeEach
    void setup() {
        Account a = new Account();
        a.setAccountHolderName("Alice");
        a.setBalance(INITIAL_BALANCE_A);
        a.setAccountType(AccountType.SAVINGS);
        a.setUserId(1L);
        accountAId = accountRepository.save(a).getId();

        Account b = new Account();
        b.setAccountHolderName("Bob");
        b.setBalance(INITIAL_BALANCE_B);
        b.setAccountType(AccountType.SAVINGS);
        b.setUserId(2L);
        accountBId = accountRepository.save(b).getId();
    }

    @AfterEach
    void cleanup() {
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("5 concurrent $100 transfers A→B: final balances are exactly correct")
    void concurrentTransfers_conservesMoney() throws InterruptedException {
        final int threadCount = 5;
        final BigDecimal transferAmount = new BigDecimal("100.00");

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate   = new CountDownLatch(threadCount);
        List<Throwable> errors   = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();  // Force maximum contention at the database
                    accountService.applyPayment(accountAId, accountBId, transferAmount);
                } catch (Throwable t) {
                    synchronized (errors) { errors.add(t); }
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();
        pool.shutdown();

        // Fail fast on unexpected errors (e.g. InsufficientBalance, deadlock)
        assertThat(errors)
                .as("No exceptions should occur during concurrent transfers")
                .isEmpty();

        // ── Money conservation assertions ────────────────────────────────────
        BigDecimal finalA = accountRepository.findById(accountAId).orElseThrow().getBalance();
        BigDecimal finalB = accountRepository.findById(accountBId).orElseThrow().getBalance();

        BigDecimal expectedA = INITIAL_BALANCE_A.subtract(transferAmount.multiply(BigDecimal.valueOf(threadCount)));
        BigDecimal expectedB = INITIAL_BALANCE_B.add(transferAmount.multiply(BigDecimal.valueOf(threadCount)));

        assertThat(finalA)
                .as("Account A: started with $1,000, sent 5 × $100 → must have $500")
                .isEqualByComparingTo(expectedA);

        assertThat(finalB)
                .as("Account B: started with $500, received 5 × $100 → must have $1,000")
                .isEqualByComparingTo(expectedB);

        // Total money in the system must be exactly conserved
        BigDecimal totalBefore = INITIAL_BALANCE_A.add(INITIAL_BALANCE_B);
        BigDecimal totalAfter  = finalA.add(finalB);
        assertThat(totalAfter)
                .as("Total money in the system must be conserved (no creation or loss)")
                .isEqualByComparingTo(totalBefore);
    }
}
