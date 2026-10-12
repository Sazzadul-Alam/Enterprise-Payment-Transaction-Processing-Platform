package com.enterprise.payment.ledger.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJournalEntryRequest {

    @NotNull(message = "Payment ID is required")
    private UUID paymentId;

    private LocalDate entryDate;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-character ISO code")
    private String currency;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0001", message = "Total amount must be greater than zero")
    private BigDecimal totalAmount;

    @NotNull(message = "Ledger lines list is required")
    @Size(min = 2, message = "At least two ledger lines are required for double-entry bookkeeping")
    @Valid
    private List<LedgerLineRequest> lines;
}
