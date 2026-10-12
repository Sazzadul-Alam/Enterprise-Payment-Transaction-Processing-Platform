package com.enterprise.payment.payment.service;

import com.enterprise.payment.payment.dto.PaymentOutboxResponse;

import java.util.List;
import java.util.UUID;

public interface OutboxService {

    List<PaymentOutboxResponse> getPendingEvents(int limit);

    void markAsPublished(UUID outboxId);

    void markAsFailed(UUID outboxId);
}
