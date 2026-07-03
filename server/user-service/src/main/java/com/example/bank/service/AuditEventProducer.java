package com.example.bank.service;

import com.example.bank.dto.Event.AuditEvent;
import com.example.bank.entity.AuditLog;
import com.example.bank.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventProducer.class);
    private static final String TOPIC = "audit_events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                              AuditLogRepository auditLogRepository,
                              ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /** Publish audit event to Kafka and save to local DB fallback. */
    public void publish(String eventType, String principal, String entityType,
                        String entityId, String action, Object detailsObj) {
        
        String detailsJson = null;
        if (detailsObj != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(detailsObj);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize audit details", e);
                detailsJson = "{\"error\": \"serialization_failed\"}";
            }
        }

        Instant now = Instant.now();

        // 1. Save to local audit DB (source of truth)
        AuditLog logEntry = new AuditLog();
        logEntry.setEventType(eventType);
        logEntry.setPrincipal(principal);
        logEntry.setEntityType(entityType);
        logEntry.setEntityId(entityId);
        logEntry.setAction(action);
        logEntry.setDetails(detailsJson);
        logEntry.setTimestamp(now);
        auditLogRepository.save(logEntry);

        // 2. Publish to Kafka (for notification-service or external SIEM)
        AuditEvent event = new AuditEvent(
                eventType, principal, entityType, entityId, action, detailsJson, now
        );
        kafkaTemplate.send(TOPIC, event);
    }
}
