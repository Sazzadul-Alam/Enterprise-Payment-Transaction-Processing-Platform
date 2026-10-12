-- ============================================================
-- ledger_db  ·  V1__initial_schema.sql
-- Ledger Service: double-entry journal, consumer deduplication
-- All tables are APPEND-ONLY (no UPDATE, no DELETE in application)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── journal_entries ──────────────────────────────────────────
-- One row per financial transaction; groups the debit+credit legs.
CREATE TABLE journal_entries (
    id           UUID          NOT NULL DEFAULT gen_random_uuid(),
    payment_id   UUID          NOT NULL,
    entry_date   DATE          NOT NULL,
    description  TEXT          NOT NULL,
    currency     VARCHAR(3)    NOT NULL,
    total_amount DECIMAL(19,4) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_journal_entries         PRIMARY KEY (id),
    CONSTRAINT uq_je_payment_id           UNIQUE (payment_id),
    CONSTRAINT chk_je_total_amount        CHECK (total_amount > 0)
);

CREATE INDEX idx_je_payment_id  ON journal_entries (payment_id);
CREATE INDEX idx_je_entry_date  ON journal_entries (entry_date DESC);

-- ── ledger_lines ─────────────────────────────────────────────
-- Individual debit / credit legs.
-- Invariant: SUM(DEBIT) = SUM(CREDIT) per journal_entry_id.
CREATE TABLE ledger_lines (
    id               UUID          NOT NULL DEFAULT gen_random_uuid(),
    journal_entry_id UUID          NOT NULL,
    account_id       UUID          NOT NULL,
    entry_type       VARCHAR(10)   NOT NULL,
    amount           DECIMAL(19,4) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_ledger_lines             PRIMARY KEY (id),
    CONSTRAINT fk_ll_journal_entry         FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ll_entry_type           CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ll_amount               CHECK (amount > 0)
);

CREATE INDEX idx_ll_journal_entry_id ON ledger_lines (journal_entry_id);
CREATE INDEX idx_ll_account_id       ON ledger_lines (account_id);

-- ── processed_events (consumer idempotency) ──────────────────
-- Prevents duplicate Kafka message processing.
CREATE TABLE processed_events (
    event_id     VARCHAR(255) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_processed_events PRIMARY KEY (event_id)
);
