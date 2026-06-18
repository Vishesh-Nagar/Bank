package com.example.bank.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes payment tasks to "payments-topic".
 * The Payment record must already be persisted (PENDING) before calling enqueue().
 */
@Service
public class PaymentProducerService {

    private final KafkaTemplate<String, PaymentTask> kafkaTemplate;

    public PaymentProducerService(KafkaTemplate<String, PaymentTask> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void enqueue(PaymentTask task) {
        // Route by the smaller account ID to preserve ordering between the same pair
        String routingKey = String.valueOf(
                Math.min(task.getSourceAccountId(), task.getTargetAccountId()));
        
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send("payments-topic", routingKey, task);
                }
            });
        } else {
            kafkaTemplate.send("payments-topic", routingKey, task);
        }
    }
}
