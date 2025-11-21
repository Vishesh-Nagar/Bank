package com.example.bank.mapper;

import com.example.bank.dto.AccountCreateDto;
import com.example.bank.dto.AccountDto;
import com.example.bank.entity.Account;

public class AccountMapper {

    public static AccountDto mapToAccountDto(Account account) {

        return new AccountDto(
                account.getId() != null ? account.getId() : null,
                account.getAccountHolderName() != null ? account.getAccountHolderName() : null,
                account.getBalance(), // double cannot be null
                account.getAccountType() != null ? account.getAccountType() : null
        );
    }

    public static Account mapToAccount(AccountCreateDto accountCreateDto) {
        Account account = new Account();
        account.setAccountHolderName(accountCreateDto.getAccountHolderName());
        account.setBalance(accountCreateDto.getBalance());
        account.setAccountType(accountCreateDto.getAccountType());
        return account;
    }
}
