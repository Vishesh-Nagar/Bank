package com.example.bank.dto.Payment;

import com.example.bank.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PaymentResponseDto {
    private String paymentId;
    private PaymentStatus status;       // enum: PENDING, COMPLETED, FAILED
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
    private LocalDateTime submittedAt;
}
