package com.example.bank.repository;

import com.example.bank.entity.ScheduledPayment;
import com.example.bank.enums.ScheduledPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {
    List<ScheduledPayment> findByStatusAndNextExecutionTimeBefore(ScheduledPaymentStatus status, LocalDateTime time);
    List<ScheduledPayment> findBySourceAccountId(Long sourceAccountId);
}
