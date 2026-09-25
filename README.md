# Enterprise AI-CRM Platform

A production-grade, event-driven Enterprise SaaS Customer Relationship Management (CRM) platform built with **Java 21 LTS**, **Spring Boot 3.3.3**, **MySQL 8.4**, **Redis Streams**, and **Google Gemini AI**.

---

## 1. System Architecture

The platform is designed following the **Event-Driven 3-Tier Enterprise SaaS Architecture** implemented as a modular monolith:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ Presentation & Security Layer (Spring Boot Web, Spring Security 6.x, SpringDoc OpenAPI)  │
│ - Stateless JWT Bearer Authentication (HS256, 1-hour access token, DB authority check)  │
│ - Strict REST Endpoints (/api/v1/) with Uniform Success/Error Response Envelopes        │
│ - Global Exception Handling with Semantic Error Codes (400, 401, 403, 404, 409, 422, 503)│
│ - Correlation Tracing via RequestIdFilter (X-Request-Id header & SLF4J MDC)             │
└───────────────────────────────────────────┬─────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼─────────────────────────────────────────────┐
│ Domain & Business Service Layer                                                         │
│ - User & RBAC Management (ROLE_ADMIN, ROLE_MARKETER)                                    │
│ - Customer Lifecycle Management (Active/Soft-Delete semantics via deleted_at IS NULL)   │
│ - Bulk Ingestion Service (Streaming OpenCSV & Apache POI SAX, Batch Size 200)           │
│ - Dynamic Boolean AST Segmentation Engine (Recursive Criteria API Compiler)             │
│ - Campaign Execution Service (Pessimistic Row Lock SELECT FOR UPDATE, Launch Invariants)│
│ - Redis Stream Delivery Producer & Consumer Worker Pool (Idempotent Conditional UPDATE) │
│ - Google Gemini AI Service (Natural Language -> Validated JSON AST, Audit Compliance)   │
│ - Real-time Aggregation & Reporting Engine (Metrics, Overviews, History)                │
└───────────────────────────────────────────┬─────────────────────────────────────────────┘
                                            │
                      ┌─────────────────────┴─────────────────────┐
                      │                                           │
