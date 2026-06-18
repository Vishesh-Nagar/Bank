package com.example.bank.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentTask {
    private String paymentId;
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
}
