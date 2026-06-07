package com.example.bank.dto.Account;

import com.example.bank.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class AccountDto {
    private Long id;
    private String accountHolderName;
    private BigDecimal balance;
    private AccountType accountType;
}
