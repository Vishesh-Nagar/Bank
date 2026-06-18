package com.example.bank.kafka;

import com.example.bank.common.event.AccountBalanceChangedEvent;
import com.example.bank.dto.Notification.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountEventListener {

    private static final Logger log = LoggerFactory.getLogger(AccountEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    public AccountEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "account-events-topic", groupId = "notification-group")
    public void onAccountEvent(AccountBalanceChangedEvent event) {
        log.info("Account balance changed: accountId={} type={} newBalance={}",
                event.getAccountId(), event.getEventType(), event.getNewBalance());

        String message = String.format("Your account balance is now ₹%s (%s).",
                event.getNewBalance(), event.getEventType());

        NotificationDto notification = new NotificationDto("BALANCE_CHANGED", message, event);
        messagingTemplate.convertAndSend("/topic/account/" + event.getAccountId(), notification);
    }
}
