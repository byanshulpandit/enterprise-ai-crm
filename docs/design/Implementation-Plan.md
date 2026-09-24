# Implementation Plan Specification
## Enterprise AI-CRM Platform (`CS-CRM-2026`)

---

## 1. Document Control

### 1.1 Document Metadata
- **Project Code:** CS-CRM-2026
- **System Name:** Enterprise AI-CRM Platform
- **Document Title:** Implementation Plan & Development Roadmap Specification
- **Document Path:** `docs/design/Implementation-Plan.md`
- **Document Version:** 1.1.0
- **SDLC Phase:** Phase 2 — System Design (Implementation Planning)
- **Document Status:** **APPROVED BASELINE (M4 SECURITY FROZEN)**
- **Date:** 2026-09-20
- **Author:** Senior Enterprise Software Architect & Engineering Lead
- **Target Audience:** Backend Engineers, QA Engineers, DevOps Engineers, Project Evaluators

### 1.2 Baseline Authority Chain
This implementation roadmap translates the approved architecture into sequential, bounded, and testable engineering milestones. It is strictly governed by:
$$\text{SRS v1.0.1} \longrightarrow \text{System Architecture v1.0.0} \longrightarrow \text{Database Design v1.0.1} \longrightarrow \text{API Design v1.0.1} \longrightarrow \text{Security + Async + AI Design v1.1.0} \longrightarrow \text{Implementation Plan v1.1.0}$$

---

## 2. Purpose

This document defines the formal, step-by-step engineering roadmap for implementing the **Enterprise AI-CRM Platform (`CS-CRM-2026`)**. It establishes a disciplined, dependency-safe progression from project bootstrap to production-ready deployment across 13 distinct milestones (`M0` through `M12`).

As a cornerstone of a learning-first enterprise SDLC, this plan guarantees that:
1. Every engineering task is strictly bounded and traceable to approved design specifications.
2. Architecture patterns (DTO/Entity isolation, Criteria API compilation, Redis Stream transient buffering, and untrusted AI containment) are implemented without architectural drift.
3. Code is produced incrementally with unit, integration, and manual verification checkpoints before progressing.
4. Open and deferred architectural decisions are managed without premature freezing.

---

## 3. Approved Baselines

The implementation plan is derived exclusively from the following five approved baselines:

1. **[docs/SRS.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/SRS.md) (v1.0.1 — APPROVED BASELINE):** Sole functional/non-functional source of truth (`FR-CUST-*`, `FR-UPLOAD-*`, `FR-SEG-*`, `FR-CAMP-*`, `FR-DEL-*`, `FR-AI-*`, `FR-SEC-*`, `FR-REPORT-*`, `NFR-*`).
2. **[docs/design/System-Architecture.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/System-Architecture.md) (v1.0.0 — APPROVED BASELINE):** Modular monolith architecture, layered package boundaries, DTO/Entity decoupling, transient async transport, Criteria API compilation, and Spring AI isolation.
3. **[docs/design/Database-Design.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/Database-Design.md) (v1.0.0 — APPROVED BASELINE):** 8 canonical tables (`users`, `customers`, `customer_tags`, `segments`, `campaigns`, `campaign_delivery_records`, `upload_history`, `ai_segment_audits`), `READ COMMITTED` isolation (`DBD-18`), soft-delete mechanics (`deleted_at`), and composite index `UNIQUE(campaign_id, customer_id)`.
4. **[docs/design/API-Design.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/API-Design.md) (v1.0.0 — APPROVED BASELINE):** 36 REST endpoints across 8 canonical domains, `/api/v1/` base path, uniform response envelopes, semantic HTTP status codes, and zero-audience launch rejection invariant.
5. **[docs/design/Security-Async-AI.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/Security-Async-AI.md) (v1.0.0 — APPROVED BASELINE):** Spring Security 6.x stateless JWT bearer authentication, two-role RBAC (`ROLE_ADMIN`, `ROLE_MARKETER`), at-least-once Redis Streams async dispatch, four-layer idempotency model, and schema-first untrusted AI validation.

---

## 4. Implementation Governance

### 4.1 Strict Scope Boundaries
- **Create Only:** This planning artifact (`docs/design/Implementation-Plan.md`).
- **No Implementation Artifacts:** No Java source files, Spring `@RestController`/`@Service` classes, JPA entities, DTOs, SQL migrations, configuration files, or tests are created in this phase.
- **Strict Prohibitions:** No PostgreSQL, Kafka, RabbitMQ, Resilience4j, Redis Cache-Aside/application caching, generic `audit_logs` table, persistent `PROCESSING` status, distributed transactions / 2PC, `target_audience_size` database column, or microservice deployments.

### 4.2 Decision Preservation Policy
Open and deferred decisions (`ODD-API-*`, `DBD-*`, `ODD-SEC-*`, `ODD-ASYNC-*`, `ODD-AI-*`) must remain open throughout implementation planning. When a milestone encounters an open decision, it must encapsulate the decision behind an abstraction or testing boundary without silently resolving it.

---

## 5. Architecture-to-Implementation Mapping

The platform's 8 canonical domains map directly to dedicated, modular service packages:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ Presentation Layer (REST Controllers, DTOs, API Envelopes, OpenAPI Annotations)       │
├─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬──────────────────┤
│    auth     │  customer   │   upload    │   segment   │  campaign   │ delivery / report│
└──────┬──────┴──────┬──────┴──────┬──────┴──────┬──────┴──────┬──────┴────────┬─────────┘
       │             │             │             │             │               │
┌──────▼─────────────▼─────────────▼─────────────▼─────────────▼───────────────▼─────────┐
│ Service Layer (Business Logic, Invariant Enforcement, AST Validation, Orchestration)   │
├─────────────┬─────────────┬─────────────┬─────────────┬─────────────┬──────────────────┤
│ UserService │ CustomerSvc │ UploadSvc   │ SegmentSvc  │ CampaignSvc │ DeliveryWorker   │
└──────┬──────┴──────┬──────┴──────┬──────┴──────┬──────┴──────┬──────┴────────┬─────────┘
       │             │             │             │             │               │
┌──────▼─────────────▼─────────────▼─────────────▼─────────────▼───────────────▼─────────┐
│ Infrastructure / Persistence / Messaging Layers                                        │
├─────────────────────────────────────────────┬──────────────────────────────────────────┤
│ MySQL 8.x InnoDB (Spring Data JPA / JDBC)   │ Redis Streams (Transient Transport Only) │
│ - 8 Canonical Entities                      │ - In-flight message buffering & PEL      │
└─────────────────────────────────────────────┴──────────────────────────────────────────┘
```

---

## 6. Implementation Principles

1. **Learning-First Engineering:** Every layer must be understood and justifiable. Developers must understand *why* DTOs decouple from entities, *why* Criteria API prevents SQL injection, and *why* Redis holds no persistent state.
2. **Strict Layer Decoupling:** Persistence entities never cross controller boundaries; API DTOs never enter database repositories.
3. **Fail-Fast Validation:** Syntactic checks execute via Jakarta Bean Validation; domain rules execute at the service boundary; constraints are enforced by MySQL.
4. **Authoritative Persistence:** MySQL is the sole persistent source of truth; Redis is strictly transient transport.
5. **Untrusted AI Integration:** Google Gemini responses are treated as untrusted user input and strictly validated before consumption.
6. **Bounded Implementation Steps:** Implementation progresses through small, verifiable, atomic units.

---

## 7. Dependency Graph

```
M0: Requirements & SDLC Baseline
 │
 ▼
M1: Project Bootstrap & Foundation (Java 21, Spring Boot 3.x, Maven, Base Config)
 │
 ├────────────────────────────────────────┐
 ▼                                        ▼
M2: Database Schema & Customer CRUD     M4: Authentication, JWT & RBAC
 │                                        │
 ▼                                        │
M3: Validation & Exception Handling       │
 │                                        │
 ├────────────────────────────────────────┘
 ▼
M6: Dynamic Segmentation & Criteria API
 │
 ├────────────────────────────────────────┐
 ▼                                        ▼
M5: Campaign & Audience Domain          M7: Bulk CSV/XLSX Ingestion
 │
 ▼
M8: Redis Infrastructure & Async Transport
 │
 ▼
M9: Campaign Dispatch & Concurrency
 │
 ▼
M10: Spring AI Integration (Gemini)
 │
 ▼
M11: Comprehensive Testing & Observability
 │
 ▼
