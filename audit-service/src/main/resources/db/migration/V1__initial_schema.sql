-- ============================================================
-- audit_db  ·  V1__initial_schema.sql
-- Audit Service: tamper-evident append-only event log
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── audit_events ─────────────────────────────────────────────
-- Append-only. No UPDATE or DELETE is ever issued by the application.
-- request_payload must be sanitized of PII before insertion.
CREATE TABLE audit_events (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    correlation_id   VARCHAR(255) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    actor_id         UUID,
    actor_role       VARCHAR(50),
    resource_type    VARCHAR(50)  NOT NULL,
    resource_id      UUID,
    action           VARCHAR(50)  NOT NULL,
    ip_address       VARCHAR(45),
    user_agent       TEXT,
    request_payload  JSONB,
    response_code    INTEGER,
    outcome          VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_audit_events          PRIMARY KEY (id),
    CONSTRAINT chk_ae_outcome           CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_ae_resource_type     CHECK (resource_type IN (
        'PAYMENT', 'USER', 'ACCOUNT', 'AUTH', 'NOTIFICATION', 'LEDGER'
    ))
);

CREATE INDEX idx_ae_correlation_id  ON audit_events (correlation_id);
CREATE INDEX idx_ae_actor_id        ON audit_events (actor_id)       WHERE actor_id IS NOT NULL;
CREATE INDEX idx_ae_resource_type   ON audit_events (resource_type, resource_id);
CREATE INDEX idx_ae_created_at      ON audit_events (created_at DESC);
CREATE INDEX idx_ae_outcome         ON audit_events (outcome)         WHERE outcome = 'FAILURE';

-- ── processed_events (consumer idempotency) ──────────────────
CREATE TABLE processed_events (
    event_id     VARCHAR(255) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_ae_proc_events PRIMARY KEY (event_id)
);
