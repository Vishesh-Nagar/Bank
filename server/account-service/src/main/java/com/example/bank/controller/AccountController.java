package com.example.bank.controller;

import com.example.bank.dto.Account.AccountCreateDto;
import com.example.bank.dto.Account.AccountDto;
import com.example.bank.dto.ApplyPaymentRequest;
import com.example.bank.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Add Account REST API
    @PostMapping("/accounts")
    public ResponseEntity<AccountDto> addAccount(Principal principal, @Valid @RequestBody AccountCreateDto accountCreateDto) {
        return new ResponseEntity<>(accountService.createAccount(principal.getName(), accountCreateDto), HttpStatus.CREATED);
    }

    // Get Account REST API
    @GetMapping("/accounts/{id}")
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #id)")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        AccountDto accountDto = accountService.getAccountById(id);
        return ResponseEntity.ok(accountDto);
    }

    // Deposit amount REST API
    @PutMapping("/accounts/{id}/deposit")
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #id)")
    public ResponseEntity<AccountDto> deposit(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal amount = request.get("amount");
        AccountDto accountDto = accountService.deposit(id, amount);
        return ResponseEntity.ok(accountDto);
    }

    // Withdraw amount REST API
    @PutMapping("/accounts/{id}/withdraw")
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #id)")
    public ResponseEntity<AccountDto> withdraw(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal amount = request.get("amount");
        AccountDto accountDto = accountService.withdraw(id, amount);
        return ResponseEntity.ok(accountDto);
    }

    // Get all accounts REST API
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        List<AccountDto> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    // Delete account REST API
    @DeleteMapping("/accounts/{id}")
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #id)")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok("Account deleted successfully");
    }

    // Internal endpoint: called by payment-service Feign client to apply a payment
    @PostMapping("/internal/accounts/apply-payment")
    public ResponseEntity<Void> applyPayment(@RequestBody ApplyPaymentRequest request) {
        accountService.applyPayment(request.getSourceId(), request.getTargetId(), request.getAmount());
        return ResponseEntity.ok().build();
    }

    // Internal endpoint: called by payment-service Feign client to check account ownership
    @GetMapping("/internal/accounts/{id}/owner")
    public ResponseEntity<Boolean> isAccountOwner(@PathVariable Long id, @RequestParam String username) {
        return ResponseEntity.ok(accountService.isAccountOwner(username, id));
    }
}
