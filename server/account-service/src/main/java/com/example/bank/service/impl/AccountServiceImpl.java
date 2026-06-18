package com.example.bank.service.impl;

import com.example.bank.dto.Account.AccountCreateDto;
import com.example.bank.dto.Account.AccountDto;
import com.example.bank.dto.User.UserDto;
import com.example.bank.entity.Account;
import com.example.bank.exception.AccountException;
import com.example.bank.exception.UserException;
import com.example.bank.mapper.AccountMapper;
import com.example.bank.repository.AccountRepository;
import com.example.bank.service.AccountService;
import com.example.bank.service.UserServiceClient;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Transactional
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserServiceClient userServiceClient;

    public AccountServiceImpl(AccountRepository accountRepository, UserServiceClient userServiceClient) {
        this.accountRepository = accountRepository;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public AccountDto createAccount(String username, AccountCreateDto accountCreateDto) {
        UserDto user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new UserException("User not found");
        }
        Account account = AccountMapper.mapToAccount(accountCreateDto);
        account.setUserId(user.getId());
        Account savedAccount = accountRepository.save(account);
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException("Account does not exist"));
        return AccountMapper.mapToAccountDto(account);
    }

    @Transactional
    @Override
    public AccountDto deposit(Long id, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(id);
        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);
        return AccountMapper.mapToAccountDto(saved);
    }

    @Transactional
    @Override
    public AccountDto withdraw(Long id, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(id);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
        Account saved = accountRepository.save(account);
        return AccountMapper.mapToAccountDto(saved);
    }

    @Override
    public List<AccountDto> getAllAccounts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserException("User not authenticated");
        }
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = principal.toString();
        }
        UserDto user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new UserException("User not found");
        }
        List<Account> accounts = accountRepository.findByUserId(user.getId());
        return accounts.stream().map(AccountMapper::mapToAccountDto).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    @Transactional
    @Override
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #fromId)")
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        if (fromId.equals(toId)) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        Long first = Math.min(fromId, toId);
        Long second = Math.max(fromId, toId);

        Account a1 = accountRepository.findByIdForUpdate(first);
        Account a2 = accountRepository.findByIdForUpdate(second);

        Account from = fromId.equals(a1.getId()) ? a1 : a2;
        Account to = from.equals(a1) ? a2 : a1;

        if (from.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);
    }

    @Transactional
    @Override
    public void applyPayment(Long sourceId, Long targetId, BigDecimal amount) {
        Account from = accountRepository.findByIdForUpdate(sourceId);
        Account to = accountRepository.findByIdForUpdate(targetId);
        if (from.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance for payment");
        }
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);
    }

    @Override
    public boolean isAccountOwner(String username, Long accountId) {
        UserDto user = userServiceClient.getUserByUsername(username);
        if (user == null) {
            throw new UserException("User not found");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException("Account not found"));
        return Objects.equals(account.getUserId(), user.getId());
    }
}
