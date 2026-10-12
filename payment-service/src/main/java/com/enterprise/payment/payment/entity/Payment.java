package com.enterprise.payment.payment.entity;

import com.enterprise.payment.payment.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core payment record.
 * Idempotency is enforced via the unique {@code idempotency_key}.
 * State transitions are validated in the service layer — never via direct field mutation.
 */
@Entity
@Table(
    name = "payments",
    uniqueConstraints = @UniqueConstraint(name = "uq_payments_idempotency", columnNames = "idempotency_key")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 255, updatable = false)
    private String idempotencyKey;

    /** Logical reference to accounts.id in user-service — no FK across services */
    @Column(name = "payer_account_id", nullable = false, updatable = false)
    private UUID payerAccountId;

    /** Logical reference to accounts.id in user-service — no FK across services */
    @Column(name = "payee_account_id", nullable = false, updatable = false)
    private UUID payeeAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "authorized_at")
    private OffsetDateTime authorizedAt;

    @Column(name = "captured_at")
    private OffsetDateTime capturedAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PaymentOutbox> outboxEvents = new ArrayList<>();
}
