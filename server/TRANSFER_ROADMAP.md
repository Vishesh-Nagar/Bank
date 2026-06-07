# Payment & Task Queue Roadmap

> **Stack**: Spring Boot 3.5 · Spring Data JPA · MySQL · Spring Security (JWT) · Java 25 · Kafka
> **Goal**: Allow an authenticated user to send funds to **another user's account** by account ID,
> processed through a distributed Kafka task queue to prevent race conditions.

---

## Naming Convention

| Term | Scope | Method / Endpoint |
|---|---|---|
| **Transfer** *(existing)* | Moves funds between the **same user's own accounts** | `AccountServiceImpl.transfer()` — keep as-is |
| **Payment** *(new)* | Sends funds to a **different user's account** | `PaymentService.initiatePayment()` · `POST /api/payments` |

This separation mirrors real-world banking: *transfers* are internal moves; *payments* go to external parties.

---

## Current State Analysis

| What already exists | Notes |
|---|---|
| `AccountServiceImpl.transfer(fromId, toId, amount)` | Internal same-user move — **do not touch** |
| `AccountRepository.findByIdForUpdate` | Pessimistic write lock — reused by the payment queue |
| `Account` entity with `@Version` | Optimistic lock field — safety net |
| `@PreAuthorize` guard on `transfer` | Stays on the transfer method; payment gets its own guard |
| `GlobalExceptionHandler` | Handles common errors; needs one new `PaymentException` handler |

**Key gaps to close:**
1. No `POST /api/payments` REST endpoint
2. No `PaymentRequestDto` / `PaymentResponseDto`
3. No payment / audit trail entity
4. No task queue — needed to serialize per-account requests at the app layer and return an async receipt
5. No `PaymentException` for domain-specific error codes

---

## Roadmap

### Phase 1 — DTOs & REST Endpoint
*Estimated effort: ~1–2 hours*

#### 1.1 — `PaymentRequestDto`
**File**: `src/main/java/com/example/bank/dto/PaymentRequestDto.java` *(NEW)*

```java
@Data
public class PaymentRequestDto {

    @NotNull(message = "Source account ID is required")
    private Long sourceAccountId;

    @NotNull(message = "Target account ID is required")
    private Long targetAccountId;

    @Positive(message = "Amount must be positive")
    private double amount;
}
```

> The caller supplies `sourceAccountId` explicitly so one endpoint serves all of the user's accounts.
> Ownership of `sourceAccountId` is validated server-side via `@PreAuthorize`.

#### 1.2 — `PaymentResponseDto`
**File**: `src/main/java/com/example/bank/dto/PaymentResponseDto.java` *(NEW)*

```java
@Data @AllArgsConstructor
public class PaymentResponseDto {
    private String paymentId;        // UUID — use this to poll status
    private String status;           // "QUEUED"
    private Long sourceAccountId;
    private Long targetAccountId;
    private double amount;
    private LocalDateTime submittedAt;
}
```

#### 1.3 — New `PaymentController`
**File**: `src/main/java/com/example/bank/controller/PaymentController.java` *(NEW)*

