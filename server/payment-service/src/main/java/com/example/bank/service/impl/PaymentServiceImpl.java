package com.example.bank.service.impl;

import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.dto.Payment.PaymentResponseDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.entity.Payment;
import com.example.bank.enums.ErrorCode;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.exception.PaymentException;
import com.example.bank.kafka.PaymentProducerService;
import com.example.bank.kafka.PaymentTask;
import com.example.bank.repository.PaymentRepository;
import com.example.bank.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProducerService paymentProducerService;
    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentProducerService paymentProducerService,
                               PaymentRepository paymentRepository) {
        this.paymentProducerService = paymentProducerService;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PaymentResponseDto initiatePayment(PaymentRequestDto request) {
        Long sourceId = request.getSourceAccountId();
        Long targetId = request.getTargetAccountId();

        if (sourceId.equals(targetId)) {
            throw new PaymentException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED,
                    "Source and target accounts must be different.");
        }

        String paymentId = UUID.randomUUID().toString();

        // Persist PENDING record before publishing to Kafka (outbox-lite pattern)
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setSourceAccountId(sourceId);
        payment.setTargetAccountId(targetId);
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        PaymentTask task = new PaymentTask(paymentId, sourceId, targetId, request.getAmount());
        paymentProducerService.enqueue(task);

        return new PaymentResponseDto(paymentId, PaymentStatus.PENDING, sourceId, targetId,
                request.getAmount(), payment.getSubmittedAt());
    }

    @Override
    public PaymentStatusDto getStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found: " + paymentId));
        return toStatusDto(payment);
    }

    @Override
    public Page<PaymentStatusDto> getPaymentHistory(Long accountId, Pageable pageable) {
        return paymentRepository
                .findBySourceAccountIdOrTargetAccountIdOrderBySubmittedAtDesc(accountId, accountId, pageable)
                .map(this::toStatusDto);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentFailed(String paymentId, String reason) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(reason);
                payment.setCompletedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        });
    }

    private PaymentStatusDto toStatusDto(Payment p) {
        return new PaymentStatusDto(p.getId(), p.getStatus(), p.getFailureReason(),
                p.getSourceAccountId(), p.getTargetAccountId(),
                p.getAmount(), p.getSubmittedAt(), p.getCompletedAt());
    }
}
