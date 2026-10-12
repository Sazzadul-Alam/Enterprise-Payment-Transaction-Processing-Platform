-- ============================================================
-- user_db  ·  V1__initial_schema.sql
-- User Service: users, accounts, addresses
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── users ────────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    auth_user_id  UUID         NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone_number  VARCHAR(20),
    date_of_birth DATE,
    kyc_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ,
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_users              PRIMARY KEY (id),
    CONSTRAINT uq_users_email        UNIQUE (email),
    CONSTRAINT uq_users_auth_user_id UNIQUE (auth_user_id),
    CONSTRAINT chk_users_kyc_status  CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_users_email       ON users (email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_auth_user   ON users (auth_user_id);
CREATE INDEX idx_users_kyc_status  ON users (kyc_status);

-- ── accounts ─────────────────────────────────────────────────
CREATE TABLE accounts (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID          NOT NULL,
    account_number VARCHAR(20)   NOT NULL,
    account_type   VARCHAR(20)   NOT NULL,
    currency       VARCHAR(3)    NOT NULL DEFAULT 'USD',
    balance        DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version        BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_accounts              PRIMARY KEY (id),
    CONSTRAINT uq_accounts_number       UNIQUE (account_number),
    CONSTRAINT fk_accounts_user         FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_accounts_balance     CHECK (balance >= 0),
    CONSTRAINT chk_accounts_type        CHECK (account_type IN ('WALLET', 'SAVINGS')),
    CONSTRAINT chk_accounts_status      CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_accounts_status  ON accounts (status) WHERE status = 'ACTIVE';

-- ── user_addresses ────────────────────────────────────────────
CREATE TABLE user_addresses (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    address_type VARCHAR(20)  NOT NULL,
    street       VARCHAR(255) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    state        VARCHAR(100),
    country      VARCHAR(2)   NOT NULL,
    postal_code  VARCHAR(20)  NOT NULL,
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_addresses      PRIMARY KEY (id),
    CONSTRAINT fk_ua_user             FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_ua_address_type    CHECK (address_type IN ('HOME', 'BILLING'))
);

CREATE INDEX idx_ua_user_id ON user_addresses (user_id);

-- ── updated_at trigger ────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
