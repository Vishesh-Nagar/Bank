package com.example.bank.kafka;

import com.example.bank.common.event.AccountBalanceChangedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AccountEventProducer {

    private final KafkaTemplate<String, AccountBalanceChangedEvent> kafkaTemplate;

    public AccountEventProducer(KafkaTemplate<String, AccountBalanceChangedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBalanceChanged(Long accountId, Long userId, BigDecimal newBalance, String eventType) {
        AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(
                accountId, userId, newBalance, eventType, LocalDateTime.now()
        );
        kafkaTemplate.send("account-events-topic", String.valueOf(accountId), event);
    }
}
