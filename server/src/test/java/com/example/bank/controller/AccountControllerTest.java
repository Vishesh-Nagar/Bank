package com.example.bank.controller;

import com.example.bank.dto.Account.AccountCreateDto;
import com.example.bank.dto.Account.AccountDto;
import com.example.bank.enums.AccountType;
import com.example.bank.service.AccountService;
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
import java.security.Principal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addAccount_success() throws Exception {
        AccountCreateDto request = new AccountCreateDto("Test", new BigDecimal("100.00"), AccountType.SAVINGS);
        AccountDto response = new AccountDto(1L, "Test", new BigDecimal("100.00"), AccountType.SAVINGS);

        when(accountService.createAccount(eq("testuser"), any(AccountCreateDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts")
                .principal(() -> "testuser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountHolderName").value("Test"));
    }

    @Test
    void deposit_success() throws Exception {
        Map<String, BigDecimal> request = Map.of("amount", new BigDecimal("50.00"));
        AccountDto response = new AccountDto(1L, "Test", new BigDecimal("150.00"), AccountType.SAVINGS);

        when(accountService.deposit(eq(1L), eq(new BigDecimal("50.00")))).thenReturn(response);

        mockMvc.perform(put("/api/accounts/1/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void withdraw_success() throws Exception {
        Map<String, BigDecimal> request = Map.of("amount", new BigDecimal("50.00"));
        AccountDto response = new AccountDto(1L, "Test", new BigDecimal("50.00"), AccountType.SAVINGS);

        when(accountService.withdraw(eq(1L), eq(new BigDecimal("50.00")))).thenReturn(response);

        mockMvc.perform(put("/api/accounts/1/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }
}
