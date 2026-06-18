package com.example.bank.repository;

import com.example.bank.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findBySourceAccountIdOrTargetAccountIdOrderBySubmittedAtDesc(Long sourceAccountId, Long targetAccountId);
}
