package com.enterprise.payment.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Caches the first-seen response for each idempotency key for 24 hours.
 * If the same key arrives again within the TTL, the cached response is returned
 * without re-processing the payment.
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key", length = 255, updatable = false, nullable = false)
    private String idempotencyKey;

    /** The payment that was created on first processing of this key */
    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "response_code", nullable = false, updatable = false)
    private int responseCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> responseBody;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
