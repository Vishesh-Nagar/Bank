package com.example.bank.service;

import com.example.bank.dto.Account.AccountCreateDto;
import com.example.bank.dto.Account.AccountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface AccountService {

    AccountDto createAccount(String username, AccountCreateDto accountCreateDto);

    AccountDto getAccountById(Long id);

    AccountDto deposit(Long id, BigDecimal amount);

    AccountDto withdraw(Long id, BigDecimal amount);

    Page<AccountDto> getAllAccounts(String username, Pageable pageable);

    void deleteAccount(Long id);

    void transfer(Long fromId, Long toId, BigDecimal amount);

    boolean isAccountOwner(String username, Long accountId);

    void applyPayment(Long sourceId, Long targetId, BigDecimal amount);
}
