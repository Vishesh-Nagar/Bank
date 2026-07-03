package com.example.bank.kafka;

import com.example.bank.common.event.PaymentNotificationEvent;
import com.example.bank.dto.ApplyPaymentRequest;
import com.example.bank.entity.Payment;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.repository.PaymentRepository;
import com.example.bank.service.AccountServiceClient;
import com.example.bank.service.PaymentService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentTaskListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentTaskListener.class);

    private final PaymentRepository paymentRepository;
    private final AccountServiceClient accountServiceClient;
    private final PaymentService paymentService;
    private final KafkaTemplate<String, PaymentNotificationEvent> notificationTemplate;
    private final String internalSecret;

    public PaymentTaskListener(PaymentRepository paymentRepository,
                                AccountServiceClient accountServiceClient,
                                PaymentService paymentService,
                                KafkaTemplate<String, PaymentNotificationEvent> notificationTemplate,
                                @Value("${internal.service-secret}") String internalSecret) {
        this.paymentRepository = paymentRepository;
        this.accountServiceClient = accountServiceClient;
        this.paymentService = paymentService;
        this.notificationTemplate = notificationTemplate;
        this.internalSecret = internalSecret;
    }

    @KafkaListener(topics = "payments-topic", groupId = "payment-group")
    @Transactional
    public void consumePaymentTask(PaymentTask task) {
        Payment payment = paymentRepository.findById(task.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + task.getPaymentId()));

        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.PROCESSING) {
            log.info("Payment {} already processed (status={}). Skipping.", payment.getId(), payment.getStatus());
            return;
        }

        try {
            accountServiceClient.applyPayment(
                    new ApplyPaymentRequest(task.getSourceAccountId(), task.getTargetAccountId(),
                            task.getAmount(), task.getPaymentId()),
                    internalSecret);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            publishNotification(payment, null);

        } catch (Exception e) {
            log.error("Payment {} failed: {}", payment.getId(), e.getMessage());
            paymentService.markPaymentFailed(payment.getId(), e.getMessage());
            Payment failed = paymentRepository.findById(payment.getId()).orElse(payment);
            failed.setStatus(PaymentStatus.FAILED);
            publishNotification(failed, e.getMessage());
        }
    }

    private void publishNotification(Payment payment, String failureReason) {
        PaymentNotificationEvent event = new PaymentNotificationEvent(
                payment.getId(),
                payment.getStatus(),
                failureReason,
                payment.getSourceAccountId(),
                payment.getTargetAccountId(),
                payment.getAmount(),
                payment.getSubmittedAt(),
                payment.getCompletedAt()
        );
        notificationTemplate.send("payment-notifications-topic",
                String.valueOf(payment.getSourceAccountId()), event);
    }
}
