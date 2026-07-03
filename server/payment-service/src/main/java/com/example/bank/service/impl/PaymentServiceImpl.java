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
import com.example.bank.service.TransferLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:payment:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final PaymentProducerService paymentProducerService;
    private final PaymentRepository paymentRepository;
    private final TransferLimitService transferLimitService;
    private final StringRedisTemplate redisTemplate;

    public PaymentServiceImpl(PaymentProducerService paymentProducerService,
                               PaymentRepository paymentRepository,
                               TransferLimitService transferLimitService,
                               StringRedisTemplate redisTemplate) {
        this.paymentProducerService = paymentProducerService;
        this.paymentRepository = paymentRepository;
        this.transferLimitService = transferLimitService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public PaymentResponseDto initiatePayment(PaymentRequestDto request, String idempotencyKey) {
        // ── Idempotency check ────────────────────────────────────────────────
        // If this key was already used, return the original payment instead of
        // creating a duplicate. This makes the endpoint safe to retry after
        // network failures without risk of double-charging the user.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            String existingPaymentId = redisTemplate.opsForValue().get(redisKey);

            if (existingPaymentId != null) {
                log.info("Idempotency hit for key={} → returning existing payment {}", idempotencyKey, existingPaymentId);
                return paymentRepository.findById(existingPaymentId)
                        .map(p -> new PaymentResponseDto(p.getId(), p.getStatus(),
                                p.getSourceAccountId(), p.getTargetAccountId(),
                                p.getAmount(), p.getSubmittedAt()))
                        .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND,
                                "Idempotent payment record missing: " + existingPaymentId));
            }
        }

        // ── Business validation ──────────────────────────────────────────────
        Long sourceId = request.getSourceAccountId();
        Long targetId = request.getTargetAccountId();

        if (sourceId.equals(targetId)) {
            throw new PaymentException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED,
                    "Source and target accounts must be different.");
        }

        // Enforce daily limit using an atomic Redis Lua script (race-free).
        transferLimitService.checkAndRecordTransfer(sourceId, request.getAmount());

        // ── Persist PENDING record ───────────────────────────────────────────
        String paymentId = UUID.randomUUID().toString();

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setSourceAccountId(sourceId);
        payment.setTargetAccountId(targetId);
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // ── Register idempotency key in Redis after successful DB save ────────
        // Registered AFTER save so that if the save fails, no stale key is left
        // in Redis pointing to a non-existent payment.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(redisKey, paymentId, IDEMPOTENCY_TTL);
        }

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

    @Override
    @Transactional
    public PaymentStatusDto disputePayment(String paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found: " + paymentId));

        // We assume the caller (controller) ensures this account belongs to the user,
        // or we trust the API Gateway for basic auth. For now, we just enforce
        // that it can only be disputed if it's still PENDING (i.e. within the 60s window).
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException(ErrorCode.VALIDATION_FAILED,
                    "Payment can only be disputed while it is PENDING.");
        }

        payment.setStatus(PaymentStatus.DISPUTED);
        payment.setFailureReason("User disputed / cancelled the payment");
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return toStatusDto(payment);
    }

    private PaymentStatusDto toStatusDto(Payment p) {
        return new PaymentStatusDto(p.getId(), p.getStatus(), p.getFailureReason(),
                p.getSourceAccountId(), p.getTargetAccountId(),
                p.getAmount(), p.getSubmittedAt(), p.getCompletedAt());
    }
}
