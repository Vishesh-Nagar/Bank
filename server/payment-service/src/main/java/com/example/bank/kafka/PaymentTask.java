package com.example.bank.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentTask {
    private String paymentId;
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
}
