package com.example.bank.kafka;

import com.example.bank.entity.Payment;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentProducerServiceTest {

    @Mock
    private KafkaTemplate<String, PaymentTask> kafkaTemplate;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentProducerService paymentProducerService;

    @Test
    void enqueue_success() {
        PaymentTask task = new PaymentTask("uuid", 2L, 1L, new BigDecimal("50.00"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentProducerService.enqueue(task);

        verify(paymentRepository, times(1)).save(argThat(payment -> 
            payment.getId().equals("uuid") &&
            payment.getSourceAccountId().equals(2L) &&
            payment.getTargetAccountId().equals(1L) &&
            payment.getAmount().equals(new BigDecimal("50.00")) &&
            payment.getStatus() == PaymentStatus.PENDING
        ));

        // The routing key should be the min of (2L, 1L) which is "1"
        verify(kafkaTemplate, times(1)).send(eq("payments-topic"), eq("1"), eq(task));
    }
}