Keeping payments in their own controller prevents `AccountController` from growing unbounded.

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // Initiate a payment (cross-user fund send)
    @PostMapping
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #request.sourceAccountId)")
    public ResponseEntity<PaymentResponseDto> initiatePayment(
            @Valid @RequestBody PaymentRequestDto request) {
        PaymentResponseDto response = paymentService.initiatePayment(request);
        return ResponseEntity.accepted().body(response); // 202 Accepted — async
    }

    // Poll payment status
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentStatusDto> getPaymentStatus(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getStatus(paymentId));
    }
}
```

> `202 Accepted` is the correct HTTP status for an async operation that has been queued but not yet completed.

#### 1.4 — `PaymentService` Interface
**File**: `src/main/java/com/example/bank/service/PaymentService.java` *(NEW)*

```java
public interface PaymentService {
    PaymentResponseDto initiatePayment(PaymentRequestDto request);
    PaymentStatusDto getStatus(String paymentId);
}
```

#### 1.5 — `PaymentException`
**File**: `src/main/java/com/example/bank/exception/PaymentException.java` *(NEW)*

```java
public class PaymentException extends RuntimeException {
    public PaymentException(String message) { super(message); }
}
```

Register in `GlobalExceptionHandler`:
```java
@ExceptionHandler(PaymentException.class)
public ResponseEntity<ErrorDetails> handlePaymentException(PaymentException ex, WebRequest req) {
    return buildError(ex, req, HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_FAILED");
}
```

---

### Phase 2 — Payment / Audit Entity
*Estimated effort: ~2–3 hours*

A persistent record of every payment is required for status polling, history, and audit.

#### 2.1 — `PaymentStatus` Enum
**File**: `src/main/java/com/example/bank/enums/PaymentStatus.java` *(NEW)*

```java
public enum PaymentStatus { PENDING, COMPLETED, FAILED }
```

#### 2.2 — `Payment` Entity
**File**: `src/main/java/com/example/bank/entity/Payment.java` *(NEW)*

| Column | Type | Notes |
|---|---|---|
| `id` | `String` (UUID) | Primary key, pre-generated |
| `sourceAccountId` | `Long` | Snapshot — no FK to prevent cascade issues |
| `targetAccountId` | `Long` | Snapshot |
| `amount` | `BigDecimal` | Prefer over `double` for financial precision |
| `status` | `PaymentStatus` | `PENDING → COMPLETED / FAILED` |
| `failureReason` | `String` | Nullable |
| `submittedAt` | `LocalDateTime` | `@CreationTimestamp` |
| `completedAt` | `LocalDateTime` | Nullable, set on settlement |

```java
@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor
public class Payment {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private Long sourceAccountId;

    @Column(nullable = false)
    private Long targetAccountId;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String failureReason;

    @CreationTimestamp
    private LocalDateTime submittedAt;

    private LocalDateTime completedAt;
}
```

#### 2.3 — `PaymentRepository`
**File**: `src/main/java/com/example/bank/repository/PaymentRepository.java` *(NEW)*

```java
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findBySourceAccountIdOrTargetAccountIdOrderBySubmittedAtDesc(Long sourceAccountId, Long targetAccountId);
}
```

#### 2.4 — `PaymentStatusDto`
**File**: `src/main/java/com/example/bank/dto/PaymentStatusDto.java` *(NEW)*

```java
@Data @AllArgsConstructor
public class PaymentStatusDto {
    private String paymentId;
    private PaymentStatus status;       // PENDING | COMPLETED | FAILED
    private String failureReason;       // null unless FAILED
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;  // null while PENDING
}
```

#### 2.5 — Payment History Endpoint
**File**: `PaymentController.java` *(MODIFY)*

```
GET /api/accounts/{accountId}/payments
```

Returns all payments where the account was sender or receiver.  
Add to `PaymentController` (or `AccountController` if preferred):

```java
@GetMapping("/accounts/{accountId}/payments")
@PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #accountId)")
public ResponseEntity<List<PaymentStatusDto>> getPaymentHistory(@PathVariable Long accountId) {
    return ResponseEntity.ok(paymentService.getPaymentHistory(accountId));
}
```

---

### Phase 3 — Distributed Task Queue with Kafka (Race Condition Prevention)
*Estimated effort: ~3–4 hours*

#### Why Kafka on top of DB locks?

The existing `transfer()` already uses `SELECT ... FOR UPDATE`. The problem with relying on DB locks alone for payments:

| Problem | DB lock only | DB lock + Kafka queue |
|---|---|---|
| Connection pool pressure | 1000 concurrent requests each hold a DB connection | Only consumer threads touch the DB |
| Back-pressure | None — all threads pile up at the DB lock | Kafka provides natural back-pressure |
| Async receipt | Caller blocks until DB commits | Caller gets `paymentId` immediately; polls for result |
| Distributed Safety | Thread locks only work per instance | Kafka ensures sequential processing across all instances |

#### 3.1 — `PaymentTask` Event
**File**: `src/main/java/com/example/bank/kafka/PaymentTask.java` *(NEW)*

```java
@Data @AllArgsConstructor
public class PaymentTask {
    private String paymentId;          // pre-generated UUID, matches Payment.id
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
}
```

#### 3.2 — Kafka Configuration
**File**: `pom.xml` *(MODIFY)*
Add `spring-kafka` dependency.

**File**: `src/main/java/com/example/bank/config/KafkaConfig.java` *(NEW)*
Configuration to ensure the topic `payments-topic` is created with sufficient partitions (e.g. 10).

#### 3.3 — `PaymentProducerService`
**File**: `src/main/java/com/example/bank/kafka/PaymentProducerService.java` *(NEW)*
Publishes messages to Kafka using `min(sourceAccountId, targetAccountId)` as the routing key.

```java
@Service
public class PaymentProducerService {

    private final KafkaTemplate<String, PaymentTask> kafkaTemplate;
    private final PaymentRepository paymentRepository;

