# Relational Data Model & ERD — Enterprise Payment Platform

## Design Principles

- **Database-per-service**: Each service owns its schema; no cross-service table joins.
- **No shared tables**: Inter-service data access only via REST APIs or Kafka events.
- **Immutability**: Ledger entries, audit records, and outbox events are append-only.
- **Optimistic locking**: `version` column on mutable financial entities.
- **Soft delete**: `deleted_at` timestamp instead of hard deletes on user/account data.
- **Audit metadata**: `created_at`, `updated_at` on all tables.
- **UUIDs as PK**: All primary keys use UUID (v4) to support distributed ID generation.

---

## 1. Auth Service Database (`auth_db`)

```
┌─────────────────────────────────────────────────────────────────┐
│                          auth_users                              │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ email            │ VARCHAR(255) │ UNIQUE NOT NULL               │
│ password_hash    │ VARCHAR(255) │ NOT NULL (Argon2/BCrypt)      │
│ is_active        │ BOOLEAN      │ DEFAULT TRUE                  │
│ is_locked        │ BOOLEAN      │ DEFAULT FALSE                 │
│ failed_attempts  │ INTEGER      │ DEFAULT 0                     │
│ locked_until     │ TIMESTAMPTZ  │ NULLABLE                      │
│ last_login_at    │ TIMESTAMPTZ  │ NULLABLE                      │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
│ updated_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
│ version          │ BIGINT       │ DEFAULT 0 (optimistic lock)   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       refresh_tokens                             │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ auth_user_id     │ UUID         │ FK → auth_users.id            │
│ token_hash       │ VARCHAR(255) │ UNIQUE NOT NULL               │
│ expires_at       │ TIMESTAMPTZ  │ NOT NULL                      │
│ revoked          │ BOOLEAN      │ DEFAULT FALSE                 │
│ revoked_at       │ TIMESTAMPTZ  │ NULLABLE                      │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         roles                                    │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ name             │ VARCHAR(50)  │ UNIQUE NOT NULL               │
│                  │              │ (ROLE_USER, ROLE_ADMIN,       │
│                  │              │  ROLE_AUDITOR, ROLE_SERVICE)  │
│ description      │ TEXT         │ NULLABLE                      │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       auth_user_roles                            │
├──────────────────┬──────────────┬────────────────────────────── │
│ auth_user_id     │ UUID         │ PK, FK → auth_users.id        │
│ role_id          │ UUID         │ PK, FK → roles.id             │
│ assigned_at      │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

Relationships:
  auth_users ──< refresh_tokens  (one-to-many)
  auth_users >──< roles          (many-to-many via auth_user_roles)
```

---

## 2. User Service Database (`user_db`)

```
┌─────────────────────────────────────────────────────────────────┐
│                          users                                   │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ auth_user_id     │ UUID         │ UNIQUE NOT NULL (ref to auth) │
│ first_name       │ VARCHAR(100) │ NOT NULL                      │
│ last_name        │ VARCHAR(100) │ NOT NULL                      │
│ email            │ VARCHAR(255) │ UNIQUE NOT NULL               │
│ phone_number     │ VARCHAR(20)  │ NULLABLE                      │
│ date_of_birth    │ DATE         │ NULLABLE                      │
│ kyc_status       │ VARCHAR(20)  │ DEFAULT 'PENDING'             │
│                  │              │ (PENDING/VERIFIED/REJECTED)   │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
│ updated_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
│ deleted_at       │ TIMESTAMPTZ  │ NULLABLE (soft delete)        │
│ version          │ BIGINT       │ DEFAULT 0                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                          accounts                                │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ user_id          │ UUID         │ FK → users.id                 │
│ account_number   │ VARCHAR(20)  │ UNIQUE NOT NULL               │
│ account_type     │ VARCHAR(20)  │ NOT NULL (WALLET/SAVINGS)     │
│ currency         │ VARCHAR(3)   │ NOT NULL DEFAULT 'USD'        │
│ balance          │ DECIMAL(19,4)│ NOT NULL DEFAULT 0.0000       │
│ status           │ VARCHAR(20)  │ NOT NULL DEFAULT 'ACTIVE'     │
│                  │              │ (ACTIVE/SUSPENDED/CLOSED)     │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
│ updated_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
│ version          │ BIGINT       │ DEFAULT 0 (optimistic lock)   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       user_addresses                             │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ user_id          │ UUID         │ FK → users.id                 │
│ address_type     │ VARCHAR(20)  │ NOT NULL (HOME/BILLING)       │
│ street           │ VARCHAR(255) │ NOT NULL                      │
│ city             │ VARCHAR(100) │ NOT NULL                      │
│ state            │ VARCHAR(100) │ NULLABLE                      │
│ country          │ VARCHAR(2)   │ NOT NULL (ISO alpha-2)        │
│ postal_code      │ VARCHAR(20)  │ NOT NULL                      │
│ is_primary       │ BOOLEAN      │ DEFAULT FALSE                 │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

Relationships:
  users ──< accounts       (one-to-many)
  users ──< user_addresses (one-to-many)
```

