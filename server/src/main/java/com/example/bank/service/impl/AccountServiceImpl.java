package com.example.bank.service.impl;

import com.example.bank.dto.AccountCreateDto;
import com.example.bank.dto.AccountDto;
import com.example.bank.entity.Account;
import com.example.bank.entity.User;
import com.example.bank.exception.AccountException;
import com.example.bank.exception.UserException;
import com.example.bank.mapper.AccountMapper;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountServiceImpl(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AccountDto createAccount(String username, AccountCreateDto accountCreateDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException("User not found"));
        Account account = AccountMapper.mapToAccount(accountCreateDto);
        account.setUser(user);
        Account savedAccount = accountRepository.save(account);
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException("Account does not exist"));
        validateAccountOwner(account);
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public AccountDto deposit(Long id, double amount) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new AccountException("Account not found"));
        validateAccountOwner(account);
        double newBalance = account.getBalance() + amount;
        account.setBalance(newBalance);
        Account savedAccount = accountRepository.save(account);
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto withdraw(Long id, double amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException("Account not found"));
        validateAccountOwner(account);
        double balance = account.getBalance();
        if (balance < amount) {
            throw new RuntimeException("Insufficient balance");
        }
        double newBalance = balance - amount;
        account.setBalance(newBalance);
        Account savedAccount = accountRepository.save(account);
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public List<AccountDto> getAllAccounts() {
        User user = getAuthenticatedUser();
        List<Account> accounts = accountRepository.findByUser(user);
        return accounts.stream().map(AccountMapper::mapToAccountDto).collect(Collectors.toList());
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException("Account not found"));
        validateAccountOwner(account);
        accountRepository.deleteById(id);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UserException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new UserException("User not authenticated");
        }

        String username;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException("User not found"));
    }

    private void validateAccountOwner(Account account) {
        User user = getAuthenticatedUser();
        if (!Objects.equals(account.getUser().getId(), user.getId())) {
            throw new AccountException("You are not authorized to access this account");
        }
    }
}
