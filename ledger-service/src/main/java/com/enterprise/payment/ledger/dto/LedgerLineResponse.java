package com.enterprise.payment.ledger.dto;

import com.enterprise.payment.ledger.entity.enums.EntryType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerLineResponse {

    private UUID id;
    private UUID accountId;
    private EntryType entryType;
    private BigDecimal amount;
    private OffsetDateTime createdAt;
}
