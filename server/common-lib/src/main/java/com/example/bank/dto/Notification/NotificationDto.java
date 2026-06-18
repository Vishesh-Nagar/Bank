package com.example.bank.dto.Notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private String type;     // PAYMENT_COMPLETED | PAYMENT_RECEIVED | PAYMENT_FAILED | BALANCE_CHANGED
    private String message;
    private Object payload;  // PaymentNotificationEvent or AccountBalanceChangedEvent
}
