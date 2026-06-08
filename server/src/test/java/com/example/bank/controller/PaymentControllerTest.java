package com.example.bank.controller;

import com.example.bank.dto.Payment.PaymentRequestDto;
import com.example.bank.dto.Payment.PaymentResponseDto;
import com.example.bank.dto.Payment.PaymentStatusDto;
import com.example.bank.enums.PaymentStatus;
import com.example.bank.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void initiatePayment_success() throws Exception {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setSourceAccountId(1L);
        request.setTargetAccountId(2L);
        request.setAmount(new BigDecimal("50.00"));

        PaymentResponseDto response = new PaymentResponseDto("uuid", "QUEUED", 1L, 2L, new BigDecimal("50.00"), LocalDateTime.now());

        when(paymentService.initiatePayment(any(PaymentRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.paymentId").value("uuid"));
    }

    @Test
    void getPaymentStatus_success() throws Exception {
        PaymentStatusDto response = new PaymentStatusDto("uuid", PaymentStatus.COMPLETED, null, 1L, 2L, new BigDecimal("50.00"), LocalDateTime.now(), LocalDateTime.now());

        when(paymentService.getStatus(eq("uuid"))).thenReturn(response);

        mockMvc.perform(get("/api/payments/uuid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
