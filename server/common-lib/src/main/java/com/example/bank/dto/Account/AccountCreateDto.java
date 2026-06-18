package com.example.bank.dto.Account;

import com.example.bank.enums.AccountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateDto {
    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @Min(value = 0, message = "Initial balance cannot be negative")
    @Digits(integer = 15, fraction = 4, message = "Initial balance has too many digits")
    private BigDecimal balance;

    @NotNull(message = "Account type is required")
    private AccountType accountType;
}
