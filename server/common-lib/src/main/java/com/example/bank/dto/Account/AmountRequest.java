package com.example.bank.dto.Account;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AmountRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive number")
    @Digits(integer = 15, fraction = 4, message = "Amount has too many digits")
    private BigDecimal amount;
}
