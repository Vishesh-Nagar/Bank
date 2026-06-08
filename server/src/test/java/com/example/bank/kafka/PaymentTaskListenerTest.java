package com.example.bank.kafka;

import com.example.bank.dto.Notification.NotificationDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.entity.Account;
import com.example.bank.entity.Payment;
import com.example.bank.entity.User;
import com.example.bank.enums.AccountType;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.PaymentRepository;
import com.example.bank.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentTaskListenerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PaymentTaskListener paymentTaskListener;

    private User user1;
    private User user2;
    private Account account1;
    private Account account2;
    private Payment payment;
    private PaymentTask task;

    @BeforeEach
    void setUp() {
        user1 = new User(1L, "user1", "pass", "email1", new ArrayList<>());
        user2 = new User(2L, "user2", "pass", "email2", new ArrayList<>());
        account1 = new Account(1L, "A1", new BigDecimal("100.00"), AccountType.SAVINGS, user1, 0L);
        account2 = new Account(2L, "A2", new BigDecimal("50.00"), AccountType.SAVINGS, user2, 0L);
        
        payment = new Payment();
        payment.setId("uuid");
        payment.setSourceAccountId(1L);
        payment.setTargetAccountId(2L);
        payment.setAmount(new BigDecimal("20.00"));
        payment.setStatus(PaymentStatus.PENDING);

        task = new PaymentTask("uuid", 1L, 2L, new BigDecimal("20.00"));
    }

    @Test
    void consumePaymentTask_success() {
        when(paymentRepository.findById("uuid")).thenReturn(Optional.of(payment));
        // canonical ordering min(1,2)=1 max(1,2)=2
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(account1);
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(account2);

        PaymentStatusDto statusDto = new PaymentStatusDto("uuid", PaymentStatus.COMPLETED, null, 1L, 2L, new BigDecimal("20.00"), LocalDateTime.now(), LocalDateTime.now());
        when(paymentService.getStatus("uuid")).thenReturn(statusDto);

        paymentTaskListener.consumePaymentTask(task);

        assertEquals(new BigDecimal("80.00"), account1.getBalance());
        assertEquals(new BigDecimal("70.00"), account2.getBalance());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(paymentRepository, times(1)).save(payment);
        verify(messagingTemplate, times(1)).convertAndSendToUser(eq("user1"), eq("/queue/notifications"), any(NotificationDto.class));
        verify(messagingTemplate, times(1)).convertAndSendToUser(eq("user2"), eq("/queue/notifications"), any(NotificationDto.class));
    }

    @Test
    void consumePaymentTask_idempotency_ignoresCompleted() {
        payment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById("uuid")).thenReturn(Optional.of(payment));

        paymentTaskListener.consumePaymentTask(task);

        verify(accountRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void consumePaymentTask_insufficientBalance_marksFailed() {
        account1.setBalance(new BigDecimal("10.00")); // Less than task amount 20.00
        
        when(paymentRepository.findById("uuid")).thenReturn(Optional.of(payment));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(account1);
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(account2);

        paymentTaskListener.consumePaymentTask(task);

        verify(paymentService, times(1)).markPaymentFailed(eq("uuid"), eq("Insufficient balance"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void consumePaymentTask_exception_marksFailed() {
        when(paymentRepository.findById("uuid")).thenReturn(Optional.of(payment));
        when(accountRepository.findByIdForUpdate(1L)).thenThrow(new RuntimeException("DB Error"));

        paymentTaskListener.consumePaymentTask(task);

        verify(paymentService, times(1)).markPaymentFailed(eq("uuid"), eq("DB Error"));
    }
}
