package com.enterprise.payment.ledger.entity;

import com.enterprise.payment.ledger.entity.enums.EntryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Individual debit or credit leg of a {@link JournalEntry}.
 * Invariant: SUM(amount WHERE entry_type=DEBIT) == SUM(amount WHERE entry_type=CREDIT)
 *            per journal_entry_id.
 * NEVER issue UPDATE or DELETE against this table from application code.
 */
@Entity
@Table(name = "ledger_lines")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "journal_entry_id",
        nullable = false,
        updatable = false,
        foreignKey = @ForeignKey(name = "fk_ll_journal_entry")
    )
    private JournalEntry journalEntry;

    /** Cross-service reference to accounts.id — no FK */
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10, updatable = false)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
