package com.example.bank.mapper;

import com.example.bank.dto.Account.AccountCreateDto;
import com.example.bank.dto.Account.AccountDto;
import com.example.bank.entity.Account;

public class AccountMapper {

    public static AccountDto mapToAccountDto(Account account) {
        return new AccountDto(
                account.getId(),
                account.getAccountHolderName(),
                account.getBalance(),
                account.getAccountType(),
                account.getUserId(),
                account.getCreatedAt()
        );
    }

    public static Account mapToAccount(AccountCreateDto dto) {
        Account account = new Account();
        account.setAccountHolderName(dto.getAccountHolderName());
        account.setBalance(dto.getBalance());
        account.setAccountType(dto.getAccountType());
        return account;
    }
}
