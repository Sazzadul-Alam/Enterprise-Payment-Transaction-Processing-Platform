package com.enterprise.payment.payment.controller;

import com.enterprise.payment.common.constant.HeaderConstants;
import com.enterprise.payment.common.dto.ApiResponse;
import com.enterprise.payment.payment.dto.CreatePaymentRequest;
import com.enterprise.payment.payment.dto.PaymentOutboxResponse;
import com.enterprise.payment.payment.dto.PaymentResponse;
import com.enterprise.payment.payment.dto.TransitionPaymentRequest;
import com.enterprise.payment.payment.service.OutboxService;
import com.enterprise.payment.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OutboxService outboxService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = HeaderConstants.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        PaymentResponse response = paymentService.createPayment(request, idempotencyKey, cid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment created successfully", cid));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable UUID paymentId,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment retrieved successfully", cid));
    }

    @GetMapping("/by-idempotency/{key}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByIdempotencyKey(
            @PathVariable String key,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        PaymentResponse response = paymentService.getPaymentByIdempotencyKey(key);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment retrieved successfully", cid));
    }

    @PostMapping("/{paymentId}/authorize")
    public ResponseEntity<ApiResponse<PaymentResponse>> authorizePayment(
            @PathVariable UUID paymentId,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        PaymentResponse response = paymentService.authorizePayment(paymentId, cid);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment authorized successfully", cid));
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<ApiResponse<PaymentResponse>> capturePayment(
            @PathVariable UUID paymentId,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        PaymentResponse response = paymentService.capturePayment(paymentId, cid);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment captured successfully", cid));
    }

    @PostMapping("/{paymentId}/fail")
    public ResponseEntity<ApiResponse<PaymentResponse>> failPayment(
            @PathVariable UUID paymentId,
            @RequestBody(required = false) TransitionPaymentRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Payment failed";
        PaymentResponse response = paymentService.failPayment(paymentId, reason, cid);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment marked as failed", cid));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable UUID paymentId,
            @RequestBody(required = false) TransitionPaymentRequest request,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Payment refunded";
        PaymentResponse response = paymentService.refundPayment(paymentId, reason, cid);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment refunded successfully", cid));
    }

    @GetMapping("/payer/{payerAccountId}")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentsByPayer(
            @PathVariable UUID payerAccountId,
            Pageable pageable,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        Page<PaymentResponse> response = paymentService.getPaymentsByPayerAccount(payerAccountId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Payer payments retrieved successfully", cid));
    }

    @GetMapping("/payee/{payeeAccountId}")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentsByPayee(
            @PathVariable UUID payeeAccountId,
            Pageable pageable,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        Page<PaymentResponse> response = paymentService.getPaymentsByPayeeAccount(payeeAccountId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Payee payments retrieved successfully", cid));
    }

    @GetMapping("/outbox/pending")
    public ResponseEntity<ApiResponse<List<PaymentOutboxResponse>>> getPendingOutbox(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        List<PaymentOutboxResponse> response = outboxService.getPendingEvents(limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Pending outbox events retrieved successfully", cid));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getStatus(
            @RequestHeader(value = HeaderConstants.CORRELATION_ID, required = false) String correlationId) {

        String cid = resolveCorrelationId(correlationId);
        Map<String, String> status = Map.of(
                "service", "payment-service",
                "status", "OPERATIONAL"
        );
        return ResponseEntity.ok(ApiResponse.success(status, "Payment service operational", cid));
    }

    private String resolveCorrelationId(String correlationId) {
        return StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString();
    }
}
