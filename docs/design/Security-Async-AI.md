# Security, Asynchronous Delivery, and AI Architecture Specification
## Enterprise AI-CRM Platform (`CS-CRM-2026`)

---

## 1. Document Control

### 1.1 Document Metadata
- **Project Code:** CS-CRM-2026
- **System Name:** Enterprise AI-CRM Platform
- **Document Title:** Security, Asynchronous Delivery, and AI Architecture Specification
- **Document Path:** `docs/design/Security-Async-AI.md`
- **Document Version:** 1.0.0
- **SDLC Phase:** Phase 2 — System Design
- **Document Status:** **APPROVED BASELINE**
- **Date:** 2026-09-19
- **Author:** System Architecture, Enterprise Security, & Async Infrastructure Engineering Team
- **Target Audience:** Enterprise Architects, Backend Security Engineers, Infrastructure & Data Engineers, Academic Evaluators

### 1.2 Baseline Authority Chain
This specification directly derives from and is strictly subordinate to the approved project baselines:
$$\text{SRS v1.0.1} \longrightarrow \text{System Architecture v1.0.0} \longrightarrow \text{Database Design v1.0.0} \longrightarrow \text{API Design v1.0.0} \longrightarrow \text{Security + Async + AI Design v1.0.0}$$

---

## 2. Purpose and Scope

### 2.1 Purpose
This document provides the formal, implementation-ready architectural specification for the three cross-cutting technical pillars of the **Enterprise AI-CRM Platform (`CS-CRM-2026`)**:
1. **Application Security & Identity Governance:** Spring Security 6.x stateless filter chains, JWT bearer authentication, credential protection, and two-role Role-Based Access Control (`ROLE_ADMIN` and `ROLE_MARKETER`).
2. **Asynchronous Delivery Engine:** Transient Redis Streams message transport, worker consumer-group mechanics, delivery idempotency boundaries, failure containment, and concurrency-safe MySQL campaign state transitions.
3. **AI Trust Perimeter & Integration:** Google Gemini integration via Spring AI, untrusted payload containment, schema-first Abstract Syntax Tree (AST) validation, dynamic Criteria API compilation, customer data minimization, and resilient narrative reporting.

This document bridges high-level requirements and implementation, defining the concrete structural contracts, component boundaries, and operational invariants necessary for subsequent development without ambiguity.

### 2.2 Scope Boundaries
- **In-Scope:**
  - Stateless authentication flows, cryptographic token handling concepts, and RBAC endpoint mapping.
  - Asynchronous delivery dispatch, Redis Stream consumer lifecycles, and at-least-once delivery semantics.
  - State machine invariants for campaign execution (`DRAFT` $\to$ `RUNNING` $\to$ `COMPLETED` / `FAILED`).
  - Untrusted LLM integration boundaries, JSON AST compilation to type-safe JPA Criteria queries, and prompt/response auditing.
  - Structured operational logging (SLF4J/MDC) and security threat mitigations.
- **Strictly Out-of-Scope:**
  - Java source code, `@RestController`, `@Service`, Spring `@Configuration` classes, DTOs, and entity implementations.
  - SQL DDL scripts, Liquibase/Flyway migrations, and database schema mutations.
  - Excluded technologies: PostgreSQL, Apache Kafka, RabbitMQ, Resilience4j, Redis Cache-Aside/application caching, generic `audit_logs` table, microservices, and distributed two-phase commit (2PC) transactions.

---

## 3. Baseline References

This specification strictly conforms to the following approved project artifacts:

1. **[docs/SRS.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/SRS.md) (Version 1.0.1 — Approved Baseline):**
   - Functional requirements: Security (`FR-SEC-001`, `FR-SEC-002`), Campaign Management (`FR-CAMP-*`), Asynchronous Delivery (`FR-DEL-*`), AI Segmentation & Personalization (`FR-AI-*`), and Operational Reporting (`FR-REPORT-*`).
   - Non-functional requirements: Performance (`NFR-PERF-*`), Scalability (`NFR-SCALE-*`), Security (`NFR-SEC-001`, `NFR-SEC-002`), Reliability (`NFR-REL-*`), Maintainability (`NFR-MAINT-*`), and Data Integrity (`NFR-DATA-*`).
2. **[docs/design/System-Architecture.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/System-Architecture.md) (Version 1.0.0 — Approved Baseline):**
   - Modular monolith pattern, strict DTO/Entity isolation, stateless Spring Security architecture, Redis Streams transient async transport, Criteria API dynamic compilation, and isolated Spring AI boundaries.
3. **[docs/design/Database-Design.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/Database-Design.md) (Version 1.0.0 — Approved Baseline):**
   - 8 canonical relational tables (`users`, `customers`, `customer_tags`, `segments`, `campaigns`, `campaign_delivery_records`, `upload_history`, `ai_segment_audits`).
   - `READ COMMITTED` transaction isolation (`DBD-18`), soft-delete mechanics (`deleted_at`), composite deduplication index `UNIQUE(campaign_id, customer_id)`, and `campaigns.ai_summary` persistence.
4. **[docs/design/API-Design.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/API-Design.md) (Version 1.0.0 — Approved Baseline):**
   - 36 REST endpoints across 8 canonical domains, `/api/v1/` base path, uniform response envelopes, semantic status codes, zero-audience launch rejection invariant, and SpringDoc OpenAPI 2.x documentation standard.
5. **Security + Async + AI Design — Step 1 (Approved Baseline Analysis):**
   - Pre-design review establishing failure windows, worker idempotency layers, AST schema validation pipelines, and open decision governance.

---

## 4. Architectural Governance

### 4.1 Decision Classification Taxonomy
Every design statement, policy, and configuration parameter in this document is governed by a strict five-tier classification model:
- **`[DECIDED]`:** Firmly established, approved by higher-level baselines, and mandatory for implementation.
- **`[RECOMMENDED]`:** Architecturally evaluated and proposed as the preferred pattern, subject to implementation review.
- **`[OPEN DESIGN DECISION]`:** Intentionally unresolved architectural choice requiring evaluation or consumer consensus before code freeze.
- **`[DEFERRED]`:** Explicitly postponed to a later SDLC design or configuration phase.
- **`[REQUIRES TESTING]`:** Numerical threshold or sizing parameter that cannot be soundly frozen without empirical benchmarking.

*Governance Invariant:* Examples, trade-off evaluations, and candidate alternatives must never be converted into decisions. Upstream open and deferred decisions must be preserved without silent closure.

---

## 5. Security Architecture Overview

