package com.example.bank.dto;

import com.example.bank.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateDto {
    private String accountHolderName;
    private double balance;
    private AccountType accountType;
}
