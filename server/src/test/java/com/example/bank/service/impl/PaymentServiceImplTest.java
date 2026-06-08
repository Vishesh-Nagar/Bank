package com.example.bank.service.impl;

import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.dto.Payment.PaymentResponseDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.entity.Account;
import com.example.bank.entity.Payment;
import com.example.bank.entity.User;
import com.example.bank.enums.AccountType;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.exception.PaymentException;
import com.example.bank.kafka.PaymentProducerService;
import com.example.bank.kafka.PaymentTask;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private PaymentProducerService paymentProducerService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User user1;
    private User user2;
    private Account account1;
    private Account account2;

    @BeforeEach
    void setUp() {
        user1 = new User(1L, "user1", "pass", "u1@e", new ArrayList<>());
        user2 = new User(2L, "user2", "pass", "u2@e", new ArrayList<>());
        account1 = new Account(1L, "A1", new BigDecimal("100"), AccountType.SAVINGS, user1, 0L);
        account2 = new Account(2L, "A2", new BigDecimal("100"), AccountType.SAVINGS, user2, 0L);
    }

    @Test
    void initiatePayment_success() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setSourceAccountId(1L);
        request.setTargetAccountId(2L);
        request.setAmount(new BigDecimal("50.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account2));
        doNothing().when(paymentProducerService).enqueue(any(PaymentTask.class));

        PaymentResponseDto response = paymentService.initiatePayment(request);

        assertNotNull(response);
        assertEquals("QUEUED", response.getStatus());
        assertEquals(new BigDecimal("50.00"), response.getAmount());
        verify(paymentProducerService, times(1)).enqueue(any(PaymentTask.class));
    }

    @Test
    void initiatePayment_sameAccountError() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setSourceAccountId(1L);
        request.setTargetAccountId(1L);
        request.setAmount(new BigDecimal("50.00"));

        assertThrows(PaymentException.class, () -> paymentService.initiatePayment(request));
    }

    @Test
    void initiatePayment_sameUserError() {
        Account account3 = new Account(3L, "A3", new BigDecimal("100"), AccountType.SAVINGS, user1, 0L);

        PaymentRequestDto request = new PaymentRequestDto();
        request.setSourceAccountId(1L);
        request.setTargetAccountId(3L);
        request.setAmount(new BigDecimal("50.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(account3));

        assertThrows(PaymentException.class, () -> paymentService.initiatePayment(request));
    }

    @Test
    void getStatus_success() {
        Payment p = new Payment();
        p.setId("uuid");
        p.setStatus(PaymentStatus.COMPLETED);
        p.setAmount(new BigDecimal("50"));
        when(paymentRepository.findById("uuid")).thenReturn(Optional.of(p));

        PaymentStatusDto status = paymentService.getStatus("uuid");
        assertEquals(PaymentStatus.COMPLETED, status.getStatus());
    }

    @Test
    void getPaymentHistory_success() {
        Payment p = new Payment();
        p.setId("uuid");
        when(paymentRepository.findBySourceAccountIdOrTargetAccountIdOrderBySubmittedAtDesc(1L, 1L))
                .thenReturn(List.of(p));

        List<PaymentStatusDto> list = paymentService.getPaymentHistory(1L);
        assertEquals(1, list.size());
    }

    @Test
    void markPaymentFailed_success() {
        Payment p = new Payment();
        p.setId("uuid");
        p.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById("uuid")).thenReturn(Optional.of(p));
        when(paymentRepository.save(any(Payment.class))).thenReturn(p);

        paymentService.markPaymentFailed("uuid", "reason");

        assertEquals(PaymentStatus.FAILED, p.getStatus());
        assertEquals("reason", p.getFailureReason());
        verify(paymentRepository, times(1)).save(p);
    }
}
