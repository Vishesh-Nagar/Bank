package com.example.bank.entity;

import com.example.bank.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private String id;                          // UUID string

    /**
     * Client-supplied idempotency key (UUID).
     * Stored here as a DB-level uniqueness guard — a safety net in case the Redis
     * cache entry expires between the check and the save.
     */
    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private Long sourceAccountId;

    @Column(nullable = false)
    private Long targetAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime completedAt;
}
