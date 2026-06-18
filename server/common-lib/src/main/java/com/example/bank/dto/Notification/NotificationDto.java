package com.example.bank.dto.Notification;

import com.example.bank.dto.Payment.PaymentStatusDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationDto {
    private String type;  // "PAYMENT_COMPLETED", "PAYMENT_COMPLETED", "PAYMENT_RECEIVED"
    private String message;
    private PaymentStatusDto payment;
}
