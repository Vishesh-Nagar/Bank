package com.example.bank.service;

import com.example.bank.dto.Account.AccountCreateDto;
import com.example.bank.dto.Account.AccountDto;

import java.util.List;
import java.math.BigDecimal;

public interface AccountService {

    AccountDto createAccount(String username, AccountCreateDto accountCreateDto);

    AccountDto getAccountById(Long id);

    AccountDto deposit(Long id, BigDecimal amount);

    AccountDto withdraw(Long id, BigDecimal amount);

    List<AccountDto> getAllAccounts();

    void deleteAccount(Long id);

    void transfer(Long fromId, Long toId, BigDecimal amount);

    boolean isAccountOwner(String username, Long accountId);
    void applyPayment(Long sourceId, Long targetId, java.math.BigDecimal amount);
}
