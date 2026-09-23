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
│ - 8 Canonical Tables                      │       │ - crm:campaign:deliveries:stream    │
│ - READ COMMITTED Transaction Isolation    │       │ - Consumer Group: crm:delivery:workers│
│ - Row-level Locking & Foreign Key Integrity│      │ - At-least-once with manual XACK    │
└───────────────────────────────────────────┘       └─────────────────────────────────────┘
```

---

## 2. Technology Stack

- **Runtime & Language:** Java 21 LTS, OpenJDK
- **Framework:** Spring Boot 3.3.3, Spring Security 6.x, Spring Data JPA / Hibernate 6.5
- **Primary Database:** MySQL 8.4 (Sole persistent source of truth)
- **Async Messaging & Queuing:** Redis Streams (Lettuce client, transient transport only)
- **File Parsing:** OpenCSV 5.9 (CSV streaming), Apache POI 5.3.0 SAX (XLSX streaming)
- **Generative AI:** Google Gemini 1.5 Flash (via Spring AI / REST client abstraction)
- **API Documentation:** SpringDoc OpenAPI 2.6.0 / Swagger UI
- **Observability:** Spring Boot Actuator, SLF4J + Logback with MDC correlation IDs
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
| `CRM_STREAM_KEY` | `crm:campaign:deliveries:stream` | Redis Stream key for campaign dispatch |
| `CRM_CONSUMER_GROUP` | `crm:delivery:workers` | Redis Stream consumer group name |

---

## 4. Building and Running

### 4.1 Prerequisites
- Java 21 LTS installed and on `PATH`
- Maven 3.9+ installed and on `PATH`
- MySQL 8.4 instance running on port 3306 with database `crm_db`
- Redis server running on port 6379

### 4.2 Database Initialization
The database schema is defined in `src/main/resources/schema.sql` containing all 8 canonical tables:
```bash
mysql -u root -p crm_db < src/main/resources/schema.sql
```

### 4.3 Running Tests
Run the complete automated test suite (340 tests across M0–M12):
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

## 5. Docker & Docker Compose Deployment

The platform includes a production-grade multi-stage Dockerfile and a complete local orchestration stack via `docker-compose.yml`:

```bash
# Start MySQL 8.4, Redis 7, and the Application
docker compose up --build -d

# View logs
docker compose logs -f app

# Tear down
docker compose down -v
```

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
- `POST /api/v1/customers` — Create individual customer.
- `GET /api/v1/customers` — Paginated list of active customers (`deleted_at IS NULL`).
- `GET /api/v1/customers/{id}` — Get customer by ID.
- `PUT /api/v1/customers/{id}` — Full customer update.
- `DELETE /api/v1/customers/{id}` — Soft delete customer (*ROLE_ADMIN only*).
- `GET /api/v1/customers/search` — Search customers by name, email, or city.

### 7.4 Bulk Ingestion (`/api/v1/uploads`)
- `POST /api/v1/uploads/bulk` — Multipart upload of CSV or XLSX files (streaming, batch size 200, partial success).
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
- `POST /api/v1/campaigns/{id}/launch` — Atomically evaluate segment, lock row, transition `DRAFT` $\to$ `RUNNING`, create `PENDING` obligations, and enqueue to Redis Streams. Rejects zero-audience with 400 Bad Request.

### 7.7 Campaign Delivery Tracking (`/api/v1/campaigns/{id}`)
- `GET /api/v1/campaigns/{id}/delivery-summary` — Aggregated delivery counts (`pendingCount`, `sentCount`, `failedCount`, `completionPercentage`, `isTerminal`).
- `GET /api/v1/campaigns/{id}/deliveries` — Paginated recipient delivery ledger (`status`, `processedAt`, `failureReason`).

### 7.8 Generative AI (`/api/v1/ai`)
- `POST /api/v1/ai/segments/generate-rules` — Translate natural-language prompt into validated Boolean AST rule tree.
- `GET /api/v1/ai/segments/audits` — Compliance audit history of all AI prompts and generated ASTs (*ROLE_ADMIN only*).

### 7.9 Analytics & Reporting (`/api/v1/reports`)
- `GET /api/v1/reports/campaigns/{id}` — Delivery rate, timeline, and duration for campaign.
- `GET /api/v1/reports/customers/overview` — High-level customer demographic KPIs, total spend, average spend, and top locations.
- `GET /api/v1/reports/campaigns/{id}/ai-summary` — AI narrative summary of campaign outcomes generated by Gemini.
- `GET /api/v1/reports/campaigns/history` — Paginated historical campaign performance summaries.

---

## 8. Verification & Quality Assurance

All 340 test cases pass with 0 failures, 0 errors, and 0 skipped tests:
- **Baseline Modules (M0–M6):** 290/290 passing
- **M7 Bulk Ingestion:** CSV streaming, XLSX SAX streaming, duplicate detection, partial success, error serialization
- **M8 Redis Streams & Asynchronous Architecture:** In-flight queueing, worker pool consumption, PEL management, at-least-once delivery, manual `XACK`
- **M9 Campaign Execution & Concurrency:** Pessimistic locking (`SELECT FOR UPDATE`), zero-audience invariant, state machine transitions (`DRAFT` $\to$ `RUNNING` $\to$ `COMPLETED`), duplicate delivery prevention
- **M10 Generative AI & Auditing:** Prompt translation, AST validation gates, immutable compliance logs in `ai_segment_audits`, graceful failure isolation (503 Service Unavailable)
- **M11 Observability & Reporting:** `X-Request-Id` correlation filter, SLF4J MDC, Actuator health endpoints, cross-domain performance analytics
- **M12 Deployment & Documentation:** OpenAPI 3.0 specification, multi-stage Dockerfile, Docker Compose stack