┌─────────────────────▼─────────────────────┐       ┌─────────────▼───────────────────────┐
│ Authoritative Relational Persistence      │       │ Transient Async Messaging Transport │
│ MySQL 8.4 (InnoDB, utf8mb4)               │       │ Redis Streams                       │
│ - 9 Canonical Tables (inc. Outbox)         │       │ - crm:campaign:deliveries:stream    │
│ - READ COMMITTED Transaction Isolation    │       │ - Consumer Group: crm:delivery:workers│
│ - Transactional Outbox Pattern            │       │ - Safe Stream Min-ID Trimming       │
│ - Row-level Locking & Foreign Key Integrity│      │ - At-least-once with manual XACK    │
└───────────────────────────────────────────┘       └─────────────────────────────────────┘
```

---

## 2. Technology Stack

- **Runtime & Language:** Java 21 LTS, OpenJDK
- **Framework:** Spring Boot 3.3.3, Spring Security 6.x, Spring Data JPA / Hibernate 6.5
- **Primary Database:** MySQL 8.4 (Sole persistent source of truth)
- **Async Messaging & Queuing:** Redis Streams (Lettuce client, transient transport only, with Transactional Outbox)
- **File Parsing:** OpenCSV 5.9 (CSV streaming), Apache POI 5.3.0 SAX (XLSX streaming)
- **Generative AI:** Google Gemini 1.5 Flash (via Spring AI / REST client abstraction with bounded timeouts)
- **API Documentation:** SpringDoc OpenAPI 2.6.0 / Swagger UI
- **Observability:** Spring Boot Actuator, SLF4J + Logback with MDC correlation IDs (propagated asynchronously)
- **Testing:** JUnit 5, Mockito, Spring Security Test, Awaitility, Maven Surefire
- **Containerization:** Docker (Multi-stage build, non-root user), Docker Compose

---

## 3. Environment Variables & Configuration

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/crm_db?...` | JDBC URL for MySQL 8.x database |
| `SPRING_DATASOURCE_USERNAME` | `root` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | *(empty / local)* | Database password |
| `REDIS_HOST` | `localhost` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |
| `JWT_SECRET` | *(required in prod)* | HMAC-SHA256 secret key (minimum 32 UTF-8 bytes) |
| `BOOTSTRAP_ADMIN_USERNAME` | `admin` | Initial admin username (created only when `users` table is empty) |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@crm.internal` | Initial admin email |
| `BOOTSTRAP_ADMIN_PASSWORD` | `Admin123!` | Initial admin password |
| `GEMINI_API_KEY` | *(optional)* | Google Gemini API key (defaults to deterministic translator if omitted) |
| `CRM_DELIVERY_PROVIDER` | `simulated` | Delivery provider implementation (`simulated` or `smtp`) |
| `SMTP_HOST` | `localhost` | SMTP server host (e.g. `smtp` in docker-compose, or external SMTP host) |
| `SMTP_PORT` | `1025` | SMTP server port (1025 for MailHog, 587 for TLS, 465 for SSL) |
| `SMTP_USERNAME` | *(optional)* | SMTP authentication username (not logged or exposed) |
| `SMTP_PASSWORD` | *(optional)* | SMTP authentication password (not logged or exposed) |
| `SMTP_FROM` | `noreply@crm.internal` | Standard RFC 5322 From address for outbound campaign emails |
| `SMTP_TIMEOUT_MS` | `5000` | SMTP connection, socket, and read timeout in milliseconds |
| `CRM_STREAM_KEY` | `crm:campaign:deliveries:stream` | Redis Stream key for campaign dispatch |
| `CRM_CONSUMER_GROUP` | `crm:delivery:workers` | Redis Stream consumer group name |
| `CRM_REDIS_MAX_STREAM_LENGTH` | `10000` | Redis Stream approximate trimming threshold (prevent unbounded growth) |
| `CRM_REDIS_OUTBOX_POLL_INTERVAL_MS` | `5000` | Background outbox poller interval for unpublished events |
| `CRM_REDIS_PEL_RECOVERY_INTERVAL_MS` | `10000` | Redis Pending Entries List (PEL) stale message recovery interval |
| `CRM_AI_CONNECT_TIMEOUT_MS` | `3000` | Gemini API connection timeout in milliseconds |
| `CRM_AI_READ_TIMEOUT_MS` | `7000` | Gemini API read timeout in milliseconds |

---

## 4. Building and Running

### 4.1 Prerequisites
- Java 21 LTS installed and on `PATH`
- Maven 3.9+ installed and on `PATH`
- MySQL 8.4 instance running on port 3306 with database `crm_db`
- Redis server running on port 6379
- Optional: Local SMTP server (e.g. MailHog on port 1025) for local SMTP delivery testing

### 4.2 Database Initialization
The database schema is defined in `src/main/resources/schema.sql` containing all 9 canonical tables:
```bash
mysql -u root -p crm_db < src/main/resources/schema.sql
```

### 4.3 Running Tests
Run the complete automated test suite (399 tests across M0–M12, production hardening, and SMTP delivery):
```bash
mvn clean test
```

### 4.4 Packaging the Application
Build the production-ready runnable JAR:
```bash
mvn clean package
```
Artifact generated: `target/crm-platform-0.0.1-SNAPSHOT.jar`

### 4.5 Running Locally
```bash
java -jar target/crm-platform-0.0.1-SNAPSHOT.jar
# Or via Maven plugin:
mvn spring-boot:run
```

---

## 5. Delivery Provider & Docker Compose Architecture

### 5.1 Configurable Delivery Architecture
The platform decouples campaign asynchronous dispatch from message delivery transport through the `DeliveryProvider` interface:

```
Campaign Launch
      ↓
Transactional Outbox (MySQL 8.4)
      ↓
Redis Stream (`crm:campaign:deliveries:stream`)
      ↓
DeliveryStreamConsumer (Worker Pool)
      ↓
DeliveryProvider (Abstraction)
  ├── SimulatedDeliveryProvider (`crm.delivery.provider=simulated`)
  │     └── In-memory deterministic 90% SENT / 10% FAILED simulation for tests & demos
  └── SmtpDeliveryProvider (`crm.delivery.provider=smtp`)
        └── Actual RFC 821/5322 SMTP delivery over TCP with:
              - Stable delivery idempotency key tracking (`X-Delivery-Idempotency-Key`)
              - Safe error propagation and timeout handling (`smtp.timeout-ms`)
              - Zero credential logging
              - Re-delivery safety (cached idempotency results prevent duplicate sends on retry)
```

#### Delivery Provider Switching
Switch delivery providers via configuration or environment variable:
- **Simulated Provider:** Set `CRM_DELIVERY_PROVIDER=simulated` (default)
- **Local / Production SMTP:** Set `CRM_DELIVERY_PROVIDER=smtp` along with `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, and credentials if required.
- **Strict Validation:** Blank or invalid provider values fail immediately at startup with an informative `IllegalStateException` preventing silent fallback.

#### Delivery Environments:
1. **Simulated Delivery:** Deterministic in-memory simulation, useful for unit tests and local demonstrations without external services.
2. **Local SMTP Delivery:** Development and Docker runtime testing using a local SMTP sink (`mailhog` on port 1025 in `docker-compose.yml`, or embedded `GreenMail` in integration tests).
3. **External Production SMTP:** Production delivery via enterprise SMTP relay or transactional email provider (e.g. Amazon SES, SendGrid, Mailgun SMTP) configured with TLS/SSL and credentials via environment variables.

