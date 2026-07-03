package com.example.bank.job;

import com.example.bank.entity.OutboxEvent;
import com.example.bank.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Phase 2 of the Transactional Outbox Pattern.
 * 
 * This background daemon continuously polls the database for events that were
 * safely committed alongside business data, and reliably forwards them to Kafka.
 * This guarantees At-Least-Once delivery semantics even if Kafka experiences downtime.
 */
@Component
public class OutboxRelayJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayJob.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxRelayJob(OutboxEventRepository outboxEventRepository,
                          KafkaTemplate<String, Object> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Polls the outbox_events table for PENDING events every 5 seconds.
     * Processes them in batches of 100 to prevent memory exhaustion.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void relayOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                "PENDING", PageRequest.of(0, 100)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to relay to Kafka", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Parse the raw JSON payload into a generic node so Spring Kafka's
                // JsonSerializer will serialize it correctly (avoiding double-escaping).
                Object payloadNode = objectMapper.readTree(event.getPayload());
                
                // Publish the parsed payload to Kafka.
                // We use .get() to block until Kafka acknowledges receipt, ensuring
                // we only mark it COMPLETED if the broker actually stored it.
                kafkaTemplate.send(event.getTopic(), event.getRoutingKey(), payloadNode).get();
                
                event.setStatus("COMPLETED");
                outboxEventRepository.save(event);
                
                log.debug("Successfully relayed outbox event {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to relay outbox event {}. Will retry on next poll.", event.getId(), e);
                // The exception stops processing the current batch (or just logs and continues).
                // It's safe to break here because the transaction will roll back the COMPLETED status
                // if we threw, but since we catch it, we just leave it as PENDING for the next run.
            }
        }
    }
}
