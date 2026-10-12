package com.enterprise.payment.payment.service.impl;

import com.enterprise.payment.common.constant.KafkaTopicConstants;
import com.enterprise.payment.common.exception.BusinessValidationException;
import com.enterprise.payment.common.exception.ResourceNotFoundException;
import com.enterprise.payment.payment.dto.CreatePaymentRequest;
import com.enterprise.payment.payment.dto.PaymentResponse;
import com.enterprise.payment.payment.entity.IdempotencyKey;
import com.enterprise.payment.payment.entity.Payment;
import com.enterprise.payment.payment.entity.PaymentOutbox;
import com.enterprise.payment.payment.entity.enums.OutboxStatus;
import com.enterprise.payment.payment.entity.enums.PaymentStatus;
import com.enterprise.payment.payment.repository.IdempotencyKeyRepository;
import com.enterprise.payment.payment.repository.PaymentOutboxRepository;
import com.enterprise.payment.payment.repository.PaymentRepository;
import com.enterprise.payment.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey, String correlationId) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessValidationException("Idempotency-Key header is required", "MISSING_IDEMPOTENCY_KEY");
        }

        String normalizedKey = idempotencyKey.trim();

        // Check for existing valid idempotency record
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findById(normalizedKey);
        if (existingKey.isPresent()) {
            IdempotencyKey ik = existingKey.get();
            if (ik.getExpiresAt().isAfter(OffsetDateTime.now())) {
                log.info("Idempotent replay detected for key: {} (payment: {}) [corr: {}]",
                        normalizedKey, ik.getPaymentId(), correlationId);
                Payment payment = paymentRepository.findById(ik.getPaymentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment", ik.getPaymentId()));
                return mapToResponse(payment);
            }
        }

        // Validate business invariants
        if (request.getPayerAccountId().equals(request.getPayeeAccountId())) {
            log.warn("Payment rejected: payer and payee accounts are identical: {} [corr: {}]",
                    request.getPayerAccountId(), correlationId);
            throw new BusinessValidationException(
                    "Payer account and Payee account cannot be the same",
                    "SAME_ACCOUNT_TRANSFER"
            );
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Payment amount must be greater than zero", "INVALID_AMOUNT");
        }

        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency().trim().toUpperCase()
                : "USD";

        Payment payment = Payment.builder()
                .idempotencyKey(normalizedKey)
                .payerAccountId(request.getPayerAccountId())
                .payeeAccountId(request.getPayeeAccountId())
                .amount(request.getAmount())
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .build();

        payment = paymentRepository.save(payment);

        // Transactional Outbox event
        createOutboxEvent(payment, "PAYMENT_CREATED", correlationId);

        // Record Idempotency cache record (24 hour TTL)
        Map<String, Object> cachedBody = new HashMap<>();
        cachedBody.put("id", payment.getId().toString());
        cachedBody.put("status", payment.getStatus().name());
        cachedBody.put("amount", payment.getAmount().toString());
        cachedBody.put("currency", payment.getCurrency());

        IdempotencyKey ikRecord = IdempotencyKey.builder()
                .idempotencyKey(normalizedKey)
                .paymentId(payment.getId())
                .responseCode(201)
                .responseBody(cachedBody)
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .build();

        idempotencyKeyRepository.save(ikRecord);

        log.info("Created payment {} amount={} {} [key: {}, corr: {}]",
                payment.getId(), payment.getAmount(), currency, normalizedKey, correlationId);

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByIdempotencyKey(String idempotencyKey) {
        Payment payment = paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ResourceNotFoundException("Payment with idempotency key '" + idempotencyKey + "' was not found"));
        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse authorizePayment(UUID paymentId, String correlationId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        validateTransition(payment.getStatus(), PaymentStatus.AUTHORIZED);

        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setAuthorizedAt(OffsetDateTime.now());
        payment = paymentRepository.save(payment);

        createOutboxEvent(payment, "PAYMENT_AUTHORIZED", correlationId);
        log.info("Payment {} transitioned to AUTHORIZED [corr: {}]", paymentId, correlationId);

        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse capturePayment(UUID paymentId, String correlationId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        validateTransition(payment.getStatus(), PaymentStatus.CAPTURED);

        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedAt(OffsetDateTime.now());
        payment = paymentRepository.save(payment);

        createOutboxEvent(payment, "PAYMENT_CAPTURED", correlationId);
        log.info("Payment {} transitioned to CAPTURED [corr: {}]", paymentId, correlationId);

        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse failPayment(UUID paymentId, String reason, String correlationId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        validateTransition(payment.getStatus(), PaymentStatus.FAILED);

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailedAt(OffsetDateTime.now());
        payment.setFailureReason(reason);
        payment = paymentRepository.save(payment);

        createOutboxEvent(payment, "PAYMENT_FAILED", correlationId);
        log.info("Payment {} transitioned to FAILED: {} [corr: {}]", paymentId, reason, correlationId);

        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, String reason, String correlationId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        validateTransition(payment.getStatus(), PaymentStatus.REFUNDED);

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(OffsetDateTime.now());
        payment.setFailureReason(reason);
        payment = paymentRepository.save(payment);

        createOutboxEvent(payment, "PAYMENT_REFUNDED", correlationId);
        log.info("Payment {} transitioned to REFUNDED: {} [corr: {}]", paymentId, reason, correlationId);

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByPayerAccount(UUID payerAccountId, Pageable pageable) {
        return paymentRepository.findByPayerAccountIdOrderByCreatedAtDesc(payerAccountId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByPayeeAccount(UUID payeeAccountId, Pageable pageable) {
        return paymentRepository.findByPayeeAccountIdOrderByCreatedAtDesc(payeeAccountId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::mapToResponse);
    }

    private void validateTransition(PaymentStatus current, PaymentStatus target) {
        boolean valid = switch (current) {
            case PENDING -> (target == PaymentStatus.AUTHORIZED || target == PaymentStatus.FAILED);
            case AUTHORIZED -> (target == PaymentStatus.CAPTURED || target == PaymentStatus.FAILED);
            case CAPTURED -> (target == PaymentStatus.REFUNDED);
            case FAILED, REFUNDED -> false;
        };

        if (!valid) {
            throw new BusinessValidationException(
                    String.format("Invalid payment state transition from %s to %s", current, target),
                    "INVALID_STATE_TRANSITION"
            );
        }
    }

    private void createOutboxEvent(Payment payment, String eventType, String correlationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", payment.getId().toString());
        payload.put("idempotencyKey", payment.getIdempotencyKey());
        payload.put("payerAccountId", payment.getPayerAccountId().toString());
        payload.put("payeeAccountId", payment.getPayeeAccountId().toString());
        payload.put("amount", payment.getAmount().toString());
        payload.put("currency", payment.getCurrency());
        payload.put("status", payment.getStatus().name());
        payload.put("correlationId", correlationId);
        payload.put("timestamp", Instant.now().toString());

        PaymentOutbox outbox = PaymentOutbox.builder()
                .payment(payment)
                .eventType(eventType)
                .topic(KafkaTopicConstants.PAYMENT_EVENTS)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build();

        outboxRepository.save(outbox);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .idempotencyKey(payment.getIdempotencyKey())
                .payerAccountId(payment.getPayerAccountId())
                .payeeAccountId(payment.getPayeeAccountId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .description(payment.getDescription())
                .failureReason(payment.getFailureReason())
                .metadata(payment.getMetadata())
                .authorizedAt(payment.getAuthorizedAt())
                .capturedAt(payment.getCapturedAt())
                .failedAt(payment.getFailedAt())
                .refundedAt(payment.getRefundedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
