package com.enterprise.payment.notification.repository;

import com.enterprise.payment.notification.entity.Notification;
import com.enterprise.payment.notification.entity.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    List<Notification> findByPaymentId(UUID paymentId);

    /**
     * Fetch notifications ready for retry:
     * status = PENDING, retry_count < max_retries, next_retry_at <= now.
     */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.status = :status AND n.retryCount < n.maxRetries AND n.nextRetryAt <= :now " +
           "ORDER BY n.nextRetryAt ASC")
    List<Notification> findRetryableNotifications(
            @Param("status") NotificationStatus status,
            @Param("now") OffsetDateTime now);
}
