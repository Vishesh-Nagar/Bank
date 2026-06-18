package com.example.bank.dto.Account;

import com.example.bank.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AccountDto {
    private Long id;
    private String accountHolderName;
    private BigDecimal balance;
    private AccountType accountType;
    private Long userId;
    private LocalDateTime createdAt;
}
