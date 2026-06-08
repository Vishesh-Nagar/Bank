package com.example.bank.kafka;

import com.example.bank.dto.Notification.NotificationDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.entity.Account;
import com.example.bank.entity.Payment;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.exception.PaymentException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.example.bank.service.PaymentService;

import java.time.LocalDateTime;

@Component
public class PaymentTaskListener {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final PaymentService paymentService;
    private final SimpMessagingTemplate messagingTemplate;

    public PaymentTaskListener(PaymentRepository paymentRepository, AccountRepository accountRepository, PaymentService paymentService, SimpMessagingTemplate messagingTemplate) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
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
            Long first = Math.min(task.getSourceAccountId(), task.getTargetAccountId());
            Long second = Math.max(task.getSourceAccountId(), task.getTargetAccountId());

            Account a1 = accountRepository.findByIdForUpdate(first);
            Account a2 = accountRepository.findByIdForUpdate(second);

            Account source = task.getSourceAccountId().equals(a1.getId()) ? a1 : a2;
            Account target = source.equals(a1) ? a2 : a1;

            if (source.getBalance().compareTo(task.getAmount()) < 0) {
                throw new PaymentException("Insufficient balance");
            }

            source.setBalance(source.getBalance().subtract(task.getAmount()));
            target.setBalance(target.getBalance().add(task.getAmount()));

            accountRepository.save(source);
            accountRepository.save(target);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            PaymentStatusDto paymentStatusDto = paymentService.getStatus(payment.getId());

            NotificationDto senderNotification = new NotificationDto("PAYMENT_COMPLETED", "Payment of Rs" + task.getAmount() + " completed", paymentStatusDto);

            NotificationDto receiverNotification = new NotificationDto("PAYMENT_RECEIVED",
                    "Received $" + task.getAmount() + " from Account " + task.getSourceAccountId(), paymentStatusDto);

            // Send to specific users using /user/{username}/queue/notifications
            messagingTemplate.convertAndSendToUser(source.getUser().getUsername(), "/queue/notifications", senderNotification);
            messagingTemplate.convertAndSendToUser(target.getUser().getUsername(), "/queue/notifications", receiverNotification);
        } catch (Exception e) {
            paymentService.markPaymentFailed(payment.getId(), e.getMessage());
        }
    }
}
