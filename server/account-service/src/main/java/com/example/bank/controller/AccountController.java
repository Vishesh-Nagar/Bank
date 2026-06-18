package com.example.bank.controller;

import com.example.bank.common.ApiResponse;
import com.example.bank.common.PaginationMeta;
import com.example.bank.dto.Account.AccountCreateDto;
import com.example.bank.dto.Account.AccountDto;
import com.example.bank.dto.Account.AmountRequest;
import com.example.bank.dto.ApplyPaymentRequest;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.AccountException;
import com.example.bank.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // EP-AC-01: Create account
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(
            Principal principal,
            @Valid @RequestBody AccountCreateDto dto,
            HttpServletRequest req) {
        AccountDto created = accountService.createAccount(principal.getName(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, req.getHeader("X-Request-Id")));
    }

    // EP-AC-02: Get account by ID (owner only)
    @GetMapping("/accounts/{id:\\d+}")
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #id)")
    public ResponseEntity<ApiResponse<AccountDto>> getAccountById(
            @PathVariable Long id, HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.getAccountById(id), req.getHeader("X-Request-Id")));
    }

    // EP-AC-03: List accounts for the authenticated user
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<Page<AccountDto>>> getAllAccounts(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<AccountDto> result = accountService.getAllAccounts(principal.getName(), pageable);
        PaginationMeta pagination = new PaginationMeta(
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.hasNext(), result.hasPrevious());
        return ResponseEntity.ok(ApiResponse.successPaginated(result, req.getHeader("X-Request-Id"), pagination));
    }

    // EP-AC-04: Deposit (owner only)
    @PostMapping("/accounts/{id:\\d+}/deposit")
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #id)")
    public ResponseEntity<ApiResponse<AccountDto>> deposit(
            @PathVariable Long id,
            @Valid @RequestBody AmountRequest body,
            HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.deposit(id, body.getAmount()), req.getHeader("X-Request-Id")));
    }

    // EP-AC-05: Withdraw (owner only)
    @PostMapping("/accounts/{id:\\d+}/withdraw")
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #id)")
    public ResponseEntity<ApiResponse<AccountDto>> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody AmountRequest body,
            HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.withdraw(id, body.getAmount()), req.getHeader("X-Request-Id")));
    }

    // EP-AC-06: Delete account (owner only)
    @DeleteMapping("/accounts/{id:\\d+}")
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #id)")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build(); // 204
    }


}
