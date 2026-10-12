package com.enterprise.payment.payment.service.impl;

import com.enterprise.payment.common.exception.ResourceNotFoundException;
import com.enterprise.payment.payment.dto.PaymentOutboxResponse;
import com.enterprise.payment.payment.entity.PaymentOutbox;
import com.enterprise.payment.payment.entity.enums.OutboxStatus;
import com.enterprise.payment.payment.repository.PaymentOutboxRepository;
import com.enterprise.payment.payment.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final PaymentOutboxRepository outboxRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentOutboxResponse> getPendingEvents(int limit) {
        return outboxRepository.findPendingEvents(OutboxStatus.PENDING, limit).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void markAsPublished(UUID outboxId) {
        PaymentOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOutbox", outboxId));

        outbox.setStatus(OutboxStatus.PUBLISHED);
        outbox.setPublishedAt(OffsetDateTime.now());
        outboxRepository.save(outbox);
        log.info("Marked outbox event {} as PUBLISHED", outboxId);
    }

    @Override
    @Transactional
    public void markAsFailed(UUID outboxId) {
        PaymentOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOutbox", outboxId));

        outbox.setStatus(OutboxStatus.FAILED);
        outbox.setRetryCount(outbox.getRetryCount() + 1);
        outboxRepository.save(outbox);
        log.warn("Marked outbox event {} as FAILED (retryCount={})", outboxId, outbox.getRetryCount());
    }

    private PaymentOutboxResponse mapToResponse(PaymentOutbox outbox) {
        return PaymentOutboxResponse.builder()
                .id(outbox.getId())
                .paymentId(outbox.getPayment().getId())
                .eventType(outbox.getEventType())
                .topic(outbox.getTopic())
                .payload(outbox.getPayload())
                .status(outbox.getStatus())
                .retryCount(outbox.getRetryCount())
                .publishedAt(outbox.getPublishedAt())
                .createdAt(outbox.getCreatedAt())
                .build();
    }
}
