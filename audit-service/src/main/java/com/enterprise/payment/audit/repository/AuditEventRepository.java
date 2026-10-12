package com.enterprise.payment.audit.repository;

import com.enterprise.payment.audit.entity.AuditEvent;
import com.enterprise.payment.audit.entity.enums.AuditOutcome;
import com.enterprise.payment.audit.entity.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByCorrelationId(String correlationId);

    Page<AuditEvent> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    Page<AuditEvent> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            ResourceType resourceType, UUID resourceId, Pageable pageable);

    Page<AuditEvent> findByOutcomeAndCreatedAtBetweenOrderByCreatedAtDesc(
            AuditOutcome outcome, OffsetDateTime start, OffsetDateTime end, Pageable pageable);
}