### 5.1 Defense-in-Depth Model
Security is enforced across five discrete, non-overlapping architectural layers:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ 1. Network & Transport Perimeter (Infrastructure Layer)                                │
│ - HTTPS/TLS mandatory in deployed environments (NFR-SEC-002; exact version open)       │
│ - Ingress request filtering and network boundary isolation                             │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│ 2. API & Filter Chain Perimeter (Spring Security 6.x)                                  │
│ - Stateless JWT Bearer Token validation (signature, expiration, claims)                │
│ - Immediate rejection of unauthenticated requests on protected endpoints (401)         │
│ - High-level URL path authorization matching (permitAll vs authenticated)              │
│ - Request format and Jakarta Bean Validation (@NotNull, @Size, @Email)                 │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│ 3. Service & Business Domain Boundary (Core Application)                               │
│ - Fine-grained method security via SpEL (@PreAuthorize("hasRole('ADMIN')"))           │
│ - Account status verification (users.is_active check)                                  │
│ - Dynamic segment AST structural & semantic schema validation                          │
│ - Domain state-machine invariant checks (e.g., campaign must be DRAFT to launch)       │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                      ┌─────────────────────┴─────────────────────┐
                      │                                           │
┌─────────────────────▼─────────────────────┐       ┌─────────────▼──────────────────────┐
│ 4. Persistence Boundary (MySQL 8.x)       │       │ 5. External AI Boundary (Gemini)   │
│ - Parameterized queries via JPA Criteria  │       │ - Untrusted input containment      │
│ - READ COMMITTED transaction isolation    │       │ - Zero direct SQL execution        │
│ - Row-level locking (SELECT FOR UPDATE)   │       │ - Strict data minimization         │
│ - Foreign key & unique constraints        │       │ - Ephemeral prompt/response flow   │
│ - Exclusion of soft-deleted customers     │       │ - Full audit trail in MySQL        │
└───────────────────────────────────────────┘       └────────────────────────────────────┘
```

---

## 6. Authentication Architecture

### 6.1 Authentication Protocol Flow
Authentication is strictly stateless and driven by JSON Web Tokens (`NFR-SEC-001`):

```
Client Application                    Spring Security Filter Chain           AuthenticationManager / UserDetailsService          MySQL (users table)
      │                                            │                                              │                                       │
      ├─ POST /api/v1/auth/login ─────────────────>│                                              │                                       │
      │  {username, password}                      ├─ AuthenticationFilter ──────────────────────>│                                       │
      │                                            │                                              ├─ SELECT by username ─────────────────>│
      │                                            │                                              │<─ UserRecord (hash, role, is_active) ─┤
      │                                            │                                              ├─ Verify is_active == true             │
      │                                            │                                              ├─ PasswordEncoder.matches(raw, hash)   │
      │                                            │<─ AuthenticationResult (Principal, Roles) ───┤                                       │
      │                                            ├─ TokenProvider.generateToken(Principal)      │                                       │
      │<─ 200 OK {token, tokenType, user} ─────────┤                                              │                                       │
      │                                            │                                              │                                       │
      │  Subsequent Request:                       │                                              │                                       │
      ├─ GET /api/v1/customers ───────────────────>│                                              │                                       │
      │  Authorization: Bearer <token>             ├─ JwtAuthenticationFilter                     │                                       │
      │                                            │  - Verify Signature                          │                                       │
      │                                            │  - Validate Expiration (exp)                 │                                       │
      │                                            │  - Extract Principal & GrantedAuthorities    │                                       │
      │                                            │  - Populate SecurityContextHolder            │                                       │
      │                                            ├─ AuthorizationFilter (verify ROLE_*)         │                                       │
      │                                            ├─ Dispatch to Controller                      │                                       │