    public void enqueue(PaymentTask task) {
        // Persist PENDING record
        Payment payment = new Payment();
        payment.setId(task.getPaymentId());
        payment.setSourceAccountId(task.getSourceAccountId());
        payment.setTargetAccountId(task.getTargetAccountId());
        payment.setAmount(task.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // Send to Kafka with canonical routing key
        String routingKey = String.valueOf(Math.min(task.getSourceAccountId(), task.getTargetAccountId()));
        kafkaTemplate.send("payments-topic", routingKey, task);
    }
}
```

#### 3.4 — `PaymentTaskListener`
**File**: `src/main/java/com/example/bank/kafka/PaymentTaskListener.java` *(NEW)*
Consumes messages and executes the DB update safely.

```java
@Component
public class PaymentTaskListener {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    @KafkaListener(topics = "payments-topic", groupId = "payment-group")
    @Transactional
    public void consumePaymentTask(PaymentTask task) {
        Payment payment = paymentRepository.findById(task.getPaymentId()).orElseThrow();
        if (payment.getStatus() != PaymentStatus.PENDING) return; // idempotency
        
        try {
            Long first  = Math.min(task.getSourceAccountId(), task.getTargetAccountId());
            Long second = Math.max(task.getSourceAccountId(), task.getTargetAccountId());

            Account a1 = accountRepository.findByIdForUpdate(first);
            Account a2 = accountRepository.findByIdForUpdate(second);

            Account from = task.getSourceAccountId().equals(a1.getId()) ? a1 : a2;
            Account to   = from.equals(a1) ? a2 : a1;

            if (from.getBalance() < task.getAmount().doubleValue()) {
                throw new PaymentException("Insufficient balance");
            }

            from.setBalance(from.getBalance() - task.getAmount().doubleValue());
            to.setBalance(to.getBalance()   + task.getAmount().doubleValue());

            accountRepository.save(from);
            accountRepository.save(to);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            payment.setCompletedAt(LocalDateTime.now());
        } finally {
            paymentRepository.save(payment);
        }
    }
}
```

#### 3.5 — `PaymentServiceImpl`
**File**: `src/main/java/com/example/bank/service/impl/PaymentServiceImpl.java` *(NEW)*

```java
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProducerService paymentProducerService;
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    @Override
    public PaymentResponseDto initiatePayment(PaymentRequestDto request) {
        Long sourceId = request.getSourceAccountId();
        Long targetId = request.getTargetAccountId();

        if (sourceId.equals(targetId)) {
            throw new PaymentException("Cannot send a payment to the same account. " +
                    "Use the transfer endpoint to move funds between your own accounts.");
        }

        // Validate both accounts exist upfront — fail fast before touching the queue
        Account from = accountRepository.findById(sourceId)
                .orElseThrow(() -> new AccountException("Source account not found"));
        Account to   = accountRepository.findById(targetId)
                .orElseThrow(() -> new AccountException("Target account not found"));

        // Cross-user guard: ensure source and target belong to different users
        if (from.getUser().getId().equals(to.getUser().getId())) {
            throw new PaymentException("Accounts belong to the same user. " +
                    "Use the transfer endpoint instead.");
        }

        String paymentId = UUID.randomUUID().toString();
        PaymentTask task = new PaymentTask(paymentId, sourceId, targetId,
                BigDecimal.valueOf(request.getAmount()));
        paymentProducerService.enqueue(task);

        return new PaymentResponseDto(paymentId, "QUEUED", sourceId, targetId,
                request.getAmount(), LocalDateTime.now());
    }

    @Override
    public PaymentStatusDto getStatus(String paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));
        return new PaymentStatusDto(p.getId(), p.getStatus(), p.getFailureReason(),
                p.getSourceAccountId(), p.getTargetAccountId(),
                p.getAmount(), p.getSubmittedAt(), p.getCompletedAt());
    }

    @Override
    public List<PaymentStatusDto> getPaymentHistory(Long accountId) {
        return paymentRepository
                .findBySourceAccountIdOrTargetAccountIdOrderBySubmittedAtDesc(accountId, accountId)
                .stream()
                .map(p -> new PaymentStatusDto(p.getId(), p.getStatus(), p.getFailureReason(),
                        p.getSourceAccountId(), p.getTargetAccountId(),
                        p.getAmount(), p.getSubmittedAt(), p.getCompletedAt()))
                .toList();
    }
}
```

---

### Phase 4 — Account Balance Type Migration (Optional but Recommended)
*Estimated effort: ~1 hour*

`Account.balance` is currently `double`, which introduces floating-point precision errors (e.g. `100.1 + 200.2 = 300.29999...`).

**Change**: `double balance` → `BigDecimal balance` in `Account`, `AccountDto`, `AccountMapper`, and `AccountServiceImpl`.

```java
// Account.java
@Column(nullable = false, precision = 19, scale = 4)
private BigDecimal balance;
```

---

## File Change Summary

```
server/src/main/java/com/example/bank/
│
│  ── EXISTING (do not rename) ──────────────────────────────────
├── service/impl/AccountServiceImpl.java     [MODIFY] — add cross-user guard only (optional)
├── pom.xml                                  [MODIFY] — add spring-kafka dependency
│
│  ── NEW — Payment feature ─────────────────────────────────────
├── config/
│   └── KafkaConfig.java                     [NEW]
├── dto/
│   ├── PaymentRequestDto.java               [NEW]
│   ├── PaymentResponseDto.java              [NEW]
│   └── PaymentStatusDto.java                [NEW]
├── entity/
│   └── Payment.java                         [NEW]
├── enums/
│   └── PaymentStatus.java                   [NEW]
├── exception/
│   ├── PaymentException.java                [NEW]
│   └── GlobalExceptionHandler.java          [MODIFY] — add PaymentException handler
├── kafka/
│   ├── PaymentTask.java                     [NEW]
│   ├── PaymentProducerService.java          [NEW]
│   └── PaymentTaskListener.java             [NEW]
├── repository/
│   └── PaymentRepository.java               [NEW]
├── service/
│   └── PaymentService.java                  [NEW]
├── service/impl/
│   └── PaymentServiceImpl.java              [NEW]
└── controller/
    └── PaymentController.java               [NEW]