### 5.2 Docker & Docker Compose Deployment

#### Prerequisites
- **Docker Desktop** (or Docker Engine with Docker Compose v2+)
- **WSL2** (`wsl.exe --install` on Windows 10/11) with Virtual Machine Platform enabled
- Hardware virtualization (Intel VT-x / AMD-V) enabled in system BIOS/UEFI

#### Orchestrated Stack Architecture
The local stack defined in `docker-compose.yml` orchestrates 4 interconnected services on the isolated `crm-network` bridge:
- **`app`:** Spring Boot 3.3.3 container (Eclipse Temurin Java 21 JRE Alpine, dedicated non-root user `crmapp:crmgroup`, port `8080:8080`)
- **`mysql`:** MySQL 8.4 official container (InnoDB, auto-initialized from `src/main/resources/schema.sql`, internal Docker network only to eliminate host port 3306 conflicts, healthcheck via `mysqladmin ping`)
- **`redis`:** Redis 7 Alpine container (AOF persistence enabled, internal Docker network only to eliminate host port 6379 conflicts, healthcheck via `redis-cli ping`)
- **`smtp`:** MailHog v1.0.1 development SMTP test sink (listening on internal port 1025; Web UI bound strictly to `127.0.0.1:8025` for localhost email inspection)

#### Lifecycle Commands
```bash
# 1. Validate Compose specification and variable interpolation
docker compose config

# 2. Build image and launch complete stack in detached mode
docker compose up --build -d

# 3. Check health and status of all services
docker compose ps

# 4. Stream application container logs
docker compose logs -f app

# 5. Inspect individual service logs
docker compose logs --tail=100 mysql
docker compose logs --tail=100 redis
docker compose logs --tail=100 smtp

# 6. Graceful shutdown and volume teardown
docker compose down
# Or tear down with persistent volumes removed:
docker compose down -v
```

> **Note on Windows Host Virtualization:**
> Running Linux container engines (`dockerd`/WSL2) on Windows requires Windows Subsystem for Linux (WSL2) and the `VirtualMachinePlatform` feature. If not yet initialized on the host machine, run `wsl.exe --install` from an elevated Administrator prompt (or run the downloaded Docker Desktop installer) to activate the WSL2 Linux kernel.

---

## 6. Swagger / OpenAPI Documentation

Once the application is running:
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON Spec:** `http://localhost:8080/v3/api-docs`

To authenticate in Swagger UI:
1. Send `POST /api/v1/auth/login` with your credentials (`username` and `password`).
2. Copy the `token` from the response.
3. Click the **Authorize** button in Swagger UI, enter the token, and click **Authorize**.

---

## 7. Canonical API Endpoints Reference

### 7.1 Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/login` — Authenticate user and receive JWT access token.
- `GET /api/v1/auth/me` — Retrieve authenticated user profile and permissions.

### 7.2 User Management (`/api/v1/users`) — *ROLE_ADMIN only*
- `POST /api/v1/users` — Create new administrative or marketer user.
- `GET /api/v1/users` — Paginated user listing.
- `GET /api/v1/users/{id}` — Get user by ID.
- `PATCH /api/v1/users/{id}` — Update user details or active status.
- `POST /api/v1/users/{id}/password` — Admin password reset.

### 7.3 Customer Management (`/api/v1/customers`)
- `POST /api/v1/customers` — Create individual customer (409 Conflict distinguishing active vs. soft-deleted duplicates).
- `GET /api/v1/customers` — Paginated list of active customers (`deleted_at IS NULL`).
- `GET /api/v1/customers/{id}` — Get customer by ID.
- `PUT /api/v1/customers/{id}` — Full customer update.
- `DELETE /api/v1/customers/{id}` — Soft delete customer (*ROLE_ADMIN only*).
- `GET /api/v1/customers/search` — Search customers by name, email, or city.

### 7.4 Bulk Ingestion (`/api/v1/uploads`)
- `POST /api/v1/uploads/bulk` — Multipart upload of CSV or XLSX files (streaming, batch size 200, isolated `REQUIRES_NEW` transactions via `UploadBatchPersister`, partial success, deterministic soft-delete duplicate handling).
- `GET /api/v1/uploads/history` — Paginated audit history of customer file imports.

### 7.5 Segmentation (`/api/v1/segments`)
- `POST /api/v1/segments` — Create dynamic segment with Boolean AST rule tree.
- `GET /api/v1/segments` — Paginated list of segments.
- `GET /api/v1/segments/{id}` — Get segment definition by ID.
- `PATCH /api/v1/segments/{id}` — Update segment rules or details.
- `DELETE /api/v1/segments/{id}` — Delete segment (blocked if bound to campaigns).
- `GET /api/v1/segments/{id}/preview` — Fast preview of matching active audience count.
- `GET /api/v1/segments/{id}/members` — Paginated list of customers currently matching segment AST.

