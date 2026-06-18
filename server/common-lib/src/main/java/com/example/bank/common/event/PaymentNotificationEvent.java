package com.example.bank.common.event;

import com.example.bank.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published by payment-service to "payment-notifications-topic" after a payment
 * transitions to COMPLETED or FAILED. Consumed by notification-service to send
 * WebSocket messages to subscribed clients.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentNotificationEvent {
    private String paymentId;
    private PaymentStatus status;
    private String failureReason;
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
}
