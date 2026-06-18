package com.example.bank.controller;

import com.example.bank.dto.ApplyPaymentRequest;
import com.example.bank.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/internal/accounts/apply-payment")
    public ResponseEntity<Void> applyPayment(@RequestBody ApplyPaymentRequest request) {
        accountService.applyPayment(request.getSourceId(), request.getTargetId(), request.getAmount());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/internal/accounts/{id:\\d+}/owner")
    public ResponseEntity<Boolean> isAccountOwner(
            @PathVariable Long id, @RequestParam String username) {
        return ResponseEntity.ok(accountService.isAccountOwner(username, id));
    }
}
