package com.enterprise.payment.payment.service;

import com.enterprise.payment.payment.dto.CreatePaymentRequest;
import com.enterprise.payment.payment.dto.PaymentResponse;
import com.enterprise.payment.payment.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey, String correlationId);

    PaymentResponse getPaymentById(UUID paymentId);

    PaymentResponse getPaymentByIdempotencyKey(String idempotencyKey);

    PaymentResponse authorizePayment(UUID paymentId, String correlationId);

    PaymentResponse capturePayment(UUID paymentId, String correlationId);

    PaymentResponse failPayment(UUID paymentId, String reason, String correlationId);

    PaymentResponse refundPayment(UUID paymentId, String reason, String correlationId);

    Page<PaymentResponse> getPaymentsByPayerAccount(UUID payerAccountId, Pageable pageable);

    Page<PaymentResponse> getPaymentsByPayeeAccount(UUID payeeAccountId, Pageable pageable);

    Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable);
}
