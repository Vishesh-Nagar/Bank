package com.example.bank.dto;

import com.example.bank.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AccountDto {
    private Long id;
    private String accountHolderName;
    private double balance;
    private AccountType accountType;
}
