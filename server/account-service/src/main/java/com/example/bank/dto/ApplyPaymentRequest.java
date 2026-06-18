package com.example.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplyPaymentRequest {
    private Long sourceId;
    private Long targetId;
    private BigDecimal amount;
    private Long paymentId;
}