### 7.6 Campaigns (`/api/v1/campaigns`)
- `POST /api/v1/campaigns` — Create campaign in `DRAFT` status with message template.
- `GET /api/v1/campaigns` — Paginated campaign listing (filterable by status).
- `GET /api/v1/campaigns/{id}` — Get campaign by ID.
- `PATCH /api/v1/campaigns/{id}` — Update campaign copy/settings (`DRAFT` only).
- `DELETE /api/v1/campaigns/{id}` — Delete campaign (`DRAFT` only, *ROLE_ADMIN only*).
- `POST /api/v1/campaigns/{id}/launch` — Atomically evaluate segment, lock row, transition `DRAFT` $\to$ `RUNNING`, create `PENDING` obligations, stage events in `campaign_delivery_outbox`, and asynchronously dispatch to Redis Streams with keyset pagination audience batching (`WHERE id > :lastSeenId ORDER BY id ASC LIMIT 500`). Rejects zero-audience with 400 Bad Request.

### 7.7 Campaign Delivery Tracking (`/api/v1/campaigns/{id}`)
- `GET /api/v1/campaigns/{id}/delivery-summary` — Aggregated delivery counts (`pendingCount`, `sentCount`, `failedCount`, `completionPercentage`, `isTerminal`).
- `GET /api/v1/campaigns/{id}/deliveries` — Paginated recipient delivery ledger (`status`, `processedAt`, `failureReason`).

### 7.8 Generative AI (`/api/v1/ai`)
- `POST /api/v1/ai/segments/generate-rules` — Translate natural-language prompt into validated Boolean AST rule tree (with `isFallback` indicator when deterministic fallback is used).
- `GET /api/v1/ai/segments/audits` — Compliance audit history of all AI prompts and generated ASTs (*ROLE_ADMIN only*).

### 7.9 Analytics & Reporting (`/api/v1/reports`)
- `GET /api/v1/reports/campaigns/{id}` — Delivery rate, timeline, and duration for campaign.
- `GET /api/v1/reports/customers/overview` — High-level customer demographic KPIs, total spend, average spend, and top locations.
- `GET /api/v1/reports/campaigns/{id}/ai-summary` — AI narrative summary of campaign outcomes generated by Gemini (returns 503 Service Unavailable when Gemini is unconfigured or unavailable; strictly no fabricated summaries).
- `GET /api/v1/reports/campaigns/history` — Paginated historical campaign performance summaries.

---

## 8. Verification & Production Quality Assurance

All 399 test cases pass with 0 failures, 0 errors, and 0 skipped tests:
- **Baseline Modules (M0–M6):** 290/290 passing
- **M7 Bulk Ingestion:** CSV streaming, XLSX SAX streaming, duplicate detection (active vs. soft-deleted rejection), partial success, isolated `UploadBatchPersister` rollback and fallback persistence
- **M8 Redis Streams & Asynchronous Architecture:** Transactional Outbox pattern, worker pool consumption, real PEL stale message ownership reclaim via `XCLAIM`, at-least-once delivery, manual `XACK`, safe MINID stream trimming protecting in-flight work
- **M9 Campaign Execution & Concurrency:** Pessimistic locking (`SELECT FOR UPDATE`), zero-audience invariant, state machine transitions (`DRAFT` $\to$ `RUNNING` $\to$ `COMPLETED`), duplicate delivery prevention, keyset pagination large audience materialization
- **M10 Generative AI & Auditing:** Prompt translation, AST validation gates, fallback transparency (`isFallback`), strict 503 on unconfigured summaries with zero fabricated text, immutable compliance logs in `ai_segment_audits`
- **M11 Observability & Reporting:** `X-Request-Id` correlation filter, SLF4J MDC async propagation across Redis Stream dispatch and consumption with guaranteed cleanup in `finally`, Actuator health endpoints, cross-domain performance analytics
- **M12 Deployment & Documentation:** OpenAPI 3.0 specification, multi-stage Dockerfile, Docker Compose stack
- **Delivery Hardening & SMTP Integration:** Configurable `DeliveryProvider` selection (`simulated` vs `smtp`), RFC 821/5322 real SMTP delivery via `SmtpDeliveryProvider`, delivery idempotency key tracking (`X-Delivery-Idempotency-Key`), retry send deduplication, local SMTP testing with GreenMail and MailHog, zero-leak credential logging, end-to-end integration tests (Outbox $\to$ Redis $\to$ Worker $\to$ SMTP $\to$ DB)

