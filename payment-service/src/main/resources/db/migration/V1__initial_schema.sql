-- ============================================================
-- payment_db  ·  V1__initial_schema.sql
-- Payment Service: payments, outbox, idempotency
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── payments ─────────────────────────────────────────────────
CREATE TABLE payments (
    id                UUID          NOT NULL DEFAULT gen_random_uuid(),
    idempotency_key   VARCHAR(255)  NOT NULL,
    payer_account_id  UUID          NOT NULL,
    payee_account_id  UUID          NOT NULL,
    amount            DECIMAL(19,4) NOT NULL,
    currency          VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    description       TEXT,
    failure_reason    TEXT,
    metadata          JSONB,
    authorized_at     TIMESTAMPTZ,
    captured_at       TIMESTAMPTZ,
    failed_at         TIMESTAMPTZ,
    refunded_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version           BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_payments              PRIMARY KEY (id),
    CONSTRAINT uq_payments_idempotency  UNIQUE (idempotency_key),
    CONSTRAINT chk_payments_amount      CHECK (amount > 0),
    CONSTRAINT chk_payments_status      CHECK (status IN (
        'PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED'
    )),
    CONSTRAINT chk_payments_different_accounts CHECK (payer_account_id != payee_account_id)
);

CREATE INDEX idx_payments_payer_account   ON payments (payer_account_id);
CREATE INDEX idx_payments_payee_account   ON payments (payee_account_id);
CREATE INDEX idx_payments_status          ON payments (status);
CREATE INDEX idx_payments_created_at      ON payments (created_at DESC);

-- ── payment_outbox (Transactional Outbox Pattern) ────────────
-- Relay process polls this table and publishes to Kafka.
-- Each row is published exactly once then marked PUBLISHED.
CREATE TABLE payment_outbox (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    payment_id   UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    topic        VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count  INTEGER      NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_payment_outbox        PRIMARY KEY (id),
    CONSTRAINT fk_outbox_payment        FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE RESTRICT,
    CONSTRAINT chk_outbox_status        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT chk_outbox_retry_count   CHECK (retry_count >= 0)
);

-- Partial index: only un-published events need polling
CREATE INDEX idx_outbox_pending ON payment_outbox (created_at ASC) WHERE status = 'PENDING';

-- ── idempotency_keys ─────────────────────────────────────────
-- Caches the first response for each idempotency key for 24 h.
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) NOT NULL,
    payment_id      UUID         NOT NULL,
    response_code   INTEGER      NOT NULL,
    response_body   JSONB        NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_idempotency_keys  PRIMARY KEY (idempotency_key),
    CONSTRAINT fk_ik_payment        FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE
);

CREATE INDEX idx_ik_expires_at ON idempotency_keys (expires_at);

-- ── updated_at trigger ────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
