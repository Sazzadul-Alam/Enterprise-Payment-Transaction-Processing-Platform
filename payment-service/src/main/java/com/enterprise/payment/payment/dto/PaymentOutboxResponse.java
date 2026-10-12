package com.enterprise.payment.payment.dto;

import com.enterprise.payment.payment.entity.enums.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOutboxResponse {

    private UUID id;
    private UUID paymentId;
    private String eventType;
    private String topic;
    private Map<String, Object> payload;
    private OutboxStatus status;
    private int retryCount;
    private OffsetDateTime publishedAt;
    private OffsetDateTime createdAt;
}
