package com.example.bank.service;

import com.example.bank.dto.AccountCreateDto;
import com.example.bank.dto.AccountDto;

import java.util.List;

public interface AccountService {

    AccountDto createAccount(String username, AccountCreateDto accountCreateDto);

    AccountDto getAccountById(Long id);

    AccountDto deposit(Long id, double amount);

    AccountDto withdraw(Long id, double amount);

    List<AccountDto> getAllAccounts();

    void deleteAccount(Long id);

    void transfer(Long fromId, Long toId, double amount);
}
