package com.example.bank.dto.Payment;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDto {

    @NotNull(message = "Source account ID is required")
    private Long sourceAccountId;

    @NotNull(message = "Target account ID is required")
    private Long targetAccountId;

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be positive")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;
}
