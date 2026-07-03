package com.example.bank.entity;

import com.example.bank.enums.RecurrenceType;
import com.example.bank.enums.ScheduledPaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_payments")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sourceAccountId;

    @Column(nullable = false)
    private Long targetAccountId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceType recurrenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduledPaymentStatus status = ScheduledPaymentStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime nextExecutionTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
