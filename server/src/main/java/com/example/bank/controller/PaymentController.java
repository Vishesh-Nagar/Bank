package com.example.bank.controller;

import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.dto.Payment.PaymentResponseDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Initiate Payment
    @PostMapping
    @PreAuthorize("@accountServiceImpl.isAccountOwner(principal.name, #request.sourceAccountId)")
    public ResponseEntity<PaymentResponseDto> initiatePayment(@Valid @RequestBody PaymentRequestDto request) {
        PaymentResponseDto response = paymentService.initiatePayment(request);
        return ResponseEntity.accepted().body(response);
    }

    // Poll payment status
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentStatusDto> getPaymentStatus(@PathVariable("paymentId") String paymentId) {
        return ResponseEntity.ok(paymentService.getStatus(paymentId));
    }
}