---

## 3. Payment Service Database (`payment_db`)

```
┌─────────────────────────────────────────────────────────────────┐
│                         payments                                 │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ idempotency_key  │ VARCHAR(255) │ UNIQUE NOT NULL               │
│ payer_account_id │ UUID         │ NOT NULL (ref to user_db)     │
│ payee_account_id │ UUID         │ NOT NULL (ref to user_db)     │
│ amount           │ DECIMAL(19,4)│ NOT NULL CHECK (amount > 0)   │
│ currency         │ VARCHAR(3)   │ NOT NULL DEFAULT 'USD'        │
│ status           │ VARCHAR(20)  │ NOT NULL DEFAULT 'PENDING'    │
│                  │              │ PENDING → AUTHORIZED →        │
│                  │              │ CAPTURED → FAILED/REFUNDED    │
│ description      │ TEXT         │ NULLABLE                      │
│ failure_reason   │ TEXT         │ NULLABLE                      │
│ metadata         │ JSONB        │ NULLABLE                      │
│ authorized_at    │ TIMESTAMPTZ  │ NULLABLE                      │
│ captured_at      │ TIMESTAMPTZ  │ NULLABLE                      │
│ failed_at        │ TIMESTAMPTZ  │ NULLABLE                      │
│ refunded_at      │ TIMESTAMPTZ  │ NULLABLE                      │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
│ updated_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
│ version          │ BIGINT       │ DEFAULT 0 (optimistic lock)   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      payment_outbox                              │
│                (Transactional Outbox Pattern)                    │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ payment_id       │ UUID         │ FK → payments.id              │
│ event_type       │ VARCHAR(100) │ NOT NULL (e.g.PaymentCapture) │
│ payload          │ JSONB        │ NOT NULL                      │
│ topic            │ VARCHAR(255) │ NOT NULL (Kafka topic)        │
│ status           │ VARCHAR(20)  │ DEFAULT 'PENDING'             │
│                  │              │ (PENDING/PUBLISHED/FAILED)    │
│ retry_count      │ INTEGER      │ DEFAULT 0                     │
│ published_at     │ TIMESTAMPTZ  │ NULLABLE                      │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     idempotency_keys                             │
├──────────────────┬──────────────┬────────────────────────────── │
│ idempotency_key  │ VARCHAR(255) │ PK                            │
│ payment_id       │ UUID         │ NOT NULL FK → payments.id     │
│ response_code    │ INTEGER      │ NOT NULL                      │
│ response_body    │ JSONB        │ NOT NULL                      │
│ expires_at       │ TIMESTAMPTZ  │ NOT NULL                      │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

Relationships:
  payments ──< payment_outbox   (one-to-many)
  payments ─── idempotency_keys (one-to-one)

Payment State Machine:
  PENDING → AUTHORIZED → CAPTURED
           ↘            ↘ REFUNDED
            → FAILED
```

---

## 4. Ledger Service Database (`ledger_db`)

```
┌─────────────────────────────────────────────────────────────────┐
│                       journal_entries                            │
│              (Immutable double-entry records)                    │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ payment_id       │ UUID         │ NOT NULL (ref to payment_db)  │
│ entry_date       │ DATE         │ NOT NULL                      │
│ description      │ TEXT         │ NOT NULL                      │
│ currency         │ VARCHAR(3)   │ NOT NULL                      │
│ total_amount     │ DECIMAL(19,4)│ NOT NULL CHECK (amount > 0)   │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘
    │
    │ 1:N
    ▼
┌─────────────────────────────────────────────────────────────────┐
│                       ledger_lines                               │
│        (Individual debit/credit legs of a journal entry)        │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ journal_entry_id │ UUID         │ FK → journal_entries.id       │
│ account_id       │ UUID         │ NOT NULL (ref to user_db)     │
│ entry_type       │ VARCHAR(10)  │ NOT NULL (DEBIT / CREDIT)     │
│ amount           │ DECIMAL(19,4)│ NOT NULL CHECK (amount > 0)   │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    processed_events                              │
│           (Consumer idempotency deduplication table)            │
├──────────────────┬──────────────┬────────────────────────────── │
│ event_id         │ VARCHAR(255) │ PK (Kafka message key/offset) │
│ event_type       │ VARCHAR(100) │ NOT NULL                      │
│ processed_at     │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

Invariant:
  For every journal_entry:
    SUM(ledger_lines WHERE entry_type='DEBIT') = SUM(ledger_lines WHERE entry_type='CREDIT')
```

---

## 5. Notification Service Database (`notification_db`)