```

### 6.2 Deactivated Account Invariant
When an administrator deactivates a user account (`PATCH /api/v1/users/{id}/deactivate`), `users.is_active` is set to `false`. Subsequent login attempts for deactivated credentials **MUST BE REJECTED** with `401 Unauthorized`.

---

## 7. JWT Architecture and Lifecycle

### 7.1 Token Structure and Claims
Tokens issued by the platform contain standard registered claims and explicit application context:
- **Header:** Algorithm identifier (`alg`) and Type (`typ: JWT`).
- **Registered Claims:**
  - `sub` (Subject): Canonical `username` (email) of the user.
  - `iat` (Issued At): UTC timestamp of issuance.
  - `exp` (Expiration): UTC timestamp after which the token is invalid.
  - `jti` (JWT ID): Unique UUID assigned to the token.
- **Private Domain Claims:**
  - `uid` (User ID): Internal `BIGINT UNSIGNED` primary key from `users.id`.
  - `role`: Canonical granted authority (`ROLE_ADMIN` or `ROLE_MARKETER`).

### 7.2 Cryptographic Signing Strategy (`ODD-SEC-08`)
- **Status:** **`[OPEN DESIGN DECISION]`**
- **Architectural Policy:** JWT bearer authentication is **`[DECIDED]`**. However, the exact cryptographic signing algorithm and key architecture remain open:
  - *Candidate A (Symmetric):* HMAC algorithms (`HS256`, `HS384`, `HS512`) utilizing a shared server-side secret. Simplifies deployment in a modular monolith.
  - *Candidate B (Asymmetric):* Public/Private key pairs (`RS256`, `ES256`). Decouples token verification from token issuance.
- *Constraint:* The algorithm choice must be finalized prior to implementation freeze. Insecure algorithms (`alg: none`) are strictly rejected.

### 7.3 Token Lifecycle & Storage Governance

| Lifecycle Dimension | Status | Architectural Rule & Constraint |
| :--- | :--- | :--- |
| **Access Token Lifetime** | **`[OPEN DESIGN DECISION]` (`ODD-SEC-01`)** | Exact duration (e.g., 15 minutes vs. 1 hour vs. 8 hours) is **NOT** frozen. Must be balanced between token exposure risk and user re-authentication friction. |
| **Refresh Token Support** | **`[OPEN DESIGN DECISION]` (`ODD-SEC-02`)** | Whether to implement refresh tokens remains open. If adopted, refresh tokens must be managed without compromising server statelessness. |
| **Token Revocation Strategy** | **`[OPEN DESIGN DECISION]` (`ODD-SEC-03`)** | Pure stateless expiration vs. active user status verification (`users.is_active`) vs. future database-backed revocation. **CRITICAL INVARIANT:** Redis **MUST NOT** be used for JWT blacklisting or revocation state. |
| **Signing Key Management** | **`[DEFERRED]` (`ODD-SEC-06`)** | Base deployment utilizes environment-injected secrets (`JWT_SECRET`). Automated key rotation and JWKS endpoints are deferred to enterprise ops hardening. |

---

## 8. Password and Credential Security

### 8.1 Password Hashing Policy (`ODD-SEC-04`)
- **Status:** **`[OPEN DESIGN DECISION / REQUIRES TESTING]`**
- **Architectural Policy:** Password hashing is a mandatory security control. **BCrypt** is evaluated and **`[RECOMMENDED]`** as the industry-standard salted hashing mechanism. However, BCrypt is not an immutable baseline mandate; alternatives (e.g., Argon2id) remain permissible if selected prior to implementation.
- **Work Factor / Cost Parameter:** The computational cost parameter (e.g., BCrypt log rounds 10 vs. 12) is **NOT** frozen. Sizing requires empirical benchmarking on target server hardware to balance brute-force resistance against CPU latency during concurrent login spikes.

### 8.2 Password Complexity Policy (`ODD-SEC-05`)
- **Status:** **`[OPEN DESIGN DECISION]`**
- **Architectural Policy:** Neither `docs/SRS.md` nor upstream designs freeze a rigid regex password complexity rule. A baseline requirement of minimum 8 characters with mixed character classes is **`[RECOMMENDED]`**, but formal regex constraints remain an open decision.

### 8.3 Administrative Password Reset Flow
Administrative credential reset (`PATCH /api/v1/users/{id}/password`) updates the user's password record in MySQL. Cleartext passwords must **never** be written to logs, cached in memory, or stored in transient Redis queues.

---

## 9. RBAC and Authorization Model

### 9.1 Canonical Role Definitions
The platform defines exactly **two canonical roles** (`docs/SRS.md` §3.1, `docs/design/System-Architecture.md` §4.3):
1. **`ROLE_ADMIN`:** Full administrative superset privilege. Owns user account administration, platform security, customer soft-deletion, and system-level configuration.
2. **`ROLE_MARKETER`:** Business operational privilege. Restricted to customer profile authoring, dynamic segment creation/preview, campaign launch, and performance reporting.

$$\text{ROLE\_ADMIN} \supset \text{ROLE\_MARKETER}$$

### 9.2 Endpoint Authorization Matrix (36 Endpoints)

| Canonical Domain | Endpoint URI | Method | Permitted Authority | Authorization Type |
| :--- | :--- | :--- | :--- | :--- |
| **Authentication** | `/api/v1/auth/login` | `POST` | `Anonymous` | `permitAll()` |
| | `/api/v1/users` | `POST` | `ROLE_ADMIN` | Method Security |
| | `/api/v1/users` | `GET` | `ROLE_ADMIN` | Method Security |
| | `/api/v1/users/{id}` | `GET` | `ROLE_ADMIN` | Method Security |
| | `/api/v1/users/{id}/role` | `PATCH` | `ROLE_ADMIN` | Method Security |
| | `/api/v1/users/{id}/deactivate`| `PATCH` | `ROLE_ADMIN` | Method Security |
| | `/api/v1/users/{id}/password`| `PATCH` | `ROLE_ADMIN` | Method Security |
| **Customer** | `/api/v1/customers` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/customers/{id}` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/customers/{id}` | `PATCH` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/customers/{id}` | `DELETE` | `ROLE_ADMIN` | Method Security (Admin Only)|
| | `/api/v1/customers` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/customers/count` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| **Upload** | `/api/v1/uploads/bulk` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/uploads/history` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| **Segment** | `/api/v1/segments` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/segments/{id}` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/segments/{id}` | `PATCH` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/segments/{id}` | `DELETE` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/segments` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/segments/{id}/preview`| `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/segments/{id}/members`| `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| **Campaign** | `/api/v1/campaigns` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/campaigns/{id}` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/campaigns/{id}` | `PATCH` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/campaigns/{id}` | `DELETE` | `ROLE_ADMIN` | Method Security (Admin Only)|
| | `/api/v1/campaigns` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/campaigns/{id}/launch`| `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| **Delivery** | `/api/v1/campaigns/{id}/delivery-summary` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/campaigns/{id}/deliveries` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| **AI** | `/api/v1/ai/segments/generate-rules` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/ai/segments/audits` | `GET` | `ROLE_ADMIN` | Method Security (Admin Only)|
| **Reporting** | `/api/v1/reports/campaigns/{id}` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/reports/customers/overview` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/reports/campaigns/{id}/ai-summary` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |
| | `/api/v1/reports/campaigns/history` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Method Security |

*Traceability Note:* The administrative restriction on customer deletion, campaign deletion, and AI segment audit inspection reflects explicit security design choices aligned with least privilege.

---

## 10. Security Enforcement Boundaries

### 10.1 Authentication vs. Authorization Enforcement
- **Authentication Enforcement:** Handled globally in the servlet filter chain via `JwtAuthenticationFilter`. Executes prior to controller dispatch. Rejects missing, malformed, or expired tokens immediately with `401 Unauthorized`.
- **Authorization Enforcement:** Enforced via Spring Security method interceptors (`@PreAuthorize("hasRole('ADMIN')")`). Executes at the service boundary. Rejects authenticated callers lacking required authorities with `403 Forbidden`.

### 10.2 Administrative Self-Protection Invariant
The platform service layer strictly enforces self-protection invariants:
1. An administrator cannot demote their own account role (`PATCH /api/v1/users/{id}/role`).
2. An administrator cannot deactivate their own active account (`PATCH /api/v1/users/{id}/deactivate`).
Violation attempts are rejected with `400 Bad Request` or `409 Conflict`.

---

## 11. Redis Async Delivery Architecture

### 11.1 Infrastructure Role of Redis
Redis operates **strictly as transient asynchronous messaging transport**. Redis is **NOT** an application database, **NOT** a persistent business store, and **NOT** a general application cache. Zero Cache-Aside architecture exists in v1.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ Relational Store: MySQL 8.x (Authoritative Source of Truth)                            │
│ - Authoritative delivery obligations: campaign_delivery_records (status = 'PENDING')   │
│ - Authoritative campaign lifecycle state: campaigns (status = 'RUNNING')               │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                     Asynchronous Dispatch  │  Authoritative Status Updates
                         (XADD Event)       │    (UPDATE SENT/FAILED)
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│ Transient Transport: Redis Streams (Volatile Buffering & Consumer Coordination)        │
│ - Transient Stream Key: Stores in-flight delivery event references                     │
│ - Consumer Group: Coordinates concurrent worker threads                                │
│ - Pending Entries List (PEL): Tracks unacknowledged in-flight tasks                    │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 12. Delivery Lifecycle

### 12.1 End-to-End Delivery Sequence

```
[1. Launch Command]
     │ POST /api/v1/campaigns/{id}/launch
     ▼
