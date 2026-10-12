package com.enterprise.payment.ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Tracks Kafka messages already processed by this service.
 * Before processing any event, the consumer checks this table.
 * If the event_id is already present, the message is skipped (at-least-once → exactly-once).
 */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    /** Kafka message key or composite "topic-partition-offset" */
    @Id
    @Column(name = "event_id", length = 255, updatable = false, nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;
}