```
┌─────────────────────────────────────────────────────────────────┐
│                       notifications                              │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ recipient_id     │ UUID         │ NOT NULL (ref to user_db)     │
│ payment_id       │ UUID         │ NULLABLE (ref to payment_db)  │
│ channel          │ VARCHAR(20)  │ NOT NULL (EMAIL/WEBHOOK/SMS)  │
│ subject          │ VARCHAR(255) │ NULLABLE                      │
│ body             │ TEXT         │ NOT NULL                      │
│ status           │ VARCHAR(20)  │ DEFAULT 'PENDING'             │
│                  │              │ (PENDING/SENT/FAILED)         │
│ retry_count      │ INTEGER      │ DEFAULT 0                     │
│ max_retries      │ INTEGER      │ DEFAULT 3                     │
│ next_retry_at    │ TIMESTAMPTZ  │ NULLABLE                      │
│ sent_at          │ TIMESTAMPTZ  │ NULLABLE                      │
│ failed_at        │ TIMESTAMPTZ  │ NULLABLE                      │
│ failure_reason   │ TEXT         │ NULLABLE                      │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
│ updated_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    processed_events                              │
├──────────────────┬──────────────┬────────────────────────────── │
│ event_id         │ VARCHAR(255) │ PK                            │
│ event_type       │ VARCHAR(100) │ NOT NULL                      │
│ processed_at     │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Audit Service Database (`audit_db`)

```
┌─────────────────────────────────────────────────────────────────┐
│                        audit_events                              │
│                 (Tamper-evident, append-only)                    │
├──────────────────┬──────────────┬────────────────────────────── │
│ id               │ UUID         │ PK                            │
│ correlation_id   │ VARCHAR(255) │ NOT NULL (X-Correlation-Id)   │
│ event_type       │ VARCHAR(100) │ NOT NULL                      │
│ actor_id         │ UUID         │ NULLABLE (user who acted)     │
│ actor_role       │ VARCHAR(50)  │ NULLABLE                      │
│ resource_type    │ VARCHAR(50)  │ NOT NULL (PAYMENT/USER/AUTH)  │
│ resource_id      │ UUID         │ NULLABLE                      │
│ action           │ VARCHAR(50)  │ NOT NULL (CREATE/UPDATE etc.) │
│ ip_address       │ VARCHAR(45)  │ NULLABLE                      │
│ user_agent       │ TEXT         │ NULLABLE                      │
│ request_payload  │ JSONB        │ NULLABLE (sanitized, no PII)  │
│ response_code    │ INTEGER      │ NULLABLE                      │
│ outcome          │ VARCHAR(20)  │ NOT NULL (SUCCESS/FAILURE)    │
│ created_at       │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    processed_events                              │
├──────────────────┬──────────────┬────────────────────────────── │
│ event_id         │ VARCHAR(255) │ PK                            │
│ event_type       │ VARCHAR(100) │ NOT NULL                      │
│ processed_at     │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW()        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Cross-Service Entity Relationship Overview

```
                 ┌──────────┐           ┌─────────────┐
                 │ auth_db  │           │   user_db   │
                 │auth_users│──────────▶│    users    │
                 │  roles   │ auth_user │  accounts   │
                 │r.tokens  │    _id    │ user_addrs  │
                 └──────────┘           └──────┬──────┘
                                               │ account_id (reference only, no FK)
                                               ▼
                 ┌───────────────────────────────────────────┐
                 │              payment_db                    │
                 │   payments (state machine: PENDING →       │
                 │            AUTHORIZED → CAPTURED/FAILED)  │
                 │   payment_outbox  (Transactional Outbox)  │
                 │   idempotency_keys                        │
                 └──────────────────┬────────────────────────┘
                                    │ payment_id (event ref)
                                    ▼
                    ┌───────────────────────────────┐
                    │        Kafka Event Bus        │
                    │  payment.events               │
                    │  ledger.events                │
                    │  notification.events          │
                    │  audit.events                 │
                    └───────┬───────────┬───────────┘
                            │           │
              ┌─────────────┘           └──────────────┐
              ▼                                         ▼
 ┌────────────────────────┐            ┌───────────────────────────┐
 │       ledger_db        │            │      notification_db       │
 │  journal_entries       │            │      notifications         │
 │  ledger_lines          │            │      processed_events      │
 │  processed_events      │            └───────────────────────────┘
 └────────────────────────┘
                                        ┌───────────────────────────┐
                                        │       audit_db             │
                                        │     audit_events           │
                                        │     processed_events       │
                                        └───────────────────────────┘
```

---

## 8. Key Data Integrity Rules

| Rule | Description |
| :--- | :--- |
| **Double-Entry Balance** | `SUM(DEBIT lines) == SUM(CREDIT lines)` per journal_entry enforced by service logic |
| **Idempotency TTL** | `idempotency_keys.expires_at` = `created_at + 24h`; rejected after expiry |
| **Payment State Machine** | Transitions validated in code; invalid transitions → exception |
| **Immutable Ledger** | `journal_entries` and `ledger_lines` have no UPDATE or DELETE allowed |
| **Soft Delete Users** | `users.deleted_at IS NOT NULL` → excluded from active queries |
| **Optimistic Locking** | `version` column on `accounts`, `payments`, `auth_users` |
| **Event Deduplication** | `processed_events` table in each consumer service prevents duplicate processing |
| **No PII in Audit Logs** | `request_payload` in audit_events scrubbed of passwords/card numbers |
