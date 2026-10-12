package com.enterprise.payment.payment.repository;

import com.enterprise.payment.payment.entity.Payment;
import com.enterprise.payment.payment.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Page<Payment> findByPayerAccountIdOrderByCreatedAtDesc(UUID payerAccountId, Pageable pageable);

    Page<Payment> findByPayeeAccountIdOrderByCreatedAtDesc(UUID payeeAccountId, Pageable pageable);

    Page<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);
}
