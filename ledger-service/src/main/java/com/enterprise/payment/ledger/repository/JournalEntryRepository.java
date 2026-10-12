package com.enterprise.payment.ledger.repository;

import com.enterprise.payment.ledger.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByPaymentId(UUID paymentId);

    boolean existsByPaymentId(UUID paymentId);

    Page<JournalEntry> findByEntryDateBetweenOrderByEntryDateDesc(
            LocalDate startDate, LocalDate endDate, Pageable pageable);
}
