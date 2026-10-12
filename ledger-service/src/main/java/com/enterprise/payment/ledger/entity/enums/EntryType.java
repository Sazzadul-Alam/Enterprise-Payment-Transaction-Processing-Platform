package com.enterprise.payment.ledger.entity.enums;

/**
 * Debit or Credit leg of a double-entry journal line.
 * Invariant: SUM(DEBIT) == SUM(CREDIT) per JournalEntry.
 */
public enum EntryType {
    DEBIT,
    CREDIT
}
