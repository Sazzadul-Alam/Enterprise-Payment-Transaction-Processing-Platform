-- ============================================================
-- auth_db  ·  V1__initial_schema.sql
-- Auth Service: users, roles, refresh tokens
-- ============================================================

-- Extension for UUID generation (PostgreSQL 13+ has gen_random_uuid())
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── roles ────────────────────────────────────────────────────
CREATE TABLE roles (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_roles      PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

-- Seed default roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER',    'Standard authenticated user'),
    ('ROLE_ADMIN',   'Platform administrator'),
    ('ROLE_AUDITOR', 'Read-only audit access'),
    ('ROLE_SERVICE', 'Internal service-to-service identity');

-- ── auth_users ───────────────────────────────────────────────
CREATE TABLE auth_users (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_locked        BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_attempts  INTEGER      NOT NULL DEFAULT 0,
    locked_until     TIMESTAMPTZ,
    last_login_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version          BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_auth_users       PRIMARY KEY (id),
    CONSTRAINT uq_auth_users_email UNIQUE (email),
    CONSTRAINT chk_failed_attempts CHECK (failed_attempts >= 0)
);

CREATE INDEX idx_auth_users_email    ON auth_users (email);
CREATE INDEX idx_auth_users_is_active ON auth_users (is_active) WHERE is_active = TRUE;

-- ── auth_user_roles ──────────────────────────────────────────
CREATE TABLE auth_user_roles (
    auth_user_id UUID        NOT NULL,
    role_id      UUID        NOT NULL,
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_auth_user_roles PRIMARY KEY (auth_user_id, role_id),
    CONSTRAINT fk_aur_user        FOREIGN KEY (auth_user_id) REFERENCES auth_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_aur_role        FOREIGN KEY (role_id)      REFERENCES roles       (id) ON DELETE RESTRICT
);

CREATE INDEX idx_aur_user_id ON auth_user_roles (auth_user_id);

-- ── refresh_tokens ───────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    auth_user_id  UUID         NOT NULL,
    token_hash    VARCHAR(255) NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_refresh_tokens       PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_hash  UNIQUE (token_hash),
    CONSTRAINT fk_rt_auth_user         FOREIGN KEY (auth_user_id) REFERENCES auth_users (id) ON DELETE CASCADE
);

CREATE INDEX idx_rt_auth_user_id ON refresh_tokens (auth_user_id);
CREATE INDEX idx_rt_expires_at   ON refresh_tokens (expires_at)   WHERE revoked = FALSE;

-- ── updated_at auto-update trigger ───────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_auth_users_updated_at
    BEFORE UPDATE ON auth_users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
