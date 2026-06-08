package com.example.bank.service.impl;

import com.example.bank.dto.Account.AccountCreateDto;
import com.example.bank.dto.Account.AccountDto;
import com.example.bank.entity.Account;
import com.example.bank.entity.User;
import com.example.bank.enums.AccountType;
import com.example.bank.exception.AccountException;
import com.example.bank.exception.UserException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = new User(1L, "testuser", "hashed", "test@example.com", new ArrayList<>());
        account = new Account(1L, "Test Account", new BigDecimal("100.00"), AccountType.SAVINGS, user, 0L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAccount_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountCreateDto createDto = new AccountCreateDto("Test Account", new BigDecimal("100.00"), AccountType.SAVINGS);
        AccountDto result = accountService.createAccount("testuser", createDto);

        assertNotNull(result);
        assertEquals("Test Account", result.getAccountHolderName());
    }

    @Test
    void getAccountById_success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        AccountDto result = accountService.getAccountById(1L);
        assertNotNull(result);
    }

    @Test
    void getAccountById_notFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AccountException.class, () -> accountService.getAccountById(1L));
    }

    @Test
    void deposit_success() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(account);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.deposit(1L, new BigDecimal("50.00"));
        assertEquals(new BigDecimal("150.00"), account.getBalance());
    }

    @Test
    void withdraw_success() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(account);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.withdraw(1L, new BigDecimal("50.00"));
        assertEquals(new BigDecimal("50.00"), account.getBalance());
    }

    @Test
    void withdraw_insufficientFunds() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(account);

        assertThrows(RuntimeException.class, () -> accountService.withdraw(1L, new BigDecimal("150.00")));
    }

    @Test
    void getAllAccounts_success() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(accountRepository.findByUser(user)).thenReturn(List.of(account));

        List<AccountDto> result = accountService.getAllAccounts();
        assertEquals(1, result.size());
    }

    @Test
    void getAllAccounts_unauthenticated() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(UserException.class, () -> accountService.getAllAccounts());
    }

    @Test
    void transfer_success() {
        Account targetAccount = new Account(2L, "Target Account", new BigDecimal("50.00"), AccountType.SAVINGS, user, 0L);

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(account);
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(targetAccount);

        accountService.transfer(1L, 2L, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("50.00"), account.getBalance());
        assertEquals(new BigDecimal("100.00"), targetAccount.getBalance());
    }

    @Test
    void transfer_sameAccount() {
        assertThrows(RuntimeException.class, () -> accountService.transfer(1L, 1L, new BigDecimal("50.00")));
    }

    @Test
    void transfer_insufficientFunds() {
        Account targetAccount = new Account(2L, "Target Account", new BigDecimal("50.00"), AccountType.SAVINGS, user, 0L);

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(account);
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(targetAccount);

        assertThrows(RuntimeException.class, () -> accountService.transfer(1L, 2L, new BigDecimal("150.00")));
    }

    @Test
    void isAccountOwner_true() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertTrue(accountService.isAccountOwner("testuser", 1L));
    }

    @Test
    void isAccountOwner_false() {
        User otherUser = new User(2L, "other", "hash", "email", new ArrayList<>());
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertFalse(accountService.isAccountOwner("other", 1L));
    }
}
