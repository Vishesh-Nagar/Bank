package com.example.bank.service;

import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.dto.Payment.PaymentResponseDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentResponseDto initiatePayment(PaymentRequestDto request);

    PaymentStatusDto getStatus(String paymentId);

    Page<PaymentStatusDto> getPaymentHistory(Long accountId, Pageable pageable);

    void markPaymentFailed(String paymentId, String reason);
}
