package com.example.bank.service.impl;

import com.example.bank.entity.ScheduledPayment;
import com.example.bank.enums.ErrorCode;
import com.example.bank.enums.ScheduledPaymentStatus;
import com.example.bank.exception.PaymentException;
import com.example.bank.repository.ScheduledPaymentRepository;
import com.example.bank.service.ScheduledPaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScheduledPaymentServiceImpl implements ScheduledPaymentService {

    private final ScheduledPaymentRepository repository;

    public ScheduledPaymentServiceImpl(ScheduledPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ScheduledPayment createScheduledPayment(ScheduledPayment payment) {
        if (payment.getSourceAccountId().equals(payment.getTargetAccountId())) {
            throw new PaymentException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED,
                    "Source and target accounts must be different.");
        }
        payment.setStatus(ScheduledPaymentStatus.ACTIVE);
        return repository.save(payment);
    }

    @Override
    @Transactional
    public void cancelScheduledPayment(Long id, Long userId) {
        ScheduledPayment sp = repository.findById(id)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Scheduled payment not found."));

        if (sp.getStatus() != ScheduledPaymentStatus.ACTIVE) {
            throw new PaymentException(ErrorCode.VALIDATION_FAILED,
                    "Only active scheduled payments can be cancelled.");
        }
        sp.setStatus(ScheduledPaymentStatus.CANCELLED);
        repository.save(sp);
    }

    @Override
    public List<ScheduledPayment> getScheduledPayments(Long accountId, Long userId) {
        return repository.findBySourceAccountId(accountId);
    }
}