[2. Persistence Boundary: MySQL InnoDB Transaction]
     │ - Acquire row lock: SELECT * FROM campaigns WHERE id = :id FOR UPDATE
     │ - Verify status == DRAFT
     │ - Authoritatively evaluate bound segment AST against MySQL (WHERE deleted_at IS NULL)
     │ - If matched audience == 0 -> REJECT (campaign remains DRAFT, 400/422 per ODD-API-02)
     │ - If matched audience > 0:
     │   - UPDATE campaigns SET status = 'RUNNING', started_at = UTC_TIMESTAMP()
     │   - Bulk INSERT audience into campaign_delivery_records (status = 'PENDING')
     │ - COMMIT Transaction
     ▼
[3. Transient Dispatch: Redis Streams]
     │ - Loop over matched audience: XADD stream * campaignId :cid customerId :cuid
     │ - Return HTTP 200 OK (Campaign RUNNING, delivery processing initiated)
     ▼
[4. Asynchronous Consumer Loop: Background Workers]
     │ - Consumer thread executes: XREADGROUP GROUP group worker_N BLOCK ... COUNT ...
     │ - Message placed into Redis Pending Entries List (PEL)
     ▼
[5. Simulated Delivery Execution]
     │ - Worker simulates channel dispatch (90% success, 10% failure per FR-DEL-002)
     ▼
[6. Persistence Update: MySQL InnoDB]
     │ - Conditional atomic update:
     │   UPDATE campaign_delivery_records SET status = 'SENT'/'FAILED', updated_at = UTC_TIMESTAMP()
     │   WHERE campaign_id = :cid AND customer_id = :cuid AND status = 'PENDING'
     ▼
[7. Acknowledgment: Redis Streams]
     │ - Worker executes: XACK stream group :msgId
     │ - Message evicted from Redis PEL
     ▼
[8. Concurrency-Safe Campaign Completion Check]
     │ - Worker evaluates if all delivery obligations for campaign are terminal
     │ - If terminal and zero PENDING -> Atomic transition RUNNING -> COMPLETED
```

---

## 13. Redis Streams Design

### 13.1 Component Definitions & Payload Schema
- **Stream Entity:** Append-only log storing delivery work items.
- **Payload Schema:**
  ```
  campaignId: "8"
  customerId: "1420"
  ```
  *Data Minimization Invariant:* Personal customer data (names, emails, phone numbers) and message bodies are **NOT** placed into Redis. The background worker queries necessary context from MySQL or constructs the message from the campaign template.
- **Consumer Group (`XGROUP`):** Manages cursor positions across multiple consumer threads, ensuring each work item is delivered to a single worker in the group.
- **Pending Entries List (PEL):** Tracks messages delivered to a worker that have not yet been acknowledged via `XACK`.

### 13.2 Unfrozen Operational Parameters

| Stream Parameter | Status | Architectural Governance |
| :--- | :--- | :--- |
| **Stream Key Name** | **`[OPEN DESIGN DECISION]` (`ODD-ASYNC-01`)** | Naming convention (e.g., `crm:campaign:deliveries:stream`) remains open. |
| **Consumer Group Name** | **`[OPEN DESIGN DECISION]` (`ODD-ASYNC-01`)** | Naming convention (e.g., `crm:delivery:workers`) remains open. |
| **Batch Retrieval Size (`COUNT`)** | **`[DEFERRED / REQUIRES TESTING]` (`ODD-ASYNC-04`)** | Number of items pulled per `XREADGROUP` requires throughput testing. |
| **Pending Idle Threshold** | **`[DEFERRED / REQUIRES TESTING]` (`ODD-ASYNC-05`)** | Inactivity timeout before reclaiming pending messages requires operational tuning. |

---

## 14. MySQL ↔ Redis Consistency Boundary

### 14.1 The Post-Commit Enqueue Gap
In a system without distributed transactions or two-phase commit (2PC), a failure window exists between committing the MySQL launch transaction and completing the Redis `XADD` dispatch loop:

```
[Step A: MySQL Transaction Commits] ─── SUCCESS
  - campaigns.status = 'RUNNING'
  - N records created in campaign_delivery_records (status = 'PENDING')
                 │
                 ▼  <--- APPLICATION CRASH OR REDIS FAILURE WINDOW
[Step B: Redis Stream Enqueue] ─────── FAILED
  - Redis receives 0 messages
