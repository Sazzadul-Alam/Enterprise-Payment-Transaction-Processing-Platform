-- ============================================================
-- notification_db  ·  V1__initial_schema.sql
-- Notification Service: delivery tracking, consumer deduplication
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── notifications ─────────────────────────────────────────────
CREATE TABLE notifications (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    recipient_id   UUID         NOT NULL,
    payment_id     UUID,
    channel        VARCHAR(20)  NOT NULL,
    subject        VARCHAR(255),
    body           TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER      NOT NULL DEFAULT 0,
    max_retries    INTEGER      NOT NULL DEFAULT 3,
    next_retry_at  TIMESTAMPTZ,
    sent_at        TIMESTAMPTZ,
    failed_at      TIMESTAMPTZ,
    failure_reason TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_notifications           PRIMARY KEY (id),
    CONSTRAINT chk_notifications_channel  CHECK (channel IN ('EMAIL', 'WEBHOOK', 'SMS')),
    CONSTRAINT chk_notifications_status   CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT chk_notifications_retries  CHECK (retry_count >= 0 AND max_retries >= 0)
);

CREATE INDEX idx_notif_recipient_id ON notifications (recipient_id);
CREATE INDEX idx_notif_status       ON notifications (status);
-- Partial index for retry scheduler
CREATE INDEX idx_notif_retry        ON notifications (next_retry_at ASC)
    WHERE status = 'PENDING' AND retry_count < max_retries;

-- ── processed_events (consumer idempotency) ──────────────────
CREATE TABLE processed_events (
    event_id     VARCHAR(255) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_proc_events PRIMARY KEY (event_id)
);

-- ── updated_at trigger ────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
