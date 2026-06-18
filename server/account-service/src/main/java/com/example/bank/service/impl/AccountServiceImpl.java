package com.example.bank.service.impl;

import com.example.bank.dto.Account.AccountCreateDto;
import com.example.bank.dto.Account.AccountDto;
import com.example.bank.dto.User.UserDto;
import com.example.bank.entity.Account;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.AccountException;
import com.example.bank.exception.UserException;
import com.example.bank.kafka.AccountEventProducer;
import com.example.bank.mapper.AccountMapper;
import com.example.bank.repository.AccountRepository;
import com.example.bank.service.AccountService;
import com.example.bank.service.UserServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserServiceClient userServiceClient;
    private final AccountEventProducer accountEventProducer;
    private final String internalSecret;

    public AccountServiceImpl(AccountRepository accountRepository,
                               UserServiceClient userServiceClient,
                               AccountEventProducer accountEventProducer,
                               @Value("${internal.service-secret}") String internalSecret) {
        this.accountRepository = accountRepository;
        this.userServiceClient = userServiceClient;
        this.accountEventProducer = accountEventProducer;
        this.internalSecret = internalSecret;
    }

    @Override
    @Transactional
    public AccountDto createAccount(String username, AccountCreateDto dto) {
        UserDto user = userServiceClient.getUserByUsername(username, internalSecret);
        if (user == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND, "User not found.");
        }
        Account account = AccountMapper.mapToAccount(dto);
        account.setUserId(user.getId());
        return AccountMapper.mapToAccountDto(accountRepository.save(account));
    }

    @Override
    public AccountDto getAccountById(Long id) {
        return AccountMapper.mapToAccountDto(findOrThrow(id));
    }

    @Override
    @Transactional
    public AccountDto deposit(Long id, BigDecimal amount) {
        Account account = findForUpdateOrThrow(id);
        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);
        accountEventProducer.publishBalanceChanged(saved.getId(), saved.getUserId(),
                saved.getBalance(), "DEPOSIT");
        return AccountMapper.mapToAccountDto(saved);
    }

    @Override
    @Transactional
    public AccountDto withdraw(Long id, BigDecimal amount) {
        Account account = findForUpdateOrThrow(id);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new AccountException(ErrorCode.INSUFFICIENT_BALANCE,
                    "Insufficient balance. Available: " + account.getBalance());
        }
        account.setBalance(account.getBalance().subtract(amount));
        Account saved = accountRepository.save(account);
        accountEventProducer.publishBalanceChanged(saved.getId(), saved.getUserId(),
                saved.getBalance(), "WITHDRAWAL");
        return AccountMapper.mapToAccountDto(saved);
    }

    @Override
    public Page<AccountDto> getAllAccounts(String username, Pageable pageable) {
        UserDto user = userServiceClient.getUserByUsername(username, internalSecret);
        if (user == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND, "User not found.");
        }
        return accountRepository.findByUserId(user.getId(), pageable)
                .map(AccountMapper::mapToAccountDto);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Account account = findOrThrow(id);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountException(ErrorCode.ACCOUNT_HAS_BALANCE,
                    "Cannot delete an account with a non-zero balance. Please withdraw all funds first.");
        }
        accountRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        if (fromId.equals(toId)) {
            throw new AccountException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED,
                    "Source and target accounts must be different.");
        }
        Long first = Math.min(fromId, toId);
        Long second = Math.max(fromId, toId);

        Account a1 = findForUpdateOrThrow(first);
        Account a2 = findForUpdateOrThrow(second);

        Account from = fromId.equals(a1.getId()) ? a1 : a2;
        Account to = from.equals(a1) ? a2 : a1;

        if (from.getBalance().compareTo(amount) < 0) {
            throw new AccountException(ErrorCode.INSUFFICIENT_BALANCE,
                    "Insufficient balance for transfer.");
        }
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);
    }

    @Override
    @Transactional
    public void applyPayment(Long sourceId, Long targetId, BigDecimal amount) {
        Account from = findForUpdateOrThrow(sourceId);
        Account to = findForUpdateOrThrow(targetId);
        if (from.getBalance().compareTo(amount) < 0) {
            throw new AccountException(ErrorCode.INSUFFICIENT_BALANCE,
                    "Insufficient balance for payment.");
        }
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);
        accountEventProducer.publishBalanceChanged(from.getId(), from.getUserId(), from.getBalance(), "PAYMENT_SENT");
        accountEventProducer.publishBalanceChanged(to.getId(), to.getUserId(), to.getBalance(), "PAYMENT_RECEIVED");
    }

    @Override
    public boolean isAccountOwner(String username, Long accountId) {
        UserDto user = userServiceClient.getUserByUsername(username, internalSecret);
        if (user == null) return false;
        return accountRepository.findById(accountId)
                .map(account -> Objects.equals(account.getUserId(), user.getId()))
                .orElse(false);
    }

    // --- helpers ---

    private Account findOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account with id " + id + " was not found."));
    }

    private Account findForUpdateOrThrow(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account with id " + id + " was not found."));
    }
}