```

### 14.2 Consistency Invariant & Non-Atomicity
- **No Dual-Write Atomicity:** The platform does **not** assume cross-system atomicity between MySQL and Redis.
- **MySQL as the Authoritative Source of Truth:** `PENDING` records in `campaign_delivery_records` are authoritative persistent delivery obligations. If Redis messages are lost, the obligations remain intact in MySQL.
- **Post-Commit Failure Boundary:** Once the MySQL transaction commits, a subsequent Redis enqueue failure **cannot** roll back the database state. The campaign remains `RUNNING` with `PENDING` obligations.

### 14.3 Candidate Reconciliation Options (`ODD-ASYNC-02`)
The exact mechanism to ensure un-enqueued `PENDING` obligations are processed through Redis remains an **`[OPEN DESIGN DECISION / DEFERRED]`**. Three candidate approaches are under consideration:
- **Candidate A (Synchronous In-Request Enqueue):** Enqueue all tasks within the launch HTTP request thread prior to returning `200 OK`. Simplifies recovery but degrades API latency on large audiences.
- **Candidate B (Asynchronous Background Reconciliation Scanner):** An in-process scheduled component scans MySQL for `RUNNING` campaigns with orphaned `PENDING` records and re-enqueues them into Redis.
- **Candidate C (Event-Driven / In-Process Dispatcher Component):** An in-process application event listener decouples the HTTP thread from the enqueue loop. *Boundary Rule:* This candidate does not imply a separate microservice and must remain strictly within the approved modular-monolith architecture.

---

## 15. Delivery Idempotency and At-Least-Once Semantics

### 15.1 Architectural Delivery Model
The platform operates strictly with **At-Least-Once Delivery Semantics**. The platform does **NOT** provide end-to-end exactly-once processing.

### 15.2 The Four Idempotency Layers

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ Layer 1: Persistent-Record Deduplication (MySQL Storage Level)                         │
│ - UNIQUE(campaign_id, customer_id) on campaign_delivery_records                         │
│ - GUARANTEE: Exactly one persistent row exists per campaign/customer pair.             │
│ - LIMITATION: Does not prevent duplicate message delivery over Redis transport.        │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│ Layer 2: Persistent State-Transition Idempotency (Database Update Level)               │
│ - Conditional SQL Update:                                                              │
│   UPDATE campaign_delivery_records SET status = :terminalStatus, updated_at = :now     │
│   WHERE campaign_id = :cid AND customer_id = :cuid AND status = 'PENDING'              │
│ - GUARANTEE: Exactly one worker transitions the row from PENDING to terminal.          │
│ - LIMITATION: Subsequent workers updating terminal rows affect 0 rows.                 │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│ Layer 3: Message Redelivery (Redis Transport Level)                                    │
│ - Worker crashes or timeouts trigger message redelivery via PEL re-reading.            │
│ - Redelivered tasks are handled gracefully by checking Layer 2 conditional updates.    │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│ Layer 4: Simulated Side-Effect Execution (Channel Level)                               │
│ - Under at-least-once semantics, if a worker executes simulated delivery but crashes   │
│   before executing the MySQL update, a redelivered task will re-execute simulation.    │
│ - INVARIANT: The conditional MySQL update protects the persistent state transition;    │
│   it does NOT guarantee exactly-once execution of the simulated delivery side effect. │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 16. Campaign Completion Consistency

### 16.1 Conceptual Completion Invariant
A marketing campaign transitions from `RUNNING` to `COMPLETED` if and only if all of the following conceptual conditions are satisfied simultaneously:
$$\text{Campaign is RUNNING} + \text{Valid non-zero audience} + \text{Delivery obligations exist} + \text{All obligations terminal} + \text{Zero PENDING obligations} + \text{Concurrency-safe transition} = \text{COMPLETED}$$

### 16.2 Concurrency & Race Condition Rules
1. **Concurrency-Safe Transition:** When multiple worker threads process the final delivery obligations concurrently, only one worker must succeed in triggering the transition to `COMPLETED`. This is enforced via conditional atomic updates against MySQL.
2. **Terminal State Immutability:** Once a campaign reaches `COMPLETED` or `FAILED`, its status is immutable. No subsequent worker execution or administrative command can alter the status or relaunch the campaign.
3. **Absence of `target_audience_size` Schema Field:** In strict alignment with `Database-Design.md` §6.5, the `campaigns` table does not contain a `target_audience_size` column. Audience size is evaluated authoritatively from the delivery records associated with the campaign.

---

## 17. Retry and Failure Model

### 17.1 Failure Classification & Handling Policies

| Failure Type | Description | Operational Handling | Classification |
| :--- | :--- | :--- | :--- |
| **Simulated Failure** | 10% delivery failure mandated by `FR-DEL-002`. | **Terminal Failure.** Row updated to `FAILED` in MySQL; acknowledged via `XACK`. Not retried. | `[DECIDED]` |
| **Transient Infrastructure** | Network glitch connecting to Redis or MySQL lock timeout. | **Retryable.** Task not acknowledged; remains in Redis PEL for reprocessing. | `[DECIDED]` |
| **Poison Message** | Corrupted data or missing customer reference that repeatedly crashes worker. | Must be quarantined after exceeding maximum delivery attempts. Updated to `FAILED` with diagnostic reason; acknowledged via `XACK` to evict from PEL. | `[DEFERRED / REQUIRES TESTING]` |
| **Persistent Statuses** | Relational delivery state in MySQL. | Strictly `PENDING`, `SENT`, and `FAILED`. Persistent `PROCESSING` status is strictly prohibited. | `[DECIDED]` |

---

## 18. Worker and Concurrency Model

### 18.1 Separation of Consumer Coordination and Worker Execution
The application strictly separates the Redis Stream consumer thread from task execution:
1. **Consumer Coordinator Thread:** Executes `XREADGROUP` in a non-blocking or short-polling loop to pull message batches.
2. **Worker Thread Pool:** An in-process `ExecutorService` executes simulated delivery logic, conditional MySQL updates, and `XACK` calls.
3. **Backpressure Governance:** If the worker thread pool's task queue reaches capacity, the consumer coordinator halts pulling new messages from Redis, preventing heap saturation.

### 18.2 Graceful Shutdown Lifecycle
Upon application shutdown (`SIGTERM`):
1. Consumer coordinator halts pulling new messages from Redis.
2. Active worker threads are given a bounded window to complete in-flight MySQL updates and `XACK` invocations.
3. Incomplete tasks remain safely tracked in the Redis PEL for subsequent recovery upon restart.

---

## 19. Recovery and Pending-Message Handling

### 19.1 Candidate In-Flight Recovery Capabilities
When a worker crashes while holding an in-flight message, the message remains tracked in the Redis Stream Pending Entries List (PEL).
- **Candidate Native Capability:** Redis Streams native capabilities, such as `XAUTOCLAIM` or `XPENDING` inspection, represent prime candidates for reclaiming abandoned tasks whose idle time exceeds a threshold.
- **Governance:** The exact recovery mechanism, scanning frequency, and idle thresholds remain an **`[OPEN DESIGN DECISION / DEFERRED]`** (`ODD-ASYNC-05`).

---

## 20. Spring AI + Gemini Architecture

### 20.1 Architectural Integration Overview
Google Gemini is integrated via the **Spring AI** framework across three distinct, isolated functional areas:

```
                                  ┌───────────────────────────────┐
                                  │      Spring AI Framework      │
                                  └───────────────┬───────────────┘
                                                  │
                 ┌────────────────────────────────┼────────────────────────────────┐
                 │                                │                                │
┌────────────────▼───────────────┐ ┌──────────────▼───────────────┐ ┌──────────────▼───────────────┐
│ Domain 1: Dynamic Segment AST  │ │ Domain 2: Message Template   │ │ Domain 3: Campaign Narrative  │
│ Rule Generation                │ │ Personalization              │ │ Performance Summary           │
├────────────────────────────────┤ ├──────────────────────────────┤ ├───────────────────────────────┤
│ Interface: POST /ai/segments/..│ │ Interface: Background Worker │ │ Interface: GET /reports/...   │
│ Input: Natural language text   │ │ Input: Customer attributes   │ │ Input: Aggregated MySQL stats │
│ Output: Structured JSON AST    │ │ Output: Plain text string    │ │ Output: Markdown summary text │
│ Validation: Multi-stage schema │ │ Validation: Length/format    │ │ Validation: Format sanitiz.   │
│ Persistence: ai_segment_audits │ │ Persistence: None / delivery │ │ Persistence: campaigns.ai_sum │
└────────────────────────────────┘ └──────────────────────────────┘ └───────────────────────────────┘
```

### 20.2 The Untrusted Input Mandate
All data received from Google Gemini is treated as **untrusted input**. The external LLM is strictly decoupled from database execution:
- Gemini **NEVER** generates executable SQL, HQL, or JPQL.
- Gemini is **NEVER** the authoritative source of business truth.
- All AI-generated structures must pass strict internal application validation before consumption.

---

## 21. AI Segmentation Trust Boundary

### 21.1 Schema-First AST Validation Pipeline
Dynamic audience rule generation adheres to a strict multi-stage compilation pipeline:

```
[User Natural Language Prompt]
              │
              ▼
[Google Gemini via Spring AI]
              │ Returns Structured JSON
              ▼
