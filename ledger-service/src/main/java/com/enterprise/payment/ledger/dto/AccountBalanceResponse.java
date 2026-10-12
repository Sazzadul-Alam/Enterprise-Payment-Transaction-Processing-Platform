package com.enterprise.payment.ledger.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {

    private UUID accountId;
    private BigDecimal netBalance;
}
