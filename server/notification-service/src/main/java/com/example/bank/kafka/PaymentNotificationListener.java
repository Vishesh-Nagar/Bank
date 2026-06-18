package com.example.bank.kafka;

import com.example.bank.common.event.PaymentNotificationEvent;
import com.example.bank.dto.Notification.NotificationDto;
import com.example.bank.enums.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    public PaymentNotificationListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "payment-notifications-topic", groupId = "notification-group")
    public void onPaymentNotification(PaymentNotificationEvent event) {
        log.info("Received payment notification: paymentId={} status={}", event.getPaymentId(), event.getStatus());

        String senderType = event.getStatus() == PaymentStatus.COMPLETED
                ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED";
        String senderMsg = event.getStatus() == PaymentStatus.COMPLETED
                ? String.format("Payment of ₹%s completed successfully.", event.getAmount())
                : String.format("Payment of ₹%s failed: %s", event.getAmount(), event.getFailureReason());

        NotificationDto senderNotification = new NotificationDto(senderType, senderMsg, event);

        // Receiver notification only on success
        if (event.getStatus() == PaymentStatus.COMPLETED) {
            String receiverMsg = String.format("You received ₹%s from account %s.",
                    event.getAmount(), event.getSourceAccountId());
            NotificationDto receiverNotification = new NotificationDto("PAYMENT_RECEIVED", receiverMsg, event);
            messagingTemplate.convertAndSend(
                    "/topic/account/" + event.getTargetAccountId(), receiverNotification);
        }

        messagingTemplate.convertAndSend(
                "/topic/account/" + event.getSourceAccountId(), senderNotification);
    }
}
