package com.example.bank.dto.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditEvent {
    private String eventType; // LOGIN_SUCCESS, LOGIN_FAILED, PASSWORD_CHANGED, etc.
    private String principal; // Username or IP
    private String entityType; // User, Payment, Account, etc.
    private String entityId;
    private String action; // CREATE, UPDATE, DELETE, EXECUTE
    private String details; // JSON or string payload
    private Instant timestamp;
}
