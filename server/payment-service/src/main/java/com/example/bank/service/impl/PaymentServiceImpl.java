package com.example.bank.service.impl;

import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.dto.Payment.PaymentResponseDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.entity.Payment;
import com.example.bank.exception.PaymentException;
import com.example.bank.kafka.PaymentProducerService;
import com.example.bank.kafka.PaymentTask;
import com.example.bank.repository.PaymentRepository;
import com.example.bank.service.AccountServiceClient;
import com.example.bank.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProducerService paymentProducerService;
    private final PaymentRepository paymentRepository;
    private final AccountServiceClient accountServiceClient;

    public PaymentServiceImpl(PaymentProducerService paymentProducerService, PaymentRepository paymentRepository, AccountServiceClient accountServiceClient) {
        this.paymentProducerService = paymentProducerService;
        this.paymentRepository = paymentRepository;
        this.accountServiceClient = accountServiceClient;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto initiatePayment(PaymentRequestDto request) {
        Long sourceId = request.getSourceAccountId();
        Long targetId = request.getTargetAccountId();

        if (sourceId.equals(targetId)) {
            throw new PaymentException("Cannot send a payment to the same account. Use the transfer endpoint to move funds between your own accounts.");
        }

        String paymentId = UUID.randomUUID().toString();
        PaymentTask task = new PaymentTask(paymentId, sourceId, targetId, request.getAmount());
        paymentProducerService.enqueue(task);

        return new PaymentResponseDto(paymentId, "QUEUED", sourceId, targetId, request.getAmount(), LocalDateTime.now());
    }

    @Override
    public PaymentStatusDto getStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));
        return new PaymentStatusDto(
                payment.getId(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getSourceAccountId(),
                payment.getTargetAccountId(),
                payment.getAmount(),
                payment.getSubmittedAt(),
                payment.getCompletedAt());
    }

    @Override
    public List<PaymentStatusDto> getPaymentHistory(Long accountId) {
        return paymentRepository.findBySourceAccountIdOrTargetAccountIdOrderBySubmittedAtDesc(accountId, accountId)
                .stream()
                .map(payment -> new PaymentStatusDto(
                        payment.getId(),
                        payment.getStatus(),
                        payment.getFailureReason(),
                        payment.getSourceAccountId(),
                        payment.getTargetAccountId(),
                        payment.getAmount(),
                        payment.getSubmittedAt(),
                        payment.getCompletedAt()))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentFailed(String paymentId, String reason) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            if (payment.getStatus() == com.example.bank.enums.PaymentStatus.PENDING) {
                payment.setStatus(com.example.bank.enums.PaymentStatus.FAILED);
                payment.setFailureReason(reason);
                payment.setCompletedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        });
    }
}
