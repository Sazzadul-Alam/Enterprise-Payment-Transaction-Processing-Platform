package com.enterprise.payment.ledger.dto;

import com.enterprise.payment.ledger.entity.enums.EntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerLineRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Entry type is required (DEBIT or CREDIT)")
    private EntryType entryType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    private BigDecimal amount;
}
