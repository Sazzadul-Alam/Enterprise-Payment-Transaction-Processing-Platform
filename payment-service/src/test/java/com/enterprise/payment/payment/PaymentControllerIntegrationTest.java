package com.enterprise.payment.payment;

import com.enterprise.payment.common.constant.HeaderConstants;
import com.enterprise.payment.payment.dto.CreatePaymentRequest;
import com.enterprise.payment.payment.dto.TransitionPaymentRequest;
import com.enterprise.payment.payment.entity.PaymentOutbox;
import com.enterprise.payment.payment.entity.enums.OutboxStatus;
import com.enterprise.payment.payment.entity.enums.PaymentStatus;
import com.enterprise.payment.payment.repository.IdempotencyKeyRepository;
import com.enterprise.payment.payment.repository.PaymentOutboxRepository;
import com.enterprise.payment.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create payment and publish outbox event in the same transaction")
    void shouldCreatePaymentAndPublishOutboxEvent() throws Exception {
        UUID payer = UUID.randomUUID();
        UUID payee = UUID.randomUUID();
        String idempotencyKey = "key-test-create-001";

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .payerAccountId(payer)
                .payeeAccountId(payee)
                .amount(new BigDecimal("250.0000"))
                .currency("USD")
                .description("Invoice payment")
                .metadata(Map.of("merchantId", "m-123"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .header(HeaderConstants.CORRELATION_ID, "corr-pay-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.correlationId").value("corr-pay-1"))
                .andExpect(jsonPath("$.data.idempotencyKey").value(idempotencyKey))
                .andExpect(jsonPath("$.data.amount").value(250.0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID paymentId = UUID.fromString(root.path("data").path("id").asText());

        // Verify outbox row exists
        List<PaymentOutbox> outboxEvents = outboxRepository.findByPaymentId(paymentId);
        assertThat(outboxEvents).hasSize(1);
        PaymentOutbox outbox = outboxEvents.get(0);
        assertThat(outbox.getEventType()).isEqualTo("PAYMENT_CREATED");
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getTopic()).isEqualTo("payment.events");

        // Verify idempotency record exists
        assertThat(idempotencyKeyRepository.findById(idempotencyKey)).isPresent();
    }

    @Test
    @DisplayName("Should return cached payment without duplicate outbox event on idempotent replay")
    void shouldReturnCachedPaymentOnIdempotentReplay() throws Exception {
        UUID payer = UUID.randomUUID();
        UUID payee = UUID.randomUUID();
        String idempotencyKey = "key-test-replay-002";

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .payerAccountId(payer)
                .payeeAccountId(payee)
                .amount(new BigDecimal("100.0000"))
                .currency("USD")
                .build();

        // First attempt -> creates payment
        MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstPaymentId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Second attempt with same key -> idempotent replay
        MvcResult secondResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String secondPaymentId = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        assertThat(secondPaymentId).isEqualTo(firstPaymentId);

        // Verify exactly one payment and one outbox event exist
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject payment without Idempotency-Key header (400 Bad Request)")
    void shouldRejectPaymentWithoutIdempotencyKey() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .payerAccountId(UUID.randomUUID())
                .payeeAccountId(UUID.randomUUID())
                .amount(new BigDecimal("50.0000"))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_IDEMPOTENCY_KEY"));
    }

    @Test
    @DisplayName("Should reject transfer between identical payer and payee accounts (400 Bad Request)")
    void shouldRejectTransferToSameAccount() throws Exception {
        UUID sharedAccount = UUID.randomUUID();
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .payerAccountId(sharedAccount)
                .payeeAccountId(sharedAccount)
                .amount(new BigDecimal("50.0000"))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, "key-same-account")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SAME_ACCOUNT_TRANSFER"));
    }

    @Test
    @DisplayName("Should reject zero or negative amount (400 Bad Request)")
    void shouldRejectZeroAmount() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .payerAccountId(UUID.randomUUID())
                .payeeAccountId(UUID.randomUUID())
                .amount(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, "key-zero-amount")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should execute full happy path state lifecycle: PENDING -> AUTHORIZED -> CAPTURED -> REFUNDED")
    void shouldExecuteFullHappyPathLifecycle() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .payerAccountId(UUID.randomUUID())
                .payeeAccountId(UUID.randomUUID())
                .amount(new BigDecimal("500.0000"))
                .currency("USD")
                .build();

        // 1. Create -> PENDING
        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, "key-lifecycle-001")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String paymentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 2. Authorize -> AUTHORIZED
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/authorize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.data.authorizedAt").isNotEmpty());

        // 3. Capture -> CAPTURED
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/capture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CAPTURED"))
                .andExpect(jsonPath("$.data.capturedAt").isNotEmpty());

        // 4. Refund -> REFUNDED
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionPaymentRequest("Customer requested refund"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refundedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.failureReason").value("Customer requested refund"));

        // Verify outbox has all 4 lifecycle events
        List<PaymentOutbox> events = outboxRepository.findByPaymentId(UUID.fromString(paymentId));
        assertThat(events).hasSize(4);
        List<String> eventTypes = events.stream().map(PaymentOutbox::getEventType).toList();
        assertThat(eventTypes).containsExactly(
                "PAYMENT_CREATED",
                "PAYMENT_AUTHORIZED",
                "PAYMENT_CAPTURED",
                "PAYMENT_REFUNDED"
        );
    }

    @Test
    @DisplayName("Should support failure transitions from PENDING and AUTHORIZED")
    void shouldSupportFailureTransitions() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .payerAccountId(UUID.randomUUID())
                .payeeAccountId(UUID.randomUUID())
                .amount(new BigDecimal("75.0000"))
                .build();

        // 1. Direct fail from PENDING
        MvcResult res1 = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, "key-fail-pending")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String id1 = objectMapper.readTree(res1.getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/payments/" + id1 + "/fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionPaymentRequest("Insufficient balance"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failureReason").value("Insufficient balance"));

        // 2. Fail from AUTHORIZED
        MvcResult res2 = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, "key-fail-auth")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String id2 = objectMapper.readTree(res2.getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/payments/" + id2 + "/authorize"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments/" + id2 + "/fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionPaymentRequest("Capture window expired"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failureReason").value("Capture window expired"));
    }

    @Test
    @DisplayName("Should reject invalid state transitions (400 Bad Request)")
    void shouldRejectInvalidStateTransitions() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .payerAccountId(UUID.randomUUID())
                .payeeAccountId(UUID.randomUUID())
                .amount(new BigDecimal("200.0000"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, "key-invalid-transitions")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String paymentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 1. Direct PENDING -> CAPTURED is invalid
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/capture"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));

        // 2. Direct PENDING -> REFUNDED is invalid
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransitionPaymentRequest("Invalid"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @DisplayName("Should query pending outbox events")
    void shouldQueryPendingOutboxEvents() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .payerAccountId(UUID.randomUUID())
                .payeeAccountId(UUID.randomUUID())
                .amount(new BigDecimal("30.0000"))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.IDEMPOTENCY_KEY, "key-outbox-query")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/payments/outbox/pending?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].eventType").value("PAYMENT_CREATED"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].topic").value("payment.events"));
    }
}
