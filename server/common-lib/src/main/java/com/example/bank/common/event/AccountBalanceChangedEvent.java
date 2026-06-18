package com.example.bank.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountBalanceChangedEvent {
    private Long accountId;
    private Long userId;
    private BigDecimal newBalance;
    private String eventType;
    private LocalDateTime timestamp;
}
