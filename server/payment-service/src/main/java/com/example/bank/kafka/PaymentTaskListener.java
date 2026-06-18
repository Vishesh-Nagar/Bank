package com.example.bank.kafka;

import com.example.bank.dto.ApplyPaymentRequest;
import com.example.bank.dto.Notification.NotificationDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.entity.Payment;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.repository.PaymentRepository;
import com.example.bank.service.AccountServiceClient;
import com.example.bank.service.PaymentService;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentTaskListener {

    private final PaymentRepository paymentRepository;
    private final AccountServiceClient accountServiceClient;
    private final PaymentService paymentService;
    private final SimpMessagingTemplate messagingTemplate;

    public PaymentTaskListener(PaymentRepository paymentRepository, AccountServiceClient accountServiceClient, PaymentService paymentService, SimpMessagingTemplate messagingTemplate) {
        this.paymentRepository = paymentRepository;
        this.accountServiceClient = accountServiceClient;
        this.paymentService = paymentService;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "payments-topic", groupId = "payment-group")
    @Transactional
    public void consumePaymentTask(PaymentTask task) {
        Payment payment = paymentRepository.findById(task.getPaymentId()).orElseThrow();
        if (payment.getStatus() != PaymentStatus.PENDING)
            return;
        try {
            // Delegate the actual balance transfer to account-service via Feign
            accountServiceClient.applyPayment(new ApplyPaymentRequest(
                    task.getSourceAccountId(),
                    task.getTargetAccountId(),
                    task.getAmount(),
                    null
            ));

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            PaymentStatusDto paymentStatusDto = paymentService.getStatus(payment.getId());

            NotificationDto senderNotification = new NotificationDto(
                    "PAYMENT_COMPLETED",
                    "Payment of Rs" + task.getAmount() + " completed",
                    paymentStatusDto
            );
            NotificationDto receiverNotification = new NotificationDto(
                    "PAYMENT_RECEIVED",
                    "Received Rs" + task.getAmount() + " from Account " + task.getSourceAccountId(),
                    paymentStatusDto
            );

            // Broadcast to account-level topics since we no longer have user references here
            messagingTemplate.convertAndSend("/topic/account/" + task.getSourceAccountId(), senderNotification);
            messagingTemplate.convertAndSend("/topic/account/" + task.getTargetAccountId(), receiverNotification);
        } catch (Exception e) {
            paymentService.markPaymentFailed(payment.getId(), e.getMessage());
        }
    }
}