M12: Docker Containerization, Deployment & Final Documentation
```

---

## 8. Milestone Roadmap

| Milestone ID | Milestone Name | Primary Focus | Critical Output |
| :--- | :--- | :--- | :--- |
| **`M0`** | Requirements & SDLC Baseline | Formal baseline freeze and SDLC setup | Approved specifications & environment requirements |
| **`M1`** | Project Bootstrap | Spring Boot 3.x setup, packages, MySQL/Redis connectivity | Runnable application skeleton & health check |
| **`M2`** | Database & Customer CRUD | Schema provisioning strategy, JPA Customer entity, CRUD APIs | Working Customer domain with soft delete |
| **`M3`** | Validation & Exception Handling | Global exception handler, Bean Validation, API error contract | Consistent error responses & parameter validation |
| **`M4`** | Authentication, JWT & RBAC | Spring Security 6.x, JWT filter, two-role RBAC | Authenticated & role-protected endpoint access |
| **`M5`** | Campaign & Audience Domain | Campaign entity, state machine, draft lifecycle | Campaign CRUD & segment reference foundations |
| **`M6`** | Dynamic Segmentation | JSON AST validation, JPA Criteria compilation, preview | Segment CRUD, Criteria API compilation & audience evaluation engine |
| **`M7`** | Bulk CSV/XLSX Ingestion | Streaming file parse, row validation, partial success | Streaming upload with `upload_history` tracking |
| **`M8`** | Redis & Async Architecture | Redis Streams setup, consumer group, worker loop | Transient message enqueue & at-least-once consume |
| **`M9`** | Campaign Dispatch & Concurrency | Launch transaction with M6 audience evaluation, worker execution, completion check | Atomic campaign launch & safe completion |
| **`M10`** | Spring AI Integration | Gemini client, AST translation, auditing, reporting | Natural language to AST & narrative summaries |
| **`M11`** | Testing & Observability | Cross-domain automated testing, regression & failure testing, SLF4J/MDC logging, reporting verification | Automated test suite & contextual request logging |
| **`M12`** | Docker & Deployment | Dockerfile, Docker Compose, OpenAPI documentation | Containerized platform deployment & Swagger UI |

---

## 9. M0 — Requirements + SDLC Baseline

### 9.1 Objective
Establish the formal project governance baseline, freeze foundational specifications, and configure developer environment standards prior to writing code.

### 9.2 Why This Milestone Exists
Prevents premature coding, scope creep, and architectural drift by ensuring all engineering participants operate from identical approved constraints.

### 9.3 Preconditions & Dependencies
None. This is the foundational inception milestone.

### 9.4 Requirements Covered
All requirements across `docs/SRS.md` v1.0.1.

### 9.5 Design Documents Used
`docs/SRS.md`, `docs/design/System-Architecture.md`, `docs/design/Database-Design.md`, `docs/design/API-Design.md`, `docs/design/Security-Async-AI.md`.

### 9.6 Main Implementation Areas
- Git repository initialization and branching strategy configuration (`main`, `develop`, `feature/*`).
- Developer environment standardization: JDK 21 (Temurin/OpenJDK), Maven 3.9+, Docker Desktop, IDE code style.
- Verification of access to approved baseline documentation.

### 9.7 Expected Artifacts
- Git repository initialized with standard `.gitignore` and `.editorconfig`.
- Frozen `docs/` repository baseline.

### 9.8 Testing Scope
- **Unit Testing:** N/A.
- **Integration Testing:** N/A.
- **Manual Verification:** Confirm baseline files exist and Git repository reflects proper initial state.

### 9.9 Negative / Edge Cases
Attempted addition of unapproved dependencies or architecture patterns is caught and rejected.

### 9.10 Security Considerations
Branch protection rules on `main` and `develop`.

### 9.11 Database / API Impact
None.

### 9.12 Git Commit Boundary
Initial commit establishing repo structure and approved documentation baseline.

### 9.13 Definition of Done & Exit Criteria
- All 5 baseline specifications approved and committed.
- Branching workflow and environment standards confirmed.

### 9.14 Dependencies on Later Milestones
Enables `M1`.

---

## 10. M1 — Project Bootstrap

### 10.1 Objective
Bootstrap the Spring Boot 3.x project structure with Java 21, establish the approved package/domain organization and create only the minimal package structure required by the bootstrap implementation, and verify runtime connectivity to local MySQL and Redis infrastructure.

### 10.2 Why This Milestone Exists
Establishes the clean compile, build, configuration, and connectivity foundation before writing domain business logic.

### 10.3 Preconditions & Dependencies
Requires `M0`. Local or containerized MySQL 8.x and Redis available.

### 10.4 Requirements Covered
`NFR-PERF-001`, `NFR-MAINT-001`, `NFR-DATA-005`.

### 10.5 Design Documents Used
`System-Architecture.md` §4, `Database-Design.md` §3.

### 10.6 Main Implementation Areas
- Maven `pom.xml` configuration: Spring Boot 3.x starter web, data-jpa, validation, redis.
- Package and Domain Organization: Establish the approved package/domain organization and create only the minimal package structure required by the bootstrap implementation. The approved canonical domains remain:
  - `auth`
  - `customer`
  - `upload`
  - `segment`
  - `campaign`
  - `delivery`
  - `ai`
  - `reporting`
- Minimal Bootstrap Scope Boundary: Do NOT create empty classes, placeholder services, placeholder controllers, placeholder repositories, or unnecessary common/utils/helpers packages. Future domain packages may be created incrementally when their respective implementation milestone begins. The approved domain architecture remains strictly preserved.
- Externalized configuration (`application.yml`): MySQL JDBC datasource, Redis host/port, JPA Hibernate properties (naming strategy, dialect).
- Spring Boot main application class and actuator `/actuator/health` verification.

### 10.7 Expected Artifacts
- Buildable Maven project.
- Minimal bootstrap package structure (no empty domain placeholders).
- Configuration profiles (`default`, `test`).

### 10.8 Testing Scope
- **Unit Testing:** Context loads test (`PlatformApplicationTests`).
- **Integration Testing:** Spring Boot context startup test verifying DataSource and RedisConnectionFactory beans.
- **Manual Verification:** Build via `mvn clean compile`; start application; verify healthy response from actuator endpoint.

### 10.9 Negative / Edge Cases
- Invalid database credentials or unreachable Redis port gracefully logs configuration error and halts startup.

### 10.10 Security Considerations
No hardcoded credentials in `application.yml`; utilize environment variable placeholders (`${DB_PASSWORD:}`).

### 10.11 Database / API Impact
Database connection established; zero business tables created yet.

### 10.12 Git Commit Boundary
Commit: `feat(bootstrap): initialize Spring Boot 3.x application with package hierarchy and base config`.

### 10.13 Definition of Done & Exit Criteria
Application builds cleanly via Maven, starts without errors, and connects to infrastructure.

### 10.14 Dependencies on Later Milestones
Enables `M2` and `M4`.

---

## 11. M2 — Database + Customer CRUD

### 11.1 Objective
Establish the database schema provisioning / migration strategy, map the `Customer` and `CustomerTag` JPA entities, and deliver full RESTful CRUD and paginated directory retrieval for the Customer domain.

### 11.2 Why This Milestone Exists
Customer management is the primary transactional anchor of the CRM upon which segmentation, uploads, and campaigns depend.

### 11.3 Preconditions & Dependencies
Requires `M1`.

### 11.4 Requirements Covered
`FR-CUST-001`, `FR-CUST-002`, `NFR-DATA-001`, `NFR-DATA-002`, `NFR-DATA-003`.

### 11.5 Design Documents Used
`Database-Design.md` §4.2, §4.3; `API-Design.md` §14.2.

### 11.6 Main Implementation Areas
- Database schema provisioning / migration strategy: Define the schema provisioning/migration strategy for creating the `customers` and `customer_tags` tables with foreign keys and unique constraints.
  *Technology Neutrality Policy:* The specific database migration/provisioning tool is not frozen by the approved baseline and must not be introduced as an architectural decision through this implementation plan. Do NOT introduce Flyway, Liquibase, or any other migration framework. No migration dependencies are added.
- Entities: `Customer` (`id`, `firstName`, `lastName`, `email`, `phone`, `totalSpend`, `orderCount`, `lastOrderDate`, `location`, `deletedAt`, `createdAt`, `updatedAt`) and `CustomerTag`.
- Repositories: `CustomerRepository` extending `JpaRepository` and `JpaSpecificationExecutor`.
- DTOs: `CustomerRequestDto`, `CustomerResponseDto`, `CustomerSummaryDto`.
- Mappers: Dedicated bidirectional mapping components between DTOs and entities.
- Service: `CustomerService` enforcing business rules (unique active email check, soft-delete execution).
- Controller: `CustomerController` mapping 6 REST endpoints:
  - `POST /api/v1/customers`
  - `GET /api/v1/customers/{id}`
  - `PATCH /api/v1/customers/{id}`
  - `DELETE /api/v1/customers/{id}`
  - `GET /api/v1/customers`
  - `GET /api/v1/customers/count`

### 11.7 Expected Artifacts
- Database schema provisioning definitions, JPA entities, Spring Data repositories, DTO classes, Service and Controller implementations.

### 11.8 Testing Scope
- **Unit Testing:** Service layer tests mocking repository; mapper unit tests.
- **Integration Testing:** Repository tests verifying query generation and soft-delete filtering; MockMvc API tests verifying HTTP 200/201/204/404 responses.
- **Manual Verification:** Create customer via cURL/Postman; verify row in MySQL; execute soft-delete; verify `deleted_at` is set and excluded from subsequent GET requests.

### 11.9 Negative / Edge Cases
- Duplicate active email returns `409 Conflict`.
- Requesting a soft-deleted customer returns `404 Not Found`.
- Negative spend or invalid email rejected.

### 11.10 Security Considerations
Customer soft-deletion restricted by service contract; data validation prevents parameter tampering.

### 11.11 Database / API Impact
`customers` and `customer_tags` tables active in MySQL. 6 customer endpoints functional.

### 11.12 Git Commit Boundary
Commit: `feat(customer): implement customer entities, repository, service, and CRUD endpoints with soft-delete`.

### 11.13 Definition of Done & Exit Criteria
All 6 customer endpoints verified against database with unit and integration test coverage.

### 11.14 Dependencies on Later Milestones
Enables `M3`, `M6`, and `M7`.

---

## 12. M3 — Validation + Exception Handling

### 12.1 Objective
Implement platform-wide declarative request validation, centralized global exception handling, and standardized error response formatting.

### 12.2 Why This Milestone Exists
Prevents malformed data from reaching service layers and ensures uniform, predictable error contracts for API consumers without leaking internal implementation details.

### 12.3 Preconditions & Dependencies
Requires `M2`.

### 12.4 Requirements Covered
`NFR-SEC-001`, `NFR-MAINT-001`, `NFR-MAINT-003`, `FR-CUST-001`.

### 12.5 Design Documents Used
`API-Design.md` §8, §9, §10, §11.

### 12.6 Main Implementation Areas
- Jakarta Bean Validation annotations applied across all Request DTOs (`@NotNull`, `@Size`, `@Email`, `@Min`, `@DecimalMin`).
- Custom domain exceptions: `ResourceNotFoundException`, `DuplicateResourceException`, `InvalidStateTransitionException`, `BusinessValidationException`.
- Global exception handler (`@RestControllerAdvice`):
  - Intercepts `MethodArgumentNotValidException` and extracts field-level validation errors.
  - Intercepts `DataIntegrityViolationException` and maps constraint violations cleanly.
  - Intercepts uncaught exceptions and returns sanitized `500 Internal Server Error`.
- Response contract: Implements recommended success envelope and isolates error representation behind configurable adapter pending `ODD-API-03` resolution.

### 12.7 Expected Artifacts
- Global `@RestControllerAdvice` class, custom exception hierarchy, standardized error DTOs.

### 12.8 Testing Scope
- **Unit Testing:** Exception handler unit tests verifying status code and payload mapping.
- **Integration Testing:** MockMvc tests submitting invalid JSON bodies and asserting HTTP 400 with detailed error lists.
- **Manual Verification:** Submit malformed POST requests; confirm error response structure and absence of Java stack traces.

### 12.9 Negative / Edge Cases
- Missing mandatory fields, empty strings, malformed JSON syntax, invalid data types (string for number).

### 12.10 Security Considerations
Stack traces and internal database errors (SQL syntax, table names) are masked from API consumers.

### 12.11 Database / API Impact
Uniform error contracts active across all existing and future endpoints.

### 12.12 Git Commit Boundary
Commit: `feat(validation): implement global exception handling and Bean Validation error mapping`.

### 12.13 Definition of Done & Exit Criteria
Zero uncaught exceptions return HTML error pages; all validation failures yield structured JSON responses.

### 12.14 Dependencies on Later Milestones
Enables `M4`, `M5`, `M6`, and `M7`.

---

## 13. M4 — Authentication + JWT + RBAC

### 13.1 Objective
Implement stateless user authentication using Spring Security 6.x, JWT token generation/validation, password hashing, and role-based access control across `ROLE_ADMIN` and `ROLE_MARKETER`.

### 13.2 Why This Milestone Exists
Protects platform resources from unauthorized access and enforces least-privilege role boundaries across the 36 REST endpoints.

### 13.3 Preconditions & Dependencies
Requires `M1` and `M3`.

### 13.4 Requirements Covered
`FR-SEC-001`, `FR-SEC-002`, `NFR-SEC-001`, `NFR-SEC-002`.

### 13.5 Design Documents Used
`Database-Design.md` §6.1; `API-Design.md` §13, §14.1; `Security-Async-AI.md` §6, §7, §8, §9, §10.

### 13.6 Main Implementation Areas
- **Database Schema:** Exactly one new M4 table `users` (`id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY`, `username VARCHAR(50) NOT NULL UNIQUE`, `email VARCHAR(255) NOT NULL UNIQUE`, `password_hash VARCHAR(255) NOT NULL`, `role VARCHAR(20) NOT NULL`, `is_active BOOLEAN NOT NULL DEFAULT TRUE`, `created_at DATETIME(6) NOT NULL`, `updated_at DATETIME(6) NOT NULL`). Check constraint restricts roles to `ROLE_ADMIN` and `ROLE_MARKETER`.
- **Customer Lifecycle Preservation:** `customers` table is NOT modified by M4; `deleted_at` remains the sole customer lifecycle marker (no `customers.status`).
- **User Domain Entities:** `User` JPA entity, `UserRepository`, and Spring Security `UserDetailsService`.
- **Password Security:** BCrypt work factor strength 12. Password policy: minimum 8 characters, maximum 72 characters, maximum 72 UTF-8 bytes. Same rules for user creation and password update. Plaintext passwords are never stored or logged.
- **JWT Provider & Configuration:**
  - Mandatory environment variable `JWT_SECRET`; no fallback/default secret; startup fails fast if missing or < 32 UTF-8 bytes (256 bits).
  - Algorithm: `HS256` (HMAC-SHA256). Issuer: `cs-crm-2026`.
  - Access token lifetime: exactly 1 hour (3,600,000 ms).
  - Refresh tokens: NOT implemented in M4.
  - Claims: `sub` (canonical username), `uid` (user ID), `role` (diagnostic claim), `iss` (`cs-crm-2026`), `iat`, `exp`, `jti`.
- **Unified Login:** `POST /api/v1/auth/login` (permitAll). Request JSON field named `"username"` accepts either username OR email; backend queries `findByUsernameOrEmail`. Issued JWT `sub` is ALWAYS canonical username.
- **Filter Chain & Live Authority Source:**
  - `JwtAuthenticationFilter` intercepting requests with `Authorization: Bearer <token>`.
  - Performs cryptographic signature, expiration, and issuer validation.
  - Loads CURRENT `User` record from MySQL; checks `users.is_active` (if false: does not populate `SecurityContextHolder`, rejects with 401 via `AuthenticationEntryPoint`).
  - Uses CURRENT database role as authoritative `GrantedAuthority` (JWT role claim is diagnostic; DB role wins). Role changes take effect on the next request.
- **Security Error Separation:**
  - `AuthenticationEntryPoint` = `401 Unauthorized` (filter-chain unauthenticated, invalid/expired token, inactive account).
  - `AccessDeniedHandler` = `403 Forbidden` (filter-chain authenticated caller lacking required role).
  - `GlobalExceptionHandler` = controller/service/validation exceptions.
- **Method Security & Roles:**
  - `@EnableMethodSecurity` enforcing role checks.
  - Roles: Exactly `ROLE_ADMIN` and `ROLE_MARKETER`. Hierarchy: `ROLE_ADMIN > ROLE_MARKETER`. Stored in `users.role` (no `roles` or `user_roles` tables).
- **Initial Admin Bootstrap:**
  - Executes ONLY when `userRepository.count() == 0`.
  - Provisions exactly one configured `ROLE_ADMIN` account.
  - If `count() > 0`, bootstrap runner does nothing.
  - If users table is non-empty but contains no active admin, automatic bootstrap does NOT intervene (requires explicit DBA procedure).
  - No Redis, no distributed locking, transactional execution, no hardcoded credentials, no plaintext password logging.
- **Admin Self-Protection Invariant:**
  - Admin cannot demote own role (`PATCH /api/v1/users/{id}/role`).
  - Admin cannot deactivate own account (`PATCH /api/v1/users/{id}/deactivate`).
- **Customer Endpoint Authorization:**
  - `POST /api/v1/customers` -> ADMIN + MARKETER
  - `GET /api/v1/customers` -> ADMIN + MARKETER
  - `GET /api/v1/customers/{id}` -> ADMIN + MARKETER
  - `PATCH /api/v1/customers/{id}` -> ADMIN + MARKETER
  - `GET /api/v1/customers/count` -> ADMIN + MARKETER
  - `DELETE /api/v1/customers/{id}` -> ADMIN only
- **User Management Endpoints (ADMIN only):**
  - `POST /api/v1/users`
  - `GET /api/v1/users`
  - `GET /api/v1/users/{id}`
  - `PATCH /api/v1/users/{id}/role`
  - `PATCH /api/v1/users/{id}/deactivate`
  - `PATCH /api/v1/users/{id}/password` (password updates do not revoke existing JWTs; tokens expire in 1h; immediate termination via deactivation)

### 13.7 Expected Artifacts
- Security configuration classes (`SecurityConfig`, filter chain, entry point, access denied handler), JWT provider component, User entity, repository, service, Auth/User controllers, bootstrap runner.

### 13.8 Testing Scope
- **Existing Test Suite Baseline (Zero Regressions):**
  - All existing 39 tests must remain passing (`CustomerIntegrationTest` = 21, `CustomerValidationAndExceptionTest` = 15, `PlatformApplicationTests` = 3).
- **Unit Testing:** JWT provider token generation and expiration unit tests; password hashing verification tests (BCrypt strength 12).
- **Integration Testing:** MockMvc tests asserting:
  - Unauthenticated access to protected endpoints returns `401 Unauthorized`.
  - Marketer accessing Admin-only endpoint returns `403 Forbidden`.
  - Admin successfully accesses protected endpoints.
  - Deactivated user request returns `401 Unauthorized` immediately.
  - Role elevation/demotion takes effect on next request via live database authority.
  - Bootstrap provisioning on empty database provisions single admin; subsequent restart with count > 0 performs no-op.
  - Admin self-protection prevents self-demotion and self-deactivation.
- **Manual Verification:** Log in via Postman; capture JWT; invoke protected endpoint with and without token.

### 13.9 Negative / Edge Cases
- Expired tokens, forged signatures, malformed headers, deactivated accounts, missing roles.

### 13.10 Security Considerations
Stateless token handling; passwords hashed with salt; no credentials logged; cleartext passwords never stored.

### 13.11 Database / API Impact
`users` table active. 7 authentication/user endpoints operational and secured.

### 13.12 Git Commit Boundary
Commit: `feat(security): implement Spring Security 6.x stateless JWT authentication and RBAC`.

### 13.13 Definition of Done & Exit Criteria
All secured endpoints reject unauthenticated or unauthorized requests; role hierarchy verified by integration tests.
- **Status:** **`[IMPLEMENTED & VERIFIED]`**
- **Test Results:** 176/176 tests passing (`mvn clean test` and `mvn clean package` successful). Full M4 security filter chain, live database user/role authority check, initial admin bootstrap, unified login, user management endpoints, password byte boundaries, and customer RBAC matrix verified.

### 13.14 Dependencies on Later Milestones
Enables secured access across all subsequent milestones (`M5` through `M12`).

---

## 14. M5 — Campaign + Audience Domain

### 14.1 Objective
Implement campaign authoring/domain foundations, establish the approved relationship/reference to Segment, and implement campaign validation and lifecycle foundations in MySQL.

### 14.2 Why This Milestone Exists
Campaigns are the central execution entity that bind audience definitions to message templates and drive delivery operations.

### 14.3 Preconditions & Dependencies
Requires `M2`, `M3`, and `M4`.

### 14.4 Requirements Covered
`FR-CAMP-001`, `FR-CAMP-002`, `FR-CAMP-005`.

### 14.5 Design Documents Used
`Database-Design.md` §4.5; `API-Design.md` §14.5, §15; `Security-Async-AI.md` §16.

### 14.6 Main Implementation Areas
- Schema Provisioning: Provision `campaigns` table with `CHECK (status IN ('DRAFT', 'RUNNING', 'COMPLETED', 'FAILED'))` and foreign keys.
- Entity: `Campaign` (`id`, `name`, `segment_id`, `message_template`, `status`, `started_at`, `completed_at`, `ai_summary`, `created_by`, `created_at`, `updated_at`).
  *Schema Boundary:* No `target_audience_size` column is created.
- Segment Linkage: Establish the approved relationship/reference to Segment (`segment_id` foreign key referencing `segments(id)`).
- Repository: `CampaignRepository` with custom status query methods.
- Service: `CampaignService` enforcing lifecycle rules:
  - New campaigns always initialize in `DRAFT`.
  - Modifications permitted only when `status == DRAFT`.
  - Immediate execution model (no scheduling).
- Milestone Scope Boundary:
  - Implement campaign authoring/domain foundations.
  - Establish the approved relationship/reference to Segment.
  - Implement campaign validation and lifecycle foundations.
  - Do NOT implement the Criteria API segmentation engine here.
  - Do NOT duplicate segment evaluation logic here.
  - Campaign launch may remain dependent on the segmentation capability completed in `M6`.
- Controller: `CampaignController` mapping 5 endpoints:
  - `POST /api/v1/campaigns`
  - `GET /api/v1/campaigns/{id}`
  - `PATCH /api/v1/campaigns/{id}`
  - `DELETE /api/v1/campaigns/{id}`
  - `GET /api/v1/campaigns`

### 14.7 Expected Artifacts
- Schema provisioning definitions, `Campaign` entity, repository, DTOs, service, and controller.

### 14.8 Testing Scope
- **Unit Testing:** Campaign state machine unit tests; template length validation tests.
- **Integration Testing:** API tests verifying campaign creation in `DRAFT`, editing draft campaigns, and rejection of edits on non-draft states.
- **Manual Verification:** Create campaign via API; verify initial `DRAFT` status and timestamps in MySQL.

### 14.9 Negative / Edge Cases
- Binding to non-existent segment ID rejected.
- Updating a campaign in non-DRAFT status yields `409 Conflict`.
- Message template exceeding length limit rejected.

### 14.10 Security Considerations
Both Admin and Marketer can author campaigns; campaign deletion restricted to Admin per API Design.

### 14.11 Database / API Impact
`campaigns` table created; 5 campaign management endpoints operational.

### 14.12 Git Commit Boundary
Commit: `feat(campaign): implement campaign entity, draft lifecycle rules, and CRUD endpoints`.

### 14.13 Definition of Done & Exit Criteria
Campaign lifecycle correctly enforced at service and database constraint levels; test suite passes.
- **Status:** **`[IMPLEMENTED & VERIFIED]`**
- **Test Results:** 229/229 tests passing (`mvn clean test` and `mvn clean package` successful). Schema provisioning for `segments` and `campaigns` completed with MySQL CHECK constraints and foreign keys; `Segment` and `Campaign` domain entities, DTOs, services, and REST controllers operational under `/api/v1/segments` and `/api/v1/campaigns`; campaign draft lifecycle (`DRAFT`, immutable on non-draft states) enforced; M4 RBAC rules enforced (`ROLE_ADMIN` and `ROLE_MARKETER` CRUD, campaign delete restricted to `ROLE_ADMIN`); direct MySQL foreign key constraints (`ON DELETE RESTRICT`) and CHECK constraint verified; mass assignment protection and segment update after campaign binding verified.

### 14.14 Dependencies on Later Milestones
Enables `M8`, `M9`, and `M10`. Note: Actual campaign launch execution in `M9` is strictly dependent on the dynamic segmentation and audience evaluation capability completed in `M6`.

---

## 15. M6 — Dynamic Segmentation / Criteria API

### 15.1 Objective
Implement actual segment creation and evaluation, JSON AST validation, Criteria API compilation, and preview/member evaluation to establish the authoritative audience evaluation capability used later by campaign launch.

### 15.2 Why This Milestone Exists
Enables targeted marketing campaigns based on customer behavior and demographic attributes without direct SQL concatenation.

### 15.3 Preconditions & Dependencies
Requires `M2`, `M3`, and `M4`.

### 15.4 Requirements Covered
`FR-SEG-001`, `FR-SEG-002`, `FR-SEG-003`, `NFR-SEC-001`.

### 15.5 Design Documents Used
`Database-Design.md` §4.4; `API-Design.md` §10.1, §14.4; `Security-Async-AI.md` §21.

### 15.6 Main Implementation Areas
- Segment Creation & Evaluation Foundations:
  - Schema Provisioning: Provision `segments` table (`id`, `name`, `description`, `rules`, `created_by`, `created_at`, `updated_at`).
  - Domain Model: Rule AST object tree (`RuleGroup`, `RuleCondition`, `OperatorEnum`, `FieldEnum`).
- JSON AST Validation: `SegmentRuleValidator` enforcing:
  - Whitelisted fields (`totalSpend`, `orderCount`, `lastOrderDate`, `location`, `tags`).
  - Whitelisted operators (`EQUALS`, `GREATER_THAN`, `BETWEEN`, `CONTAINS`, etc.).
  - Combinators (`AND`, `OR`).
  - Value type compatibility.
  - Bounded recursive depth limit (`MAX_DEPTH = 10`; rejects trees exceeding depth with clean 400 Bad Request to prevent stack overflow or unbounded recursion).
- Criteria API Compilation: `CriteriaQueryCompiler` dynamically converting validated AST into a `Predicate` applied to `Customer` entity:
  - Mandatory inclusion of `WHERE deleted_at IS NULL`.
  - Zero raw SQL concatenation.
- Preview & Member Evaluation:
  - Dynamic preview execution returning matching audience count.
  - Paginated member retrieval against live dataset.
- Authoritative Audience Evaluation Engine: Establish the authoritative audience evaluation capability used later by campaign launch in `M9`.
- Controller: `SegmentController` mapping 7 endpoints:
  - `POST /api/v1/segments`
  - `GET /api/v1/segments/{id}`
  - `PATCH /api/v1/segments/{id}`
  - `DELETE /api/v1/segments/{id}` (blocked if referenced by campaigns via `RESTRICT`)
  - `GET /api/v1/segments`
  - `POST /api/v1/segments/{id}/preview` (returns matched audience count)
  - `GET /api/v1/segments/{id}/members` (paginated member list)

### 15.7 Expected Artifacts
- AST domain model, AST validator, Criteria API compiler, Segment entity/repo/service/controller.

### 15.8 Testing Scope
- **Unit Testing:** AST validator tests on valid/invalid rule trees; Criteria compiler predicate tests.
- **Integration Testing:** Database tests verifying AST evaluation against seeded customer dataset; preview count accuracy; member retrieval pagination.
- **Manual Verification:** Create segment with complex `(Spend > 5000 AND Location = 'Delhi')`; trigger preview endpoint; verify matching count aligns with database records.

### 15.9 Negative / Edge Cases
- Unsupported field names, illegal operators for data types, empty groups, soft-deleted customer exclusion.
- Deleting a segment bound to an existing campaign returns `409 Conflict`.

### 15.10 Security Considerations
Complete injection immunity through Criteria API parametrization; no raw SQL execution.

### 15.11 Database / API Impact
`segments` table active; 7 segment endpoints operational.

### 15.12 Git Commit Boundary
Commit: `feat(segment): implement dynamic rule AST validation, Criteria API compilation, and preview endpoints`.

### 15.13 Definition of Done & Exit Criteria
Dynamic queries evaluate accurately against live MySQL customer data; AST validation rejects malformed trees.
- **Status:** **`[IMPLEMENTED & VERIFIED]`**
- **Test Results:** 290/290 tests passing (`mvn clean test` and `mvn clean package` successful; 0 failures, 0 errors, 0 skipped; 52 new M6 tests added across unit, model, parser, compiler, controller, and live MySQL integration test suites from 238 M5 baseline). No pre-existing test cases were deleted or replaced. Existing test classes were extended with additional M6 test cases. Schema validation, AST parsing, whitelisted field/operator validation, bounded recursive depth limit (`MAX_DEPTH = 10`), JPA Criteria API compilation with strict `deleted_at IS NULL` soft-delete filtering, correlated subquery `CustomerTag` handling (eliminating cartesian and duplicate rows; no application-level N+1 query pattern; tag filtering uses correlated Criteria subqueries executed within the database query), preview endpoint (`POST /api/v1/segments/{id}/preview`), paginated members endpoint (`GET /api/v1/segments/{id}/members`), count and member `totalElements` consistency, and M4 security authorization (`ROLE_ADMIN`, `ROLE_MARKETER`) fully verified against live MySQL 8.x database.

### 15.14 Dependencies on Later Milestones
Enables `M5` campaign launch and `M10` AI rule generation.

---

## 16. M7 — Bulk CSV/XLSX Ingestion

### 16.1 Objective
Implement streaming bulk ingestion of customer records via multipart CSV and XLSX files with row-level validation, partial success support, and audit tracking.

### 16.2 Why This Milestone Exists
Enables high-throughput customer data onboarding without exhausting server memory or failing entire files due to isolated row errors.

### 16.3 Preconditions & Dependencies
Requires `M2`, `M3`, and `M4`.

### 16.4 Requirements Covered
`FR-UPLOAD-001` through `FR-UPLOAD-006`, `NFR-SCALE-002`.

### 16.5 Design Documents Used
`Database-Design.md` §4.7; `API-Design.md` §14.3, §17.

### 16.6 Main Implementation Areas
- Migration: Create `upload_history` table (`id`, `file_name`, `status`, `total_records`, `successful_records`, `failed_records`, `error_details`, `created_by`, `created_at`, `updated_at`).
- Streaming Parsers:
  - CSV: Streaming line parser (OpenCSV / Apache Commons CSV).
  - XLSX: Event-driven streaming parser (Apache POI SAX parsing) to avoid loading entire DOM into heap.
- Validation Pipeline: Row-by-row field validation and duplicate email check against batch.
- Batch Persistence: Partition valid records into chunks for batch insert into MySQL.
- Partial Success Engine: Valid rows committed in transactions; failed rows captured with row index and reason into `error_details`.
- Controller: `UploadController` mapping:
  - `POST /api/v1/uploads/bulk` (`multipart/form-data`)
  - `GET /api/v1/uploads/history` (paginated audit trail)

### 16.7 Expected Artifacts
- Streaming parsers, ingestion service, `UploadHistory` entity/repo, Upload controller.

### 16.8 Testing Scope
- **Unit Testing:** Row validation unit tests; error details serialization tests.
- **Integration Testing:** Upload test CSV with mixed valid/invalid rows; verify database record count increases by valid count; verify `upload_history` status `PARTIAL_SUCCESS`.
- **Manual Verification:** Upload multi-row CSV/XLSX; inspect `upload_history` record in database.

### 16.9 Negative / Edge Cases
- Corrupt files, missing headers, empty files, mixed character encodings, duplicate emails within the same file.

### 16.10 Security Considerations
File type and structure validation; file processing streams discarded immediately; no persistent temp files on host disk.

### 16.11 Database / API Impact
`upload_history` table created; 2 upload endpoints active.

### 16.12 Git Commit Boundary
Commit: `feat(upload): implement streaming CSV/XLSX ingestion with partial success and audit history`.

### 16.13 Definition of Done & Exit Criteria
Files ingest successfully with partial success handling; heap memory remains bounded during parse.

### 16.14 Dependencies on Later Milestones
Enables large dataset testing in `M9` and `M11`.

---

## 17. M8 — Redis + Asynchronous Architecture

### 17.1 Objective
Configure Redis Streams as the transient asynchronous messaging transport, establish consumer group coordination, and implement the background worker message consumption loop.

### 17.2 Why This Milestone Exists
Decouples campaign launch from message delivery processing, ensuring high API responsiveness and resilience.

### 17.3 Preconditions & Dependencies
Requires `M1` and `M5`. Local/containerized Redis active.

### 17.4 Requirements Covered
`FR-DEL-001`, `FR-DEL-002`, `NFR-SCALE-003`, `NFR-REL-002`.

### 17.5 Design Documents Used
`Database-Design.md` §4.6; `System-Architecture.md` §5.2; `Security-Async-AI.md` §11, §12, §13, §18.

### 17.6 Main Implementation Areas
- Migration: Create `campaign_delivery_records` table with composite index `UNIQUE(campaign_id, customer_id)`.
- Redis Stream Infrastructure Service: Stream initialization, consumer group creation (`XGROUP CREATE`).
- Dispatch Producer: Enqueues minimal task payload (`campaignId`, `customerId`) via `XADD`.
- Consumer Loop: Dedicated background coordinator executing `XREADGROUP` in non-blocking polling cycles.
- Worker Pool: In-process `ExecutorService` executing delivery simulation tasks:
  - Simulates delivery channel dispatch (90% success, 10% failure per `FR-DEL-002`).
  - Executes conditional update against `campaign_delivery_records` (`status = 'SENT'/'FAILED'`).
  - Calls `XACK` to remove task from Redis PEL.
- Graceful Shutdown: Lifecycle bean cleanly halting consumer loop and awaiting worker task completion on `SIGTERM`.

### 17.7 Expected Artifacts
- Redis stream config, message producer, consumer listener, delivery worker executor, `CampaignDeliveryRecord` entity/repo.

### 17.8 Testing Scope
- **Unit Testing:** Simulated delivery distribution tests (verifying ~90/10 ratio); payload serialization tests; `SmtpDeliveryProviderTest` validating RFC 821/5322 compliance, timeouts, delivery idempotency caching, and connection failure handling; `DeliveryProviderConfigTest` validating provider selection (`simulated` vs `smtp`) and fail-fast startup on invalid inputs.
- **Integration Testing:** Test container/local Redis integration test producing stream messages and asserting consumer receipt, database update, and `XACK` execution; `SmtpDeliveryEndToEndIntegrationTest` with GreenMail verifying full async Campaign $\to$ Outbox $\to$ Redis $\to$ Worker $\to$ SmtpDeliveryProvider $\to$ GreenMail SMTP server $\to$ MySQL delivery ledger update.
- **Manual Verification:** Enqueue test delivery task; monitor Redis CLI (`XPENDING`, `XACK`); verify row updated to `SENT` or `FAILED` in MySQL; inspect local MailHog sink on port 1025.

### 17.9 Negative / Edge Cases
- Worker crashes before `XACK` (task remains in PEL); duplicate message receipt handled via conditional update.

### 17.10 Security Considerations
Customer PII (emails, names, phone numbers) is **never** placed into Redis Stream payloads; only IDs.

### 17.11 Database / API Impact
`campaign_delivery_records` table created; Redis Stream active.

### 17.12 Git Commit Boundary
Commit: `feat(async): implement Redis Streams transient transport, consumer group, and delivery worker`.

### 17.13 Definition of Done & Exit Criteria
Messages flow reliably through Redis Streams; worker updates MySQL and acknowledges tasks without message loss.

### 17.14 Dependencies on Later Milestones
Enables `M9`.

---

## 18. M9 — Campaign Dispatch + Concurrency

### 18.1 Objective
Implement atomic campaign launch orchestration using the completed M6 segmentation/audience evaluation capability, zero-audience rejection, asynchronous Redis dispatch, delivery monitoring APIs, and concurrency-safe campaign completion invariants.

### 18.2 Why This Milestone Exists
Orchestrates the entire campaign execution lifecycle, uniting segments, campaigns, database transactions, and background workers.

### 18.3 Preconditions & Dependencies
Requires `M5`, `M6`, and `M8`.

### 18.4 Requirements Covered
`FR-CAMP-003`, `FR-CAMP-004`, `FR-CAMP-005`, `FR-DEL-003`, `FR-DEL-004`, `FR-DEL-005`.

### 18.5 Design Documents Used
`API-Design.md` §14.5.6, §14.6, §15, §16; `Security-Async-AI.md` §14, §15, §16.

### 18.6 Main Implementation Areas
- Campaign Launch Orchestrator (`POST /api/v1/campaigns/{id}/launch`):
  1. Row-level lock acquisition: `SELECT ... FOR UPDATE` on `campaigns` table.
  2. Status verification: Must be `DRAFT`.
  3. Dynamic segment evaluation: Authoritatively query MySQL using the completed `M6` segmentation/audience evaluation capability for matching active customer IDs.
  4. **Zero-Audience Rejection:** If matched count == 0, launch is rejected. Campaign **MUST REMAIN IN DRAFT**.
  5. If audience > 0: Atomically update `status = 'RUNNING'`, bulk-insert `campaign_delivery_records` in `PENDING` status, and commit transaction.
  6. Dispatch loop: Enqueue task messages to Redis Streams.
  7. Return HTTP `200 OK` (launch initiated).
- Delivery Monitoring APIs:
  - `GET /api/v1/campaigns/{id}/delivery-summary` (aggregates `PENDING`, `SENT`, `FAILED` from MySQL).
  - `GET /api/v1/campaigns/{id}/deliveries` (paginated delivery record log).
- Campaign Completion Evaluator:
  - Evaluates completion invariant: `status == RUNNING`, all obligations terminal, zero `PENDING`.
  - Executes concurrency-safe atomic transition from `RUNNING` to `COMPLETED` (or `FAILED` if platform fault).

### 18.7 Expected Artifacts
- Launch orchestrator, completion manager, delivery query services and controllers.

### 18.8 Testing Scope
- **Unit Testing:** Launch validation unit tests; zero-audience rejection tests.
- **Integration Testing:** End-to-end launch test: author campaign, launch against seeded audience, await worker completion, verify transition to `COMPLETED` and delivery summary counts.
- **Concurrency Testing:** Concurrent launch attempts on the same campaign (only one succeeds; second gets 409).
- **Manual Verification:** Launch campaign via API; poll delivery-summary endpoint until `isTerminal == true` and status is `COMPLETED`.

### 18.9 Negative / Edge Cases
- Zero-audience campaign launch rejected (remains `DRAFT`).
- Concurrent launch requests handled safely via row-level locking.
- Launching non-DRAFT campaign rejected with `409 Conflict`.

### 18.10 Security Considerations
Launch restricted to authorized roles (`ROLE_ADMIN`, `ROLE_MARKETER`); row-lock prevents race conditions.

### 18.11 Database / API Impact
Campaign launch and delivery query endpoints operational; campaign state machine active.

### 18.12 Git Commit Boundary
Commit: `feat(dispatch): implement atomic campaign launch, delivery tracking APIs, and completion invariant`.

### 18.13 Definition of Done & Exit Criteria
Campaign transitions reliably through `DRAFT` $\to$ `RUNNING` $\to$ `COMPLETED`; zero-audience invariant verified.

### 18.14 Dependencies on Later Milestones
Enables `M10` and `M11`.

---

## 19. M10 — Spring AI Integration

### 19.1 Objective
Integrate Google Gemini via Spring AI across three isolated domains: dynamic natural-language segment rule generation with multi-stage AST validation, campaign message personalization, and narrative performance reporting.

### 19.2 Why This Milestone Exists
Provides enterprise AI-assisted capabilities while strictly enforcing untrusted input containment, data privacy, and core CRM operational independence.

### 19.3 Preconditions & Dependencies
Requires `M4`, `M6`, and `M9`. Valid Gemini API key configured.

### 19.4 Requirements Covered
`FR-AI-SEG-001` through `FR-AI-SEG-004`, `FR-REPORT-003`.

### 19.5 Design Documents Used
`Database-Design.md` §4.8; `API-Design.md` §14.7, §14.8.3, §18; `Security-Async-AI.md` §20, §21, §22, §23, §24, §25, §26.

### 19.6 Main Implementation Areas
- Spring AI Gemini configuration: API key, base client setup, timeout settings.
- Domain 1: Dynamic Segment Rule Generation (`POST /api/v1/ai/segments/generate-rules`):
  - Prompts Gemini with natural language input and schema definition.
  - Parses returned JSON and executes `SegmentRuleValidator` (Section 15.6).
  - Records interaction in `ai_segment_audits` (`user_id`, `prompt_text`, `generated_rules`, `action_taken`, `segment_id`).
  - Returns validated AST to client (zero direct SQL execution).
  - Audit inspection endpoint: `GET /api/v1/ai/segments/audits` (ADMIN only).
- Domain 2: Message Personalization:
  - Injects minimal customer attributes into message template.
  - Implements deferred fallback strategy (`ODD-API-09`): fallback to standard template token substitution on Gemini timeout or error.
- Domain 3: Narrative Performance Reporting (`GET /api/v1/reports/campaigns/{id}/ai-summary`):
  - Queries authoritative numerical metrics from MySQL.
  - Prompts Gemini to generate narrative summary from numerical fact sheet.
  - Persists summary in `campaigns.ai_summary`.
- Fault Isolation: Gemini failure yields `503 Service Unavailable`; core CRM CRUD, segmentation, and campaigns remain fully operational.

### 19.7 Expected Artifacts
- Spring AI client wrappers, prompt templates, AI audit service, AI controllers, `AiSegmentAudit` entity/repo.

### 19.8 Testing Scope
- **Unit Testing:** Prompt construction tests; AI output schema validation tests; fallback execution tests.
- **Integration Testing:** Mock Gemini client tests: assert valid AST returned on success; assert `422` on malformed AST; assert `503` on simulated timeout.
- **Manual Verification:** Submit natural language prompt; inspect returned rule AST; verify audit record created in MySQL `ai_segment_audits`.

### 19.9 Negative / Edge Cases
- Prompt injection attempts, invalid fields in AI output, Gemini API unreachable/timed out, rate limit exhaustion.

### 19.10 Security Considerations
Gemini output is untrusted; customer PII minimized; prompts logged only in compliance table; zero SQL generated.

### 19.11 Database / API Impact
`ai_segment_audits` table active; 2 AI endpoints and 1 AI reporting endpoint operational.

### 19.12 Git Commit Boundary
Commit: `feat(ai): integrate Spring AI Gemini for segment rule generation, audits, personalization, and reporting`.

### 19.13 Definition of Done & Exit Criteria
AI generation produces strictly valid ASTs; audit records persist; core platform survives simulated Gemini outage.

### 19.14 Dependencies on Later Milestones
Enables `M11`.

---

## 20. M11 — Testing + Observability

### 20.1 Objective
Establish comprehensive cross-domain automated test coverage, regression testing, security/concurrency/async/AI failure testing, configure contextual structured logging with MDC across all domains, and conduct operational verification of already-implemented reporting features.

### 20.2 Why This Milestone Exists
Validates system reliability, verifies non-functional requirements, and ensures operational debuggability before containerized packaging.

### 20.3 Preconditions & Dependencies
Requires all functional milestones (`M1` through `M10`).

### 20.4 Requirements Covered
`NFR-PERF-001`, `NFR-SCALE-*`, `NFR-REL-*`, `NFR-MAINT-*`, `FR-REPORT-001`, `FR-REPORT-002`.

### 20.5 Design Documents Used
`API-Design.md` §14.8; `Security-Async-AI.md` §27.

### 20.6 Main Implementation Areas
- Observability: Logback configuration with JSON/structured console output and MDC correlation:
  - `requestId` filter injecting UUID for every HTTP request.
  - `userId`, `campaignId`, `workerId` populated contextually in service and worker threads.
- Reporting Implementation Responsibility Boundary:
  - Reporting Feature Implementation: Implement the approved reporting APIs (`GET /api/v1/reports/campaigns/{id}`, `GET /api/v1/reports/customers/overview`, `GET /api/v1/reports/campaigns/history`, and `GET /api/v1/reports/campaigns/{id}/ai-summary`) when their required transactional data and domain dependencies are available.
  - Dependency Lifecycle: Reporting implementation depends on the relevant campaign, delivery, customer, and AI-summary data being implemented.
  - Invariant Enforcement: Do NOT remove the approved Reporting domain. Do NOT create new reporting APIs. Do NOT create a reporting database; reporting aggregates directly from authoritative MySQL transactional records.
  - M11 Scope Boundary: M11 does NOT serve as a separate business-feature development bucket. M11 focuses on operational verification and automated testing of already-implemented reporting features.
- Cross-Domain Automated Testing:
  - Cross-domain automated testing and regression testing across all services, validators, and mappers.
  - Security, concurrency, async dispatch, and AI failure testing.
  - WebMvc integration tests verifying security, validation, and status codes.
  - DataJpa integration tests verifying constraints, queries, and soft-delete exclusions.
  - End-to-end integration test: upload customers $\to$ create segment $\to$ author campaign $\to$ launch $\to$ await completion $\to$ verify delivery summary and operational reporting verification.

### 20.7 Expected Artifacts
- Logback configuration, MDC correlation filters, cross-domain automated test suite, reporting operational verification tests.

### 20.8 Testing Scope
- **Full Test Suite:** Run `mvn clean test verify`.
- **Manual Verification:** Trigger campaign flow; inspect application log output to confirm consistent `requestId` propagation across API and worker threads.

### 20.9 Negative / Edge Cases
- Verification of exception logging without credential or PII leakage.

### 20.10 Security Considerations
No sensitive tokens, hashes, or PII exposed in MDC or log messages; generic `audit_logs` table strictly excluded.

### 20.11 Database / API Impact
All 36 API endpoints verified; reporting endpoints operational and verified.

### 20.12 Git Commit Boundary
Commit: `test(observability): configure structured MDC logging, verify operational reporting, and execute cross-domain test suite`.

### 20.13 Definition of Done & Exit Criteria
Automated test suite executes cleanly; log correlation verified across asynchronous execution boundaries.

### 20.14 Dependencies on Later Milestones
Enables `M12`.

---

## 21. M12 — Docker + Deployment + Documentation

### 21.1 Objective
Package the platform into production-ready Docker containers, provide multi-service Docker Compose orchestration (App + MySQL + Redis), configure SpringDoc OpenAPI documentation, and finalize operational manuals.

### 21.2 Why This Milestone Exists
Ensures reproducible, turnkey local and staging deployments with complete API documentation for external consumers.

### 21.3 Preconditions & Dependencies
Requires `M11`.

### 21.4 Requirements Covered
`NFR-MAINT-001`, `NFR-MAINT-002`, `NFR-SEC-002`.

### 21.5 Design Documents Used
`System-Architecture.md` §3, `API-Design.md` §21.

### 21.6 Main Implementation Areas
- Dockerfile: Multi-stage build (Maven build stage $\to$ lightweight JRE 21 runtime container) with non-root user execution.
- Docker Compose (`docker-compose.yml`):
  - Service 1: `mysql` (MySQL 8.x, healthcheck, persistent volume, utf8mb4 collation).
  - Service 2: `redis` (approved Redis image, healthcheck, ephemeral data).
  - Service 3: `app` (Spring Boot application container, dependent on mysql and redis health, environment variable bindings).
- API Documentation: Configure SpringDoc OpenAPI 2.x starter (`springdoc-openapi-starter-webmvc-ui`):
  - `@OpenAPIDefinition` and security scheme (`BearerAuth`).
  - Swagger UI accessible at standard UI path.
- Documentation: Finalized `README.md` with setup guide, environment variables, API curl walkthrough, and operational architecture summary.

### 21.7 Expected Artifacts
- `Dockerfile`, `docker-compose.yml`, OpenAPI configuration, updated `README.md`.

### 21.8 Testing Scope
- **Deployment Verification:** Execute `docker compose up --build`; verify all 3 containers reach healthy status.
- **Smoke Testing:** Execute automated smoke test script against containerized instance (login, create customer, launch campaign).
- **Manual Verification:** Open Swagger UI in browser; authenticate via bearer token modal; execute API request.

### 21.9 Negative / Edge Cases
- Container startup order dependencies handled via Docker Compose healthchecks (`service_healthy`).

### 21.10 Security Considerations
Containers run as non-root; database passwords injected via environment; no default credentials committed.

### 21.11 Database / API Impact
Complete platform runnable via single command.

### 21.12 Git Commit Boundary
Commit: `feat(deployment): add multi-stage Dockerfile, Docker Compose orchestration, and OpenAPI documentation`.

### 21.13 Definition of Done & Exit Criteria
Entire platform deploys cleanly via `docker compose up`; Swagger UI functional; smoke test passes.

### 21.14 Dependencies on Later Milestones
Final milestone of Phase 2 / Phase 3 implementation.

---

## 22. Package / Domain Strategy

The codebase preserves strict domain cohesion and layered separation:

```
com.crm.platform
├── auth                  # Authentication, JWT, User entity, Login/User controllers
├── customer              # Customer & Tag entities, Customer CRUD, Repository, Service
├── upload                # Bulk CSV/XLSX streaming parsers, UploadHistory, Ingestion service
├── segment               # Dynamic Rule AST, AST Validator, Criteria Query Compiler, Segment APIs
├── campaign              # Campaign entity, Draft state machine, Campaign Service & Controller
├── delivery              # Redis Stream Producer/Consumer, Delivery Worker, Delivery records
├── ai                    # Spring AI Gemini client, AST translation, Prompt auditing
├── reporting             # Aggregation queries, KPI reports, AI narrative integration
├── common                # Generic response envelopes, Pagination DTOs, Base exceptions
└── config                # Spring Security, Redis, JPA, OpenAPI, Async Executor configurations
```

*Package Boundary Invariant:* Cross-domain dependencies flow in a single direction (e.g., `campaign` depends on `segment` and `customer`; `customer` never depends on `campaign`). Avoid giant unmaintainable `utils/` or `helpers/` dump packages.

---

## 23. Testing Strategy

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ End-to-End Acceptance Tests (Full Multi-domain User Flows via MockMvc / Test Context)  │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ Integration Tests (Spring Data JPA Repositories, Database Constraints, MockMvc APIs)   │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ Component & Service Tests (Business Logic, AST Compilation, State Machine Invariants)   │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ Isolated Unit Tests (Mappers, Validators, DTOs, Password Hashing, Pure Functions)      │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

- **Unit Testing:** Fast, mock-driven (Mockito), testing validation rules, AST structure parsing, and state transitions.
- **Data Integration Testing:** Validates MySQL queries, composite unique constraints, soft-delete filtering, and Criteria API compilation against live database instances.
- **Web Integration Testing:** Validates security filter chains, JWT extraction, RBAC `@PreAuthorize` enforcement, and error payload serialization.
- **Asynchronous Testing:** Awaitility-driven tests asserting background delivery completion and Redis PEL state transitions.

---

## 24. Git / SDLC Workflow

### 24.1 Branching Strategy
- **`main`:** Production-ready baseline. All commits must be tagged and tested.
- **`develop`:** Integration branch for completed milestones.
- **`feature/<name>`:** Bounded milestone implementation branches (e.g., `feature/customer-crud`, `feature/jwt-auth`, `feature/dynamic-segmentation`, `feature/redis-delivery`).

### 24.2 Commit Standards
Conventional Commits format:
```
<type>(<domain>): <imperative summary>

[optional body explaining architectural rationale]
[optional issue reference]
```
*Types:* `feat`, `fix`, `test`, `refactor`, `docs`, `chore`.

---

## 25. Antigravity Development Workflow

To ensure code quality and prevent hallucinated monolithic code dumps, all implementation tasks executed with Antigravity must follow the **Bounded Task Protocol**:

```
[1. Specific Requirement & Baseline Reference]
  - Identify exact SRS ID (e.g., FR-CUST-001) and Design Section.
                    │
                    ▼
[2. Bounded Scope Definition]
  - Explicitly define target files, classes, and expected behavior.
  - NEVER instruct: "Build the entire CRM".
                    │
                    ▼
[3. Architectural Explanation]
  - Explain design rationale, boundaries, and trade-offs before generating code.
                    │
                    ▼
[4. Code Generation & Inspection]
  - Write clean, documented Java/Spring implementation.
  - Inspect generated JPA queries and SQL execution plans.
                    │
                    ▼
[5. Automated Unit & Integration Testing]
  - Write and execute corresponding JUnit 5 / Mockito tests.
                    │
                    ▼
[6. Manual Verification & Git Commit]
  - Verify via API client (cURL/Postman); verify database state; commit to Git.
```

---

## 26. Definition of Done

An implementation task or milestone is considered **DONE** if and only if all of the following criteria are satisfied:
1. **Traceability:** Mapped directly to an approved requirement ID and design specification section.
2. **Implementation Completeness:** All declared classes, methods, and configurations are fully implemented without placeholder stubs.
3. **Validation & Security:** Request inputs validated; RBAC permissions verified at endpoint and service levels.
4. **Testing Coverage:** Happy path, negative edge cases, and validation failures backed by passing unit and integration tests.
5. **Database & API Integrity:** Schema conforms to `Database-Design.md`; endpoints conform to `API-Design.md`.
6. **Observability:** Key operations log contextual messages with MDC correlation IDs.
7. **No Scope Drift:** Zero unapproved technologies, dependencies, tables, or endpoints added.
8. **Committed & Documented:** Changes committed to Git under proper branch; documentation updated.
9. **Explainability:** Developer can articulate *why* the implementation was structured in this manner and what trade-offs were made.

---

## 27. Open Decision Dependencies

The implementation plan acknowledges the following unresolved decisions and defines the corresponding development handling strategy:

| Decision ID | Area | Current Status | Milestone Impact & Development Strategy |
| :--- | :--- | :--- | :--- |
| **`ODD-API-01`** | Idempotency | `[OPEN]` | `M9` | Mutating endpoints rely on MySQL unique constraints; header support isolated in filter if adopted. Does not block core implementation. |
| **`ODD-API-02`** | Launch Status | `[OPEN]` | `M9` | Launch orchestrator isolates zero-audience status code (`400` vs `422`) via constant/configuration. Does not block core launch logic. |
| **`ODD-API-03`** | Error Envelope | `[OPEN]` | `M3` | Global exception handler formats error envelope via dedicated builder component, allowing switch between Custom Envelope and RFC 7807 without service logic changes. |
| **`ODD-API-04`** | Page Sizes | `[OPEN]` | `M2`, `M6` | Default and max page sizes defined as injectable properties in `application.yml` (`crm.pagination.default-size`, `crm.pagination.max-size`). |
| **`ODD-API-05`** | AI Sizing | `[OPEN]` | `M10` | AI rule generation returns strict AST; audience count preview handled via separate `/preview` call. |
| **`ODD-API-06`** | Campaign Delete | `[OPEN]` | `M5` | Campaign delete endpoint implemented to reject deletion of campaigns with delivery records via foreign key RESTRICT. Soft-delete deferred. |
| **`ODD-API-07`** | Upload Errors | `[OPEN]` | `M7` | Response error array truncation cap externalized via configuration (`crm.upload.max-response-errors`). All errors saved to database. |
| **`ODD-API-08`** | AST Limits | `[REQUIRES TESTING]`| `M6` | AST depth and node limits externalized in configuration; benchmarked during performance testing in `M11`. |
| **`ODD-API-09`** | AI Fallback | `[DEFERRED]` | `M10` | Worker implements static template placeholder substitution as default fallback strategy. |
| **`ODD-API-10`** | Upload Limits | `[REQUIRES TESTING]`| `M7` | Max file size and row counts configured via Spring multipart properties; finalized during load testing in `M11`. |
| **`ODD-SEC-01`** | Token Lifetime | `[DECIDED / FROZEN]`| `M4` | Exactly 1 hour / 3,600,000 ms. |
| **`ODD-SEC-03`** | Token Revocation| `[DECIDED / FROZEN]`| `M4` | Handled via live request-time database active status verification (`users.is_active` check); password changes do not revoke tokens (expire in 1h); Redis blacklist strictly barred. |
| **`ODD-SEC-04`** | Password Hash | `[DECIDED / FROZEN]`| `M4` | BCrypt strength 12; plaintext passwords never logged or stored. |
| **`ODD-SEC-05`** | Password Policy | `[DECIDED / FROZEN]`| `M4` | Minimum 8 characters, maximum 72 characters, maximum 72 UTF-8 bytes across user creation and password update. |
| **`ODD-SEC-08`** | Signing Algorithm| `[DECIDED / FROZEN]`| `M4` | HS256 (HMAC-SHA256); mandatory `JWT_SECRET` (>= 32 UTF-8 bytes). |
| **`ODD-ASYNC-01`**| Stream Naming | `[OPEN]` | `M8` | Stream and consumer group names configured via `application.yml` (`crm.async.stream-key`). |
| **`ODD-ASYNC-02`**| Enqueue Gap | `[OPEN / DEFERRED]`| `M9` | Candidate B (background reconciliation scanner) evaluated during async hardening. |
| **`ODD-ASYNC-03`**| Worker Sizing | `[REQUIRES TESTING]`| `M8`, `M9` | Worker thread pool executor parameters externalized; benchmarked in `M11`. |
| **`ODD-AI-02`** | Gemini Model | `[OPEN]` | `M10` | Model identifier (`gemini-1.5-flash`) externalized in Spring AI properties. |
| **`ODD-AI-03`** | AI Timeout | `[REQUIRES TESTING]`| `M10` | Connect and read timeouts configured via Spring AI client properties. |

---

## 28. Risks and Mitigations

| Risk Area | Specific Technical Risk | Architectural Mitigation Strategy |
| :--- | :--- | :--- |
| **Architecture** | Leaking JPA entities into REST controllers | Enforce dedicated DTO mapping layer; entities never cross service boundary. |
| **Architecture** | Scope creep (Kafka, Cache-Aside, microservices) | Strict gatekeeping via Definition of Done; automated code review against prohibitions. |
| **Persistence** | Soft-deleted customers included in segment queries | Mandatory `WHERE deleted_at IS NULL` predicate baked into Criteria API compiler. |
| **Persistence** | Concurrent campaign launch race conditions | Database row-level locking (`SELECT ... FOR UPDATE`) in launch transaction. |
| **Messaging** | Redis Stream memory saturation | Data minimization (IDs only in payload); stream trimming strategy; prompt `XACK`. |
| **Messaging** | Duplicate delivery side-effect execution | Conditional atomic update (`WHERE status = 'PENDING'`); at-least-once accepted. |
| **AI Integration** | Prompt injection altering SQL | Schema-first AST validation; compilation via JPA Criteria API; zero raw SQL. |
| **AI Integration** | Gemini outage blocking core CRM | Strict HTTP client timeouts; fail-fast with `503`; core CRM fully decoupled. |
| **Ingestion** | Heap exhaustion on large CSV/XLSX files | Mandatory streaming parsers (SAX parsing for Excel); chunked batch insertion. |
| **Security** | Over-privileged marketer access | Method-level authorization (`@PreAuthorize`) enforced across service methods. |

---

## 29. Traceability Matrix

| SRS Requirement ID | Requirement Summary | Design Reference | Implementation Milestone | Primary Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **`FR-CUST-001`** | Customer CRUD & Validation | Database §4.2, API §14.2 | `M2`, `M3` | Unit tests, MockMvc CRUD integration tests |
| **`FR-CUST-002`** | Customer Listing & Search | Database §4.2, API §14.2.5 | `M2` | Filtered directory query integration tests |
| **`FR-UPLOAD-001`** | Bulk Upload Support | System §6, API §14.3.1 | `M7` | Multipart upload test with sample CSV/XLSX |
| **`FR-UPLOAD-002`** | Streaming Parse & Batching | System §6, API §17 | `M7` | Heap memory profile during 10k row ingestion |
| **`FR-UPLOAD-004`** | Partial Success Processing | System §6, API §17.2 | `M7` | Ingestion test with corrupt rows asserting `PARTIAL_SUCCESS` |
| **`FR-UPLOAD-006`** | Ingestion Audit History | Database §4.7, API §14.3.2 | `M7` | Database verification of `upload_history` records |
| **`FR-SEG-001`** | Dynamic Segment Definition | Database §4.4, API §14.4 | `M6` | Segment CRUD integration tests |
| **`FR-SEG-002`** | Criteria API Compilation | System §4.2, Security §21 | `M6` | Predicate compilation unit tests, zero raw SQL check |
| **`FR-SEG-003`** | Dynamic Audience Preview | API §14.4.5, §14.4.6 | `M6` | Preview count assertion against live MySQL dataset |
| **`FR-CAMP-001`** | Campaign CRUD & Template | Database §4.5, API §14.5 | `M5` | Campaign authoring integration tests |
| **`FR-CAMP-003`** | Immediate Campaign Launch | API §14.5.6, Security §12 | `M9` | Launch flow integration test |
| **`FR-CAMP-004`** | Zero-Audience Rejection | API §9.1, Security §12 | `M9` | Empty audience launch test asserting campaign remains `DRAFT` |
| **`FR-CAMP-005`** | Campaign Status Tracking | API §14.6.1, §14.6.2 | `M9` | Real-time delivery summary polling test |
| **`FR-DEL-001`** | Async Message Dispatch | System §5.2, Security §11 | `M8` | Redis Stream message enqueue verification |
| **`FR-DEL-002`** | Configurable Delivery Provider | Security §12, §17 | `M8` | Delivery simulation distribution test (~90/10 ratio) and real SMTP delivery tests (GreenMail, MailHog) |
| **`FR-DEL-003`** | Delivery Status Updates | Database §4.6, Security §15 | `M8`, `M9` | MySQL status verification (`SENT`, `FAILED`) |
| **`FR-AI-SEG-001`** | Natural Language Query Input | API §14.7.1, Security §20 | `M10` | AI prompt endpoint integration test |
| **`FR-AI-SEG-002`** | AST JSON Output via Gemini | API §14.7.1, Security §21 | `M10` | AST schema validator integration test |
| **`FR-AI-SEG-003`** | Fallback & Error Handling | System §5.3, Security §25 | `M10` | Gemini outage mock test asserting `503` and CRM survival |
| **`FR-AI-SEG-004`** | AI Generation Auditing | Database §4.8, Security §21.2 | `M10` | Database verification of `ai_segment_audits` rows |
| **`FR-SEC-001`** | Authentication & User Mgmt | Database §4.1, API §14.1 | `M4` | BCrypt verification, login MockMvc tests |
| **`FR-SEC-002`** | Role-Based Access Control | API §13, Security §9 | `M4` | MockMvc 401/403 security constraint tests |
| **`FR-REPORT-001`** | Campaign Performance Report | API §14.8.1, Database §4.6 | Reporting domain implementation at applicable dependency-ready milestone(s); `M11` provides cross-domain verification, regression testing, and observability validation | Metric aggregation assertion tests |
| **`FR-REPORT-003`**| AI Narrative Summary | API §14.8.3, Database §4.5 | `M10` | AI summary persistence in `campaigns.ai_summary` |
| **`NFR-SEC-001`** | Stateless Session Security | Security §6, §7 | `M4` | Verified zero HTTP session state in server |
| **`NFR-DATA-005`** | MySQL Sole Persistent Store | Database §3, Security §11 | `M1`–`M12` | Code audit confirming MySQL sole persistent store |
| **`DBD-18`** | READ COMMITTED Isolation | Database §3, Security §30.1 | `M1`, `M2` | JDBC/JPA connection isolation level verification |

---

## 30. Implementation Exit Criteria

Phase 2 (System Design) formally closes and Phase 3 (Implementation) completes when:
1. All milestones `M0` through `M12` have satisfied their respective Definitions of Done.
2. The automated test suite achieves comprehensive coverage across unit, integration, security, and concurrency paths without failures.
3. The platform deploys cleanly via `docker compose up --build`.
4. Swagger UI accurately documents all 36 endpoints and permits interactive testing.
5. All 5 approved baselines (`SRS`, `System-Architecture`, `Database-Design`, `API-Design`, `Security-Async-AI`) are completely satisfied with zero architectural drift.

---

## 31. Final Scope-Consistency Check

- [x] **Sole persistent store:** MySQL 8.x (InnoDB).
- [x] **Transient async transport only:** Redis Streams.
- [x] **No prohibited technologies:** Zero Kafka, RabbitMQ, PostgreSQL, Resilience4j, Redis Cache-Aside, microservices, 2PC, or generic `audit_logs` table.
- [x] **Delivery semantics:** Explicitly at-least-once; persistent statuses strictly `PENDING`, `SENT`, `FAILED` (no persistent `PROCESSING`).
- [x] **Campaign completion:** Purely conceptual invariant enforced; zero `target_audience_size` column additions.
- [x] **AI trust perimeter:** Untrusted input; zero direct SQL generated; `ai_segment_audits` schema unexpanded; `campaigns.ai_summary` respected.
- [x] **Open decisions:** `ODD-API-*`, `DBD-*`, `ODD-ASYNC-*`, `ODD-AI-*` remain open and unclosed; `ODD-SEC-*` decisions relevant to M4 (`ODD-SEC-01`, `03`, `04`, `05`, `08`) are formally decided/frozen per the approved M4 Security Baseline.
- [x] **Implementation files:** Zero code, entities, DTOs, controllers, migrations, or tests created.

---

## 32. Document Status

- **Status:** **APPROVED BASELINE (M4 SECURITY FROZEN)**
- **SDLC Phase:** Phase 2 — System Design (Implementation Planning)
- **Baseline Freeze Notice:** Formally approved by enterprise architecture review. Serves as the authoritative implementation roadmap for Phase 3 engineering milestones (M1–M12).

---

## 33. Revision History

| Version | Date | Author | Description | Status |
| :--- | :--- | :--- | :--- | :--- |
| 1.0.0 | 2026-09-19 | Senior Enterprise Software Architect & Engineering Lead | Initial Implementation Plan Specification baseline. | Approved |
| 1.1.0 | 2026-09-20 | Senior Enterprise Software Architect & Engineering Lead | Formally froze M4 Security Baseline: Updated Section 13 with exact users schema (including email), mandatory JWT_SECRET (>= 32 bytes), HS256 algorithm, 1h token lifetime, unified login lookup (username OR email in username field, canonical username in sub claim), live database active check and role authority (DB role wins over JWT claim), security error separation (AuthenticationEntryPoint = 401, AccessDeniedHandler = 403, GlobalExceptionHandler), password constraints (8–72 chars / 72 UTF-8 bytes, BCrypt strength 12), initial admin bootstrap semantics (runs only when userRepository.count() == 0; DBA intervention if non-empty with no admin), admin self-protection, and zero regressions on existing 39 test baseline. Closed ODD-SEC-01, 03, 04, 05, 08 in Section 27. | Approved |
| 1.2.0 | 2026-09-23 | Senior Enterprise Software Architect & Engineering Lead | Completed all remaining Backend Milestones (M7 through M12): Streaming CSV/XLSX bulk ingestion, Redis Streams asynchronous campaign delivery worker, atomic campaign launch with pessimistic locking, Spring AI Gemini integration with AST schema validation & auditing, reporting endpoints, request correlation tracing, OpenAPI 3.0 specification, Docker containerization, and full test suite verification (340/340 passing). | Approved |
| 1.3.0 | 2026-09-24 | Senior Enterprise Software Architect & Engineering Lead | Consolidated Backend Hardening Pass: Implemented Transactional Outbox pattern (`campaign_delivery_outbox`) for MySQL ↔ Redis atomicity, Redis Streams Pending Entries List (PEL) stale message recovery, safe stream MINID/MAXLEN trimming, DeliveryProvider abstraction with deterministic idempotency keys, deterministic customer email uniqueness & soft-delete duplicate rejection, chunked large audience materialization (500/page), AI fallback flags & strict 503 unavailability for campaign narrative summaries, asynchronous correlation ID propagation via SLF4J MDC, database query index optimizations, and expanded test suite to 352 passing tests (0 failures, 0 errors, 0 skipped). | Approved |
| 1.4.0 | 2026-09-24 | Senior Enterprise Software Architect & Engineering Lead | Final Complete Backend Hardening: Redis PEL ownership reclaim via `XCLAIM`, safe Min-ID stream trimming based on unacknowledged pending state, `UploadBatchPersister` with isolated `REQUIRES_NEW` transactions to prevent poisoned sessions during batch fallback, deterministic customer email conflict handling, keyset pagination audience materialization (`WHERE id > :lastId ORDER BY id ASC LIMIT 500`), correlation ID propagation across HTTP/Outbox/Redis/Worker MDC with guaranteed `finally` cleanup, strict AI summary contracts (no fabricated summaries on Gemini failure), and expanded test suite to 375 passing tests (0 failures, 0 errors, 0 skipped). | Approved |
