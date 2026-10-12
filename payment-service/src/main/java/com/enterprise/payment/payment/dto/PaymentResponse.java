package com.enterprise.payment.payment.dto;

import com.enterprise.payment.payment.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private String idempotencyKey;
    private UUID payerAccountId;
    private UUID payeeAccountId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String description;
    private String failureReason;
    private Map<String, Object> metadata;
    private OffsetDateTime authorizedAt;
    private OffsetDateTime capturedAt;
    private OffsetDateTime failedAt;
    private OffsetDateTime refundedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
