package com.enterprise.payment.ledger.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryResponse {

    private UUID id;
    private UUID paymentId;
    private LocalDate entryDate;
    private String description;
    private String currency;
    private BigDecimal totalAmount;
    private OffsetDateTime createdAt;
    private List<LedgerLineResponse> lines;
}
