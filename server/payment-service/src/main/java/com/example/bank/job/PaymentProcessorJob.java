package com.example.bank.job;

import com.example.bank.entity.Payment;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.kafka.PaymentProducerService;
import com.example.bank.kafka.PaymentTask;
import com.example.bank.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentProcessorJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorJob.class);

    private final PaymentRepository paymentRepository;
    private final PaymentProducerService paymentProducerService;

    public PaymentProcessorJob(PaymentRepository paymentRepository,
                               PaymentProducerService paymentProducerService) {
        this.paymentRepository = paymentRepository;
        this.paymentProducerService = paymentProducerService;
    }

    /**
     * Runs every 10 seconds.
     * Finds PENDING payments that are older than 60 seconds,
     * marks them as PROCESSING, and queues them to Kafka.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void processPendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(60);
        List<Payment> readyToProcess = paymentRepository.findByStatusAndSubmittedAtBefore(PaymentStatus.PENDING, cutoff);

        if (readyToProcess.isEmpty()) {
            return;
        }

        log.info("Found {} pending payments older than 60 seconds. Processing...", readyToProcess.size());

        for (Payment payment : readyToProcess) {
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            PaymentTask task = new PaymentTask(
                    payment.getId(),
                    payment.getSourceAccountId(),
                    payment.getTargetAccountId(),
                    payment.getAmount()
            );
            paymentProducerService.enqueue(task);
            log.debug("Enqueued payment {} to Kafka.", payment.getId());
        }
    }
}
