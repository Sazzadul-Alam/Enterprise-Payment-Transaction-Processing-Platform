package com.enterprise.payment.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Consumer idempotency deduplication table for notification-service.
 * Prevents the same Kafka event from triggering duplicate notifications.
 */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 255, updatable = false, nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;
}
