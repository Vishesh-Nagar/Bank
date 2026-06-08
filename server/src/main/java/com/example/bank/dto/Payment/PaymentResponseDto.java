package com.example.bank.dto.Payment;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentResponseDto {
    private String paymentId;               // UUID for tracking status
    private String status;                  // "PENDING", "COMPLETED", or "FAILED"
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
