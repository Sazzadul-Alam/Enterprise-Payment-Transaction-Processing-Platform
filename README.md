# Enterprise Payment & Transaction Processing Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-black.svg)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326ce5.svg)](https://kubernetes.io/)

A distributed, enterprise-grade payment processing platform engineered for high-throughput, fault-tolerant financial transactions, immutable double-entry ledger bookkeeping, and automated reconciliation.

---

## Architecture Overview

```
                                  [ Client / External Consumers ]
                                                │
                                                ▼ (HTTPS / REST)
                                      ┌───────────────────┐
                                      │    API Gateway    │ (Spring Cloud Gateway)
                                      │  - Rate Limiting  │ (Redis token bucket)
                                      │  - Route Routing  │
                                      │  - Auth Forward   │
                                      └─────────┬─────────┘
                                                │
                 ┌──────────────────────────────┼──────────────────────────────┐
                 │ (Internal REST / JWT)        │ (Internal REST / JWT)        │ (Internal REST / JWT)
                 ▼                              ▼                              ▼
      ┌─────────────────────┐        ┌─────────────────────┐        ┌─────────────────────┐
      │     Auth Service    │        │     User Service    │        │   Payment Service   │
      │ - JWT / Refresh     │        │ - Profiles          │        │ - State Machine     │
      │ - Password Hashing  │        │ - Account Balances  │        │ - Idempotency Check │
      │ - Token Revocation  │        │ - KYC / User Data   │        │ - Outbox Publisher  │
      └──────────┬──────────┘        └──────────┬──────────┘        └──────────┬──────────┘
                 │                              │                              │
                 ▼                              ▼                              ▼
          [ Auth Database ]              [ User Database ]             [ Payment Database ]
                                                                               │
                                                                               ▼ (Outbox Relayed Events)
                                                                    ┌─────────────────────┐
                                                                    │    Apache Kafka     │
                                                                    │ (Event Backbone)    │
                                                                    └──────────┬──────────┘
                                                                               │
                                   ┌───────────────────────────────────────────┼───────────────────────────────────────────┐
                                   │                                           │                                           │
                                   ▼ (Kafka Consumer)                          ▼ (Kafka Consumer)                          ▼ (Kafka Consumer)
                        ┌─────────────────────┐                     ┌─────────────────────┐                     ┌─────────────────────┐
                        │   Ledger Service    │                     │   Audit Service     │                     │ Notification Service│
                        │ - Double-Entry Book │                     │ - Immutable Logs    │                     │ - Webhooks / Email  │
                        │ - Journal & Entries │                     │ - Security Tracing  │                     │ - Retry & Deadletter│
                        └──────────┬──────────┘                     └──────────┬──────────┘                     └──────────┬──────────┘
                                   │                                           │                                           │
                                   ▼                                           ▼                                           ▼
                          [ Ledger Database ]                         [ Audit Database ]                          [ Notification DB ]

                                 ▲                                           ▲
                                 │                                           │
                                 └─────────────────────┬─────────────────────┘
                                                       │
                                            ┌─────────────────────┐
                                            │ Reconciliation /    │ (Spring Batch)
                                            │   Batch Service     │ - Scheduled EOD Reconciliation
                                            │                     │ - Balance Discrepancy Detection
                                            └─────────────────────┘
```

---

## Key Modules & Domain Breakdown

| Module | Description | Port (Local Default) |
| :--- | :--- | :--- |
| `api-gateway` | Spring Cloud Gateway, Redis Rate Limiting, Route Routing | `8080` |
| `auth-service` | User Authentication, JWT issuance, Refresh Token rotation, Token Blacklist | `8081` |
| `user-service` | User profile management, KYC, Account Balances | `8082` |
| `payment-service`| Payment State Machine, Idempotency enforcement, Transactional Outbox | `8083` |
| `ledger-service` | Double-entry immutable accounting, Journal Entries, Ledger Balances | `8084` |
| `notification-service` | Multi-channel notifications (Email/Webhook) with DLQ & Retry | `8085` |
| `audit-service` | Tamper-evident audit trail & compliance logging | `8086` |
| `reconciliation-service` | Spring Batch EOD payment vs ledger reconciliation | `8087` |

---

## Technical Stack & Architectural Patterns

- **Core Framework**: Java 21 LTS, Spring Boot 3.3.x, Spring Security 6.x
- **Data & Persistence**: PostgreSQL 16 (Database-per-service pattern), Spring Data JPA, Liquibase / Flyway
- **Caching & Locks**: Redis 7.x (Multi-level cache, Distributed locks, Token bucket rate limiter)
- **Messaging & Events**: Apache Kafka 3.7 (Event-driven architecture, DLQ, consumer idempotency)
- **Reliability & Consistency**: Transactional Outbox Pattern, Saga Orchestration / Compensation, Double-Entry Bookkeeping
- **Resilience**: Resilience4j (Circuit Breaker, Rate Limiter, Retry, Bulkhead, TimeLimiter)
- **Batch Processing**: Spring Batch 5.x with Quartz / Spring Scheduler
- **Observability**: Spring Boot Actuator, Micrometer, Prometheus, Grafana, OpenTelemetry, Structured JSON logging
- **Testing**: JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL, Kafka, Redis)
- **DevOps**: Docker Multi-stage builds, Docker Compose, Kubernetes Manifests, Helm Charts, GitHub Actions CI/CD

---

## Documentation Structure

Detailed documentation is organized in the [`docs/`](./docs) directory:
- [Architecture & Domain Model](docs/ARCHITECTURE.md): System topology, service boundaries, NFRs, and acceptance criteria.
- [Branching & Git Strategy](docs/BRANCHING_STRATEGY.md): Git branching model, commit conventions, and code review standards.
- `docs/api/`: OpenAPI / Swagger specifications per service.
- `docs/database/`: ERD diagrams, schema migration standards, and isolation rules.
- `docs/runbooks/`: Operational runbooks, deployment guides, and troubleshooting steps.

---

## Getting Started

### Prerequisites
- JDK 21+
- Apache Maven 3.9+
- Docker & Docker Compose v2+
- Git 2.40+

### Development Workflow
Refer to [Branching Strategy](docs/BRANCHING_STRATEGY.md) before contributing code. All commits follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.
