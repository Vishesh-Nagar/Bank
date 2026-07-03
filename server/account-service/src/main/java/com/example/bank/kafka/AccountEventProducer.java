package com.example.bank.kafka;

import com.example.bank.common.event.AccountBalanceChangedEvent;
import com.example.bank.entity.OutboxEvent;
import com.example.bank.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AccountEventProducer {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public AccountEventProducer(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void publishBalanceChanged(Long accountId, Long userId, BigDecimal newBalance, String eventType) {
        AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(
                accountId, userId, newBalance, eventType, LocalDateTime.now()
        );

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setTopic("account-events-topic");
        outboxEvent.setRoutingKey(String.valueOf(accountId));
        
        try {
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize AccountBalanceChangedEvent to JSON", e);
        }

        // Phase 1 of the Outbox Pattern: Save the event in the exact same database
        // transaction as the Account balance update.
        outboxEventRepository.save(outboxEvent);
    }
}
