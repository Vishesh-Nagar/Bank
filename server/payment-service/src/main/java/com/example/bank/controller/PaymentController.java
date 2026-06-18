package com.example.bank.controller;

import com.example.bank.common.ApiResponse;
import com.example.bank.common.PaginationMeta;
import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.dto.Payment.PaymentResponseDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.enums.ErrorCode;
import com.example.bank.exception.PaymentException;
import com.example.bank.service.AccountServiceClient;
import com.example.bank.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AccountServiceClient accountServiceClient;
    private final String internalSecret;

    public PaymentController(PaymentService paymentService,
                              AccountServiceClient accountServiceClient,
                              @Value("${internal.service-secret}") String internalSecret) {
        this.paymentService = paymentService;
        this.accountServiceClient = accountServiceClient;
        this.internalSecret = internalSecret;
    }

    // EP-PM-01: Initiate payment
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDto>> initiatePayment(
            @Valid @RequestBody PaymentRequestDto request,
            Principal principal,
            HttpServletRequest req) {

        // Verify source account ownership via Feign
        Boolean isOwner = accountServiceClient.isAccountOwner(
                request.getSourceAccountId(), principal.getName(), internalSecret);
        if (Boolean.FALSE.equals(isOwner)) {
            throw new PaymentException(ErrorCode.ACCOUNT_OWNERSHIP_REQUIRED,
                    "You do not own the source account.");
        }

        PaymentResponseDto response = paymentService.initiatePayment(request);
        return ResponseEntity.accepted()  // 202
                .body(ApiResponse.success(response, req.getHeader("X-Request-Id")));
    }

    // EP-PM-02: Poll payment status
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentStatusDto>> getPaymentStatus(
            @PathVariable String paymentId, HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getStatus(paymentId), req.getHeader("X-Request-Id")));
    }

    // EP-PM-03: Payment history for an account
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentStatusDto>>> getPaymentHistory(
            @RequestParam Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal,
            HttpServletRequest req) {

        // Verify account ownership before returning history
        Boolean isOwner = accountServiceClient.isAccountOwner(accountId, principal.getName(), internalSecret);
        if (Boolean.FALSE.equals(isOwner)) {
            throw new PaymentException(ErrorCode.ACCOUNT_OWNERSHIP_REQUIRED,
                    "You do not own this account.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        Page<PaymentStatusDto> result = paymentService.getPaymentHistory(accountId, pageable);
        PaginationMeta pagination = new PaginationMeta(
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.hasNext(), result.hasPrevious());
        return ResponseEntity.ok(ApiResponse.successPaginated(result, req.getHeader("X-Request-Id"), pagination));
    }
}
