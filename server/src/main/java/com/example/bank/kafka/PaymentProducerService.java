package com.example.bank.kafka;

import com.example.bank.entity.Payment;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentProducerService {

    private final KafkaTemplate<String, PaymentTask> kafkaTemplate;
    private final PaymentRepository paymentRepository;

    public PaymentProducerService(KafkaTemplate<String, PaymentTask> kafkaTemplate, PaymentRepository paymentRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void enqueue(PaymentTask task) {
        // Persist PENDING record
        Payment payment = new Payment();

        payment.setId(task.getPaymentId());
        payment.setSourceAccountId(task.getSourceAccountId());
        payment.setTargetAccountId(task.getTargetAccountId());
        payment.setAmount(task.getAmount());
        payment.setStatus(PaymentStatus.PENDING);

        paymentRepository.save(payment);

        // Send to Kafka
        String routingKey = String.valueOf(Math.min(task.getSourceAccountId(), task.getTargetAccountId()));
        kafkaTemplate.send("payment-topic", routingKey, task);
    }
}
