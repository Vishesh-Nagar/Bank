package com.example.bank.dto.Payment;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PaymentResponseDto {
    private String paymentId;               // UUID for tracking status
    private String status;                  // "Completed", "Queued" or "Failed"
    private Long sourceAccountId;
    private Long targetAccountId;
    private double amount;
    private LocalDateTime timestamp;
}
