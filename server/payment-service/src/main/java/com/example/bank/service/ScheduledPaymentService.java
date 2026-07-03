package com.example.bank.service;

import com.example.bank.entity.ScheduledPayment;
import java.util.List;

public interface ScheduledPaymentService {
    ScheduledPayment createScheduledPayment(ScheduledPayment payment);
    void cancelScheduledPayment(Long id, Long userId);
    List<ScheduledPayment> getScheduledPayments(Long accountId, Long userId);
}