┌────────────────────────────────────────────────────────┐
│ Application Validation Layer (Untrusted Input Boundary)│
│ 1. Syntactic JSON Parsing: Well-formed syntax check    │
│ 2. Schema Validation: Group combinators (AND, OR) only │
│ 3. Field Whitelist: totalSpend, orderCount, etc. only  │
│ 4. Operator Whitelist: EQUALS, GREATER_THAN, etc. only │
│ 5. Type Safety: Values safely coerced to target types  │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             │ Valid AST                 │ Invalid / Malformed
             ▼                           ▼
[JPA Criteria API Compiler]      [Reject: 422 Unprocessable Entity]
- Type-safe Predicate generation  - Log failure in ai_segment_audits
- Parameterized PreparedStatement - Mask internal error diagnostics
             │
             ▼
[Execute Safely against MySQL]
```

*Architectural Security Boundary:* Keyword blacklisting is explicitly **not** the security boundary. The application does not execute AI-generated queries; validated AST nodes are compiled through the controlled JPA Criteria API path, preventing AI output from directly becoming executable SQL.

### 21.2 Approved Audit Schema Conformance
Per `FR-AI-SEG-004` and `Database-Design.md` §6.8, segmentation prompts and responses are audited exclusively within the approved `ai_segment_audits` schema:
- `id`: Primary key (`BIGINT UNSIGNED`).
- `user_id`: Authenticated user requesting generation (`BIGINT UNSIGNED`).
- `prompt_text`: Raw input prompt string (`TEXT`).
- `generated_rules`: Generated JSON AST payload (`JSON`).
- `action_taken`: User disposition enum (`SAVED` vs `DISCARDED`).
- `segment_id`: Linked segment reference (`BIGINT UNSIGNED`, nullable).
- `created_at`, `updated_at`: UTC timestamps.
*Schema Boundary:* No unapproved columns (e.g., `is_valid`, `error_message`, `generated_rule_tree`) are added to the database.

---

## 22. AI Personalization Architecture

### 22.1 Context Minimization & Execution
When campaign messages are personalized dynamically:
- Only minimal, non-sensitive customer attributes (`firstName`, `location`, `orderCount`, `totalSpend`) are injected into the prompt context.
- Sensitive credentials, contact addresses, and financial identifiers are barred from prompt templates.

### 22.2 Deferred Fallback Strategy (`ODD-API-09` / `ODD-AI-01`)
- **Status:** **`[DEFERRED]`**
- **Architectural Policy:** AI personalization fallback behavior is explicitly deferred. Possible strategies under consideration include:
  - Base-template placeholder substitution (e.g., replacing `{{firstName}}` directly via string token replacement).
  - A static non-AI fallback message.
  - Deferred personalization retry.
- *Constraint:* The delivery worker must not block or abort campaign processing if Gemini fails during personalization.

---

## 23. AI Reporting Architecture

### 23.1 Separation of Facts from Narrative
- **Authoritative Facts:** Numerical metrics (sent count, failed count, delivery rates, execution durations) originate exclusively from MySQL aggregation queries.
- **Narrative Role:** Gemini receives the pre-calculated facts and generates an executive summary. Gemini is **never** asked to calculate, aggregate, or guess metrics.

### 23.2 Summary Persistence
Where persistence of the AI-generated campaign performance summary is required by the approved design, `campaigns.ai_summary` is the designated relational storage column (`Database-Design.md` §6.5). No secondary reporting database or audit table is introduced.

---

## 24. AI Validation and Security Controls

### 24.1 Prompt Injection Mitigation
Prompt injection attacks attempting to alter segmentation logic or bypass business rules are mitigated by:
1. Strict schema validation of the returned JSON structure.
2. Whitelisting permitted customer fields and operators.
3. Parameterized query generation via JPA Criteria API.

---

## 25. AI Failure / Degradation Model

### 25.1 Failure Scenarios & System Response

| Failure Scenario | Direct Symptom | System Behavior & Fallback |
| :--- | :--- | :--- |
| **Gemini Socket Timeout** | HTTP client read/connect timeout. | Fail-fast; release web server threads; return `503 Service Unavailable`. |
| **Gemini Outage / Rate Limit** | HTTP 429 or 503 from external API. | Back off; return `503`. Non-AI core CRM features remain fully operational. |
| **Malformed JSON Payload** | Unparseable syntax or markdown wrapper. | JSON parser catches fault; record failure in `ai_segment_audits`; return `422`. |
| **Semantic Validation Failure** | Rule references unapproved column. | AST Validator rejects structure; audit record logged; return `422`. |
| **Personalization Timeout** | Worker timeout during send simulation. | Execute deferred fallback strategy (`ODD-API-09`); record delivery record. |

*Architectural Reliability Rule:* Core non-AI functionality is architecturally isolated from Gemini availability. Exact degraded behavior for AI-dependent operations depends on the later timeout/retry/fallback design.

---

## 26. Data Privacy and Data Minimization

### 26.1 Privacy Invariants Across External Perimeters
1. **Minimum Necessary Data:** Prompts sent to external AI providers must contain only the structural attributes required for the specific task.
2. **Prohibited Sensitive Context:** Passwords, password hashes, full phone numbers, recipient email addresses, and secret tokens are strictly barred from external AI prompts.
3. **Log Sanitization:** Sensitive authorization tokens and raw credentials must be redacted from application and diagnostic logs.

---

## 27. Observability and Structured Logging

### 27.1 Structured MDC Logging
Operational observability is implemented using **SLF4J / Logback** with **Mapped Diagnostic Context (MDC)** (`NFR-MAINT-003`):
- `requestId`: Unique correlation UUID generated at the API filter boundary.
- `userId`: Identifier of authenticated user executing request.
- `campaignId`: Contextual campaign ID for launch, delivery, and reporting events.
- `workerId`: Identifier of background worker executing async delivery tasks.

### 27.2 Storage Boundary Categorization
To preserve architectural boundaries, logging and audit categories are strictly separated:
- **Application Logs:** Rolling text/JSON log files capturing system events, exceptions, and lifecycle states.
- **Business Transaction State:** Relational records in MySQL (`campaigns`, `campaign_delivery_records`).
- **Domain Audit Records:** Dedicated MySQL tables (`ai_segment_audits`, `upload_history`).
- **Generic Audit Table:** **`[STRICTLY OUT OF SCOPE]`** (confirmed by DBD-16).

---

## 28. Security / Async / AI Threats and Mitigations

| Threat / Risk | Root Cause | Impact | Mitigation Strategy | Status |
| :--- | :--- | :--- | :--- | :--- |
| **JWT Leakage** | Token intercepted on untrusted network. | Unauthorized API access. | Enforce HTTPS/TLS; keep token lifetime bounded (`ODD-SEC-01`); verify user `is_active`. | `[OPEN]` |
| **Privilege Escalation** | Marketer invokes administrative endpoint. | Unauthorized user or system mutation. | Method-level authorization via `@PreAuthorize("hasRole('ADMIN')")` at service layer. | `[DECIDED]` |
| **Redis Enqueue Gap** | Crash after MySQL commit but before Redis `XADD`. | Campaign stuck in `RUNNING` with orphaned `PENDING` rows. | Evaluate candidate reconciliation mechanisms (`ODD-ASYNC-02`) to re-enqueue pending obligations. | `[OPEN]` |
| **Worker Crash In-Flight** | Worker crashes while holding unacknowledged message. | Task stalled in Redis PEL; delivery unfulfilled. | Reclaim idle pending tasks via candidate Redis Streams capabilities (e.g., `XAUTOCLAIM`). | `[DEFERRED]` |
| **Duplicate Delivery Execution** | Message redelivered under at-least-once transport. | Simulated send executed multiple times. | Conditional MySQL update protects persistent state; at-least-once delivery accepted by design. | `[DECIDED]` |
| **Campaign Completion Race** | Concurrent workers updating final delivery records. | Missed transition to `COMPLETED`. | Conditional atomic update verifies all records terminal and zero PENDING before updating status. | `[DECIDED]` |
| **AI Prompt Injection** | Malicious text attempting SQL execution. | Potential data breach or query manipulation. | Schema-first AST validation; compilation via JPA Criteria API; zero raw SQL execution. | `[DECIDED]` |
| **Gemini Outage / Latency** | Google Cloud API degradation or rate limiting. | Web server thread exhaustion. | Strict HTTP client timeouts; fail-fast with `503`. Non-AI CRM features operate independently. | `[DECIDED]` |

---

## 29. Dependency and Scope Control

### 29.1 Permitted Technology Stack
- **Language & Platform:** Java 21, Spring Boot 3.x.
- **Persistence Store:** MySQL 8.x (InnoDB engine ONLY).
- **Transient Async Transport:** Redis Streams (approved Redis deployment; transient transport only).
- **Security:** Spring Security 6.x, JWT library (JJWT or Spring Security JWT).
- **AI Integration:** Spring AI with Google Gemini client.
- **Build & Test:** Maven, JUnit 5, Mockito. (Testcontainers is classified as `[OPTIONAL TESTING CANDIDATE — NOT YET DECIDED]`).

### 29.2 Explicit Architectural Scope Exclusions
The following technologies and patterns are **strictly excluded** from the platform architecture:
- **No PostgreSQL:** MySQL 8.x is the sole persistent store.
- **No Apache Kafka / RabbitMQ:** Redis Streams satisfies all transient buffering needs.
- **No Resilience4j:** Circuit breakers excluded; native HTTP client timeouts utilized.
- **No Redis Cache-Aside / Caching:** Redis contains zero business state, zero token revocation state, and zero caching layers.
- **No Generic `audit_logs` Table:** Confirmed `[OUT OF SCOPE]` by DBD-16.
- **No Microservices / 2PC / Distributed Transactions:** Modular monolith architecture with local ACID transactions.

---

## 30. Decision Register

### 30.1 Decided Architecture Invariants (`[DECIDED]`)
1. **Stateless Security:** Spring Security 6.x with stateless JWT bearer token authentication (`NFR-SEC-001`).
2. **Two-Role Taxonomy:** Exactly two canonical roles: `ROLE_ADMIN` and `ROLE_MARKETER`; ADMIN is superset.
3. **Sole Persistent Store:** MySQL 8.x (InnoDB) is the sole persistent source of truth.
4. **Database Transaction Isolation:** `READ COMMITTED` isolation level across all relational operations (`DBD-18`).
5. **Transient Async Messaging:** Redis Streams operates strictly as transient asynchronous messaging transport.
6. **No Redis Application State:** Redis contains zero persistent state, zero cache data, and zero JWT revocation state.
7. **At-Least-Once Delivery:** Async delivery operates with at-least-once semantics. Persistent delivery statuses are strictly `PENDING`, `SENT`, and `FAILED` (no persistent `PROCESSING` status).
8. **Persistent Record Deduplication:** Composite index `UNIQUE(campaign_id, customer_id)` on `campaign_delivery_records` prevents duplicate persistent rows.
9. **Zero-Audience Campaign Launch:** Launch is rejected; campaign remains in `DRAFT` status and does not transition to `RUNNING`.
10. **Untrusted AI Perimeter:** Google Gemini is an untrusted external service; Gemini never generates executable SQL; schema-first AST validation via Criteria API compiles safe queries.
11. **AI Reporting Source of Truth:** Authoritative facts originate from MySQL; narrative summaries persist in `campaigns.ai_summary`.

### 30.2 Recommended Architectural Patterns (`[RECOMMENDED]`)
1. **Password Hashing:** BCrypt password hashing with per-user salt evaluated as preferred algorithm.
2. **Standardized Response Envelope:** Uniform success/error envelope across all REST endpoints.
3. **OpenAPI Tooling:** SpringDoc OpenAPI 2.x (OpenAPI 3.x-compatible) for Spring Boot 3.x.

### 30.3 Open Design Decisions (`[OPEN DESIGN DECISION]`)
- **`ODD-SEC-01`:** JWT Access Token Lifetime duration.
- **`ODD-SEC-02`:** Refresh Token support, lifecycle, and storage strategy.
- **`ODD-SEC-03`:** JWT Token Revocation strategy (stateless expiration vs. user `is_active` check vs. DB-backed).
- **`ODD-SEC-04`:** Password Hashing algorithm finalization and computational cost parameters.
- **`ODD-SEC-05`:** Formal password complexity policy and regex constraints.
- **`ODD-SEC-08`:** JWT cryptographic signing algorithm (symmetric HMAC vs. asymmetric RSA/ECDSA).
- **`ODD-ASYNC-01`:** Redis Stream key name and consumer group naming conventions.
- **`ODD-ASYNC-02`:** MySQL $\to$ Redis enqueue failure reconciliation mechanism.
- **`ODD-AI-02`:** Gemini model selection (`gemini-1.5-flash` vs `gemini-1.5-pro`) and generation parameters.
- **`ODD-API-01`:** API-Level `Idempotency-Key` header support.
- **`ODD-API-02`:** Zero-audience campaign launch rejection HTTP status code (`400` vs `422`).
- **`ODD-API-03`:** Error response envelope representation (Custom Envelope vs RFC 7807 Problem Details).
- **`ODD-API-04`:** Default and maximum collection pagination sizes.
- **`ODD-API-05`:** AI rule generation response envelope structure (`estimatedCount` inclusion).
- **`ODD-API-06`:** Campaign deletion semantics (allowed states and persistence behavior).
- **`ODD-API-07`:** Bulk upload error response bounding in HTTP payload.

### 30.4 Deferred & Testing-Dependent Decisions (`[DEFERRED]` / `[REQUIRES TESTING]`)
- **`ODD-SEC-06`:** Signing Key Management and rotation strategy (`[DEFERRED]`).
- **`ODD-SEC-07`:** Deployment-level TLS protocol version and cipher configuration (`[DEFERRED]`).
- **`ODD-ASYNC-03`:** Worker concurrency and thread pool sizing (`[DEFERRED / REQUIRES TESTING]`).
- **`ODD-ASYNC-04`:** Stream batch retrieval count (`COUNT`) (`[DEFERRED / REQUIRES TESTING]`).
- **`ODD-ASYNC-05`:** Pending message idle threshold and poison message policy (`[DEFERRED / REQUIRES TESTING]`).
- **`ODD-AI-01` / `ODD-API-09`:** AI personalization fallback strategy (`[DEFERRED]`).
- **`ODD-AI-03`:** AI socket and connect timeout durations (`[DEFERRED / REQUIRES TESTING]`).
- **`ODD-AI-04`:** AI personalization batching and rate limiting thresholds (`[DEFERRED / REQUIRES TESTING]`).
- **`ODD-API-08`:** Segment AST complexity constraints (max depth and condition counts) (`[DEFERRED / REQUIRES TESTING]`).
- **`ODD-API-10`:** Bulk file upload size and row count thresholds (`[DEFERRED / REQUIRES TESTING]`).

---

## 31. Traceability Matrix

| Requirement ID | Architectural Focus | Classification | Implementation & Design Realization |
| :--- | :--- | :--- | :--- |
| **`FR-SEC-001`** | Authentication & User Mgmt | `[EXPLICIT SRS]` | Salted password hashing; JWT token issuance; `/api/v1/auth/login`. |
| **`FR-SEC-002`** | Role-Based Access Control | `[EXPLICIT SRS]` | Exactly two roles (`ROLE_ADMIN`, `ROLE_MARKETER`); method security. |
| **`NFR-SEC-001`** | Stateless Session Security | `[EXPLICIT SRS]` | Stateless JWT Bearer authentication; zero server HTTP session state. |
| **`NFR-SEC-002`** | Transport Layer Encryption | `[EXPLICIT SRS]` | HTTPS/TLS required in deployed environments; exact version open. |
| **`FR-DEL-001`** | Async Message Dispatch | `[EXPLICIT SRS]` | Redis Streams transient transport; background consumer worker threads. |
| **`FR-DEL-002`** | Simulated Delivery Worker | `[EXPLICIT SRS]` | 90% success, 10% simulated failure; at-least-once dispatch semantics. |
| **`FR-DEL-003`** | Delivery Status Updates | `[EXPLICIT SRS]` | Conditional `UPDATE campaign_delivery_records SET status = ...`. |
| **`FR-CAMP-003`** | Immediate Campaign Launch | `[EXPLICIT SRS]` | Row-lock `SELECT ... FOR UPDATE`; immediate evaluation against MySQL. |
| **`FR-CAMP-004`** | Zero-Audience Rejection | `[EXPLICIT SRS]` | Decided: Launch rejected, campaign remains `DRAFT`. Status code open. |
| **`FR-AI-SEG-001`** | Natural Language Query Input | `[EXPLICIT SRS]` | Accepted via `POST /api/v1/ai/segments/generate-rules`. |
| **`FR-AI-SEG-002`** | Structured AST Output | `[EXPLICIT SRS]` | Gemini returns JSON AST; schema-first validation compiles Criteria query. |
| **`FR-AI-SEG-003`** | AI Fallback & Isolation | `[EXPLICIT SRS]` | Core CRM independent of AI; fail-fast with `503 Service Unavailable`. |
| **`FR-AI-SEG-004`** | AI Generation Auditing | `[EXPLICIT SRS]` | Persistently recorded in MySQL `ai_segment_audits` table. |
| **`FR-REPORT-003`** | AI Campaign Summary | `[EXPLICIT SRS]` | Narrative generated from MySQL facts; stored in `campaigns.ai_summary`. |
| **`DBD-18`** | Transaction Isolation | `[DATABASE DECISION]` | MySQL InnoDB `READ COMMITTED` isolation level (`[DECIDED]`). |
| **`ODD-API-09`** | Personalization Fallback | `[API DESIGN DECISION]` | Dynamic personalization fallback strategy (`[DEFERRED]`). |

---

## 32. Risks and Deferred Work

1. **MySQL $\to$ Redis Enqueue Gap:** Post-commit application failure requires resolution via candidate reconciliation options (`ODD-ASYNC-02`) prior to production deployment.
2. **External AI Rate Limiting & Latency:** Google Gemini response latency spikes require client timeouts and fallback testing (`ODD-AI-03`, `ODD-AI-04`).
3. **Benchmarking Dependencies:** Worker thread pool sizing, Redis batch counts, and upload row thresholds remain testing-dependent to prevent unbenchmarked resource starvation.

---

## 33. Implementation Boundaries

- **No Implementation Code in Design:** This specification defines architectural contracts and operational boundaries. No Java classes, configuration files, or database DDL scripts are provided herein.
- **Subordination Invariant:** Developers implementing Phase 3 must strictly adhere to the baseline chain: `SRS v1.0.1` $\to$ `System Architecture v1.0.0` $\to$ `Database Design v1.0.0` $\to$ `API Design v1.0.0` $\to$ `Security-Async-AI.md v1.0.0`.

---

## 34. Summary

The Security, Asynchronous Delivery, and AI Architecture Specification establishes a cohesive, enterprise-grade foundation for the **Enterprise AI-CRM Platform (`CS-CRM-2026`)**:
- **Stateless & Robust Security:** Comprehensive identity governance using Spring Security 6.x, JWT bearer tokens, and least-privilege RBAC.
- **Resilient Asynchronous Delivery:** Scalable Redis Streams transport operating under realistic at-least-once semantics, backed by authoritative MySQL persistence and conditional state transitions.
- **Zero-Trust AI Integration:** Complete containment of external LLM interactions through schema-first AST validation, dynamic Criteria API compilation, customer data minimization, and persistent compliance auditing.

---

## 35. Document Status

- **Status:** **APPROVED BASELINE**
- **Lifecycle Phase:** Phase 2 — System Design
- **Approved Baseline Chain:** SRS v1.0.1 → System Architecture v1.0.0 → Database Design v1.0.0 → API Design v1.0.0 → Security + Async + AI Design v1.0.0

---
