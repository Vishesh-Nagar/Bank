package com.example.bank.service;

import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.dto.Payment.PaymentResponseDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.enums.PaymentStatus;

import java.util.List;

public interface PaymentService {
    PaymentResponseDto initiatePayment(PaymentRequestDto request);

    PaymentStatusDto getStatus(String paymentId);

    List<PaymentStatusDto> getPaymentHistory(Long accountId);

    void markPaymentFailed(String paymentId, String reason);
}
