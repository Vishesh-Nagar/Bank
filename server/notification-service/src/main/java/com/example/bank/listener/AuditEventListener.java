package com.example.bank.listener;

import com.example.bank.dto.Event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    public AuditEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "audit_events", groupId = "notification-group")
    public void handleAuditEvent(AuditEvent event) {
        log.info("Received Audit Event: {} by {}", event.getEventType(), event.getPrincipal());
        
        // Notify admins in real-time about critical events
        // e.g. LOGIN_FAILED, ACCOUNT_LOCKED, DISPUTED_PAYMENT
        if (isCritical(event.getEventType())) {
            messagingTemplate.convertAndSend("/topic/admin/alerts", event);
        }
    }

    private boolean isCritical(String eventType) {
        return eventType.contains("FAILED") || 
               eventType.contains("LOCKED") || 
               eventType.contains("DISPUTED");
    }
}