```

> No existing files are renamed or structurally broken. The entire payment feature is additive.

---

## API Contract After Implementation

| Method | Path | Auth | Description |
|---|---|---|---|
| **Existing** | | | |
| `PUT` | `/api/accounts/{id}/deposit` | Owner | Deposit into own account |
| `PUT` | `/api/accounts/{id}/withdraw` | Owner | Withdraw from own account |
| `POST` | `/api/accounts/{id}/transfer` | *(to be wired)* | Move between **own** accounts |
| **New** | | | |
| `POST` | `/api/payments` | Owner of `sourceAccountId` | Send funds to **another user's** account |
| `GET` | `/api/payments/{paymentId}` | Authenticated | Poll payment status |
| `GET` | `/api/accounts/{id}/payments` | Owner | Payment history for account |

### Request — `POST /api/payments`
```json
{
  "sourceAccountId": 3,
  "targetAccountId": 7,
  "amount": 250.00
}
```

### Response — `202 Accepted`
```json
{
  "paymentId": "a3f2c1d0-84b1-4f22-9c3e-...",
  "status": "QUEUED",
  "sourceAccountId": 3,
  "targetAccountId": 7,
  "amount": 250.00,
  "submittedAt": "2026-06-07T14:08:00"
}
```

### Response — `GET /api/payments/{paymentId}` (after completion)
```json
{
  "paymentId": "a3f2c1d0-84b1-4f22-9c3e-...",
  "status": "COMPLETED",
  "failureReason": null,
  "sourceAccountId": 3,
  "targetAccountId": 7,
  "amount": 250.00,
  "submittedAt": "2026-06-07T14:08:00",
  "completedAt": "2026-06-07T14:08:00.053"
}
```

---

## Race Condition Prevention — How It Works

```
Thread A: payment(3 → 7, $100)       Thread B: payment(7 → 3, $50)
          │                                     │
          ▼                                     ▼
   kafkaKey = min(3,7) = 3            kafkaKey = min(3,7) = 3
          │                                     │
          ▼                                     ▼
   producer.send(TaskA)       →→→     producer.send(TaskB)
          │                                     │
          ▼                                     ▼
   [ Kafka Partition 2 ]              [ Kafka Partition 2 ]
          │                                     │
          ▼                                     ▼
   consumer reads TaskA               (waits for TaskA to finish)
   SELECT ... FOR UPDATE (id=3)
   SELECT ... FOR UPDATE (id=7)
   debit 3, credit 7
   COMMIT  ──────────────────────────────────▶  TaskB starts now
                                                SELECT ... FOR UPDATE (id=3)
                                                SELECT ... FOR UPDATE (id=7)
                                                ...
```

| Layer | Mechanism | Prevents |
|---|---|---|
| Application / Kafka | `min(sourceAccountId,targetAccountId)` as Kafka Routing Key | Ensures multiple payments on the same accounts run sequentially via the same Kafka partition |
| Database | `SELECT ... FOR UPDATE` (existing `findByIdForUpdate`) | Dirty reads / lost updates at DB level |
| ORM | `@Version` on `Account` | Last-ditch optimistic lock |

---

## Implementation Order

- [ ] **Phase 1** — DTOs + `PaymentController` + `PaymentService` interface + `PaymentException`
- [ ] **Phase 2** — `Payment` entity + `PaymentRepository` + `PaymentStatusDto` + history endpoint
- [ ] **Phase 3** — `spring-kafka` dependency + `PaymentTask` + `PaymentProducerService` + `PaymentTaskListener` + `PaymentServiceImpl`
- [ ] **Phase 4** — `BigDecimal` migration for `Account.balance`
