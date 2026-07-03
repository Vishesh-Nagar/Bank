package com.example.bank.job;

import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.entity.ScheduledPayment;
import com.example.bank.enums.ScheduledPaymentStatus;
import com.example.bank.repository.ScheduledPaymentRepository;
import com.example.bank.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ScheduledPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPaymentProcessor.class);

    private final ScheduledPaymentRepository repository;
    private final PaymentService paymentService;

    public ScheduledPaymentProcessor(ScheduledPaymentRepository repository,
                                     PaymentService paymentService) {
        this.repository = repository;
        this.paymentService = paymentService;
    }

    /**
     * Poll every minute to trigger scheduled payments that are due.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processDuePayments() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledPayment> duePayments = repository.findByStatusAndNextExecutionTimeBefore(
                ScheduledPaymentStatus.ACTIVE, now);

        for (ScheduledPayment sp : duePayments) {
            log.info("Processing scheduled payment {}", sp.getId());
            try {
                PaymentRequestDto req = new PaymentRequestDto();
                req.setSourceAccountId(sp.getSourceAccountId());
                req.setTargetAccountId(sp.getTargetAccountId());
                req.setAmount(sp.getAmount());

                // Generate a deterministic idempotency key for this specific execution.
                // This prevents duplicate payments if the cron job fires on multiple
                // instances of the payment-service simultaneously.
                String idempotencyKey = "sched_" + sp.getId() + "_" + sp.getNextExecutionTime().toString();

                // Use the standard initiatePayment flow which enforces limits, etc.
                paymentService.initiatePayment(req, idempotencyKey);

                // Update next execution time
                updateNextExecutionTime(sp);
                repository.save(sp);

            } catch (Exception e) {
                log.error("Failed to process scheduled payment {}", sp.getId(), e);
                // Optionally mark as cancelled or failed
            }
        }
    }

    private void updateNextExecutionTime(ScheduledPayment sp) {
        switch (sp.getRecurrenceType()) {
            case ONCE:
                sp.setStatus(ScheduledPaymentStatus.COMPLETED);
                break;
            case DAILY:
                sp.setNextExecutionTime(sp.getNextExecutionTime().plusDays(1));
                break;
            case WEEKLY:
                sp.setNextExecutionTime(sp.getNextExecutionTime().plusWeeks(1));
                break;
            case MONTHLY:
                sp.setNextExecutionTime(sp.getNextExecutionTime().plusMonths(1));
                break;
        }
    }
}
