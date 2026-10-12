package com.enterprise.payment.ledger.repository;

import com.enterprise.payment.ledger.entity.LedgerLine;
import com.enterprise.payment.ledger.entity.enums.EntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerLineRepository extends JpaRepository<LedgerLine, UUID> {

    List<LedgerLine> findByJournalEntryId(UUID journalEntryId);

    List<LedgerLine> findByAccountId(UUID accountId);

    /**
     * Calculate the net balance for an account across all journal entries.
     * Returns SUM(CREDIT amounts) - SUM(DEBIT amounts) for the given account.
     */
    @Query("SELECT SUM(CASE WHEN ll.entryType = com.enterprise.payment.ledger.entity.enums.EntryType.CREDIT THEN ll.amount ELSE -ll.amount END) " +
           "FROM LedgerLine ll WHERE ll.accountId = :accountId")
    BigDecimal sumNetBalance(@Param("accountId") UUID accountId);

    default BigDecimal calculateNetBalance(UUID accountId) {
        BigDecimal sum = sumNetBalance(accountId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * Validate the double-entry invariant for a journal entry:
     * SUM(DEBIT) must equal SUM(CREDIT).
     */
    @Query("SELECT COALESCE(SUM(ll.amount), 0) FROM LedgerLine ll " +
           "WHERE ll.journalEntry.id = :journalEntryId AND ll.entryType = :entryType")
    BigDecimal sumByJournalEntryIdAndEntryType(
            @Param("journalEntryId") UUID journalEntryId,
            @Param("entryType") EntryType entryType);
}
