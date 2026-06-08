package com.example.bank.dto.Payment;

import com.example.bank.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PaymentStatusDto {
    private String paymentId;                       // UUID for tracking status
    private PaymentStatus status;                   // "Completed" | "Queued" | "Failed"
    private String failureReason;                   // null unless FAILED
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;              // null while PENDING
}
