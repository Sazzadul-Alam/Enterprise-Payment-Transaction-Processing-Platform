package com.enterprise.payment.audit.entity;

import com.enterprise.payment.audit.entity.enums.AuditOutcome;
import com.enterprise.payment.audit.entity.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Tamper-evident, append-only audit log entry.
 * NEVER issue UPDATE or DELETE against this table from application code.
 * {@code requestPayload} must be sanitized of PII (passwords, card numbers) before persistence.
 */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "correlation_id", nullable = false, length = 255, updatable = false)
    private String correlationId;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    /** UUID of the authenticated user who triggered the event — null for system events */
    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_role", length = 50, updatable = false)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50, updatable = false)
    private ResourceType resourceType;

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @Column(name = "action", nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT", updatable = false)
    private String userAgent;

    /**
     * Sanitized request snapshot — must NOT contain passwords, tokens, or card numbers.
     * The audit-service or upstream scrubber is responsible for redaction.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> requestPayload;

    @Column(name = "response_code", updatable = false)
    private Integer responseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20, updatable = false)
    private AuditOutcome outcome;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
