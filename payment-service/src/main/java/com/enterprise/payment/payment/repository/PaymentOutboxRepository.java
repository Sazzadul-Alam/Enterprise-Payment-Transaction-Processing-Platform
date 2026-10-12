package com.enterprise.payment.payment.repository;

import com.enterprise.payment.payment.entity.PaymentOutbox;
import com.enterprise.payment.payment.entity.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, UUID> {

    /**
     * Fetch the next batch of un-published outbox events for the relay to process.
     * Ordered by creation time (FIFO) with a configurable limit.
     */
    @Query("SELECT o FROM PaymentOutbox o WHERE o.status = :status ORDER BY o.createdAt ASC LIMIT :limit")
    List<PaymentOutbox> findPendingEvents(@Param("status") OutboxStatus status, @Param("limit") int limit);

    List<PaymentOutbox> findByPaymentId(UUID paymentId);
}
