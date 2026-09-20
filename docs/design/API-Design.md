# API Design Document
## Enterprise AI-CRM Platform (`CS-CRM-2026`)

---

### Document Metadata
- **Project Code:** CS-CRM-2026
- **System Name:** Enterprise AI-CRM Platform
- **Document Version:** 1.0.1
- **SDLC Phase:** Phase 2 — System Design (API Design Specification)
- **Status:** APPROVED BASELINE (M4 SECURITY FROZEN)
- **Author:** Senior Enterprise Software Architect & API Design Team
- **Date:** 2026-09-20
- **Primary Source of Truth:** [Software Requirements Specification (docs/SRS.md)](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/SRS.md) v1.0.1
- **Architectural Reference:** [System Architecture Document (docs/design/System-Architecture.md)](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/System-Architecture.md) v1.0.0
- **Database Reference:** [Database Design Document (docs/design/Database-Design.md)](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/Database-Design.md) v1.0.1
- **Approved Baseline Chain:** SRS v1.0.1 → System Architecture v1.0.0 → Database Design v1.0.1 → API Design v1.0.1 → Security + Async + AI Design v1.1.0
- **Target Audience:** Backend Engineers, Frontend/API Consumers, QA Engineers, Security Engineers, Enterprise Architects

---

## 1. Document Purpose

This document provides the formal, implementation-ready **API Design Specification** for the **Enterprise AI-CRM Platform (`CS-CRM-2026`)**. It defines the RESTful HTTP interface contracts, request/response payload structures, validation policies, authentication/authorization requirements, asynchronous interaction patterns, and error handling behaviors across all platform capabilities.

As part of a strict, learning-first enterprise SDLC, this document bridges the functional and non-functional requirements established in `docs/SRS.md` (v1.0.1) and the technical architecture defined in `docs/design/System-Architecture.md` (v1.0.0) and `docs/design/Database-Design.md` (v1.0.0). It translates architectural principles into explicit endpoint specifications while maintaining rigorous traceability and formal governance over open and deferred decisions.

---

## 2. Scope

### 2.1 In Scope
- **Interface Definitions:** All HTTP REST endpoints across the eight canonical platform domains:
  1. Authentication (`/api/v1/auth`, `/api/v1/users`)
  2. Customer (`/api/v1/customers`)
  3. Upload (`/api/v1/uploads`)
  4. Segment (`/api/v1/segments`)
  5. Campaign (`/api/v1/campaigns`)
  6. Delivery (`/api/v1/campaigns/{id}/deliveries`, `/api/v1/campaigns/{id}/delivery-summary`)
  7. AI (`/api/v1/ai/segments`)
  8. Reporting (`/api/v1/reports`)
- **Protocol & Serialization:** HTTP/1.1 (TLS/HTTPS in deployed environments), JSON payload serialization, and `multipart/form-data` for bulk file uploads.
- **Contract Specifications:** Request bodies, query/path parameters, response envelopes, HTTP status code semantics, and validation rules.
- **Security Contracts:** Spring Security 6.x JWT stateless authentication, role-based access control (`ROLE_ADMIN`, `ROLE_MARKETER`), and sensitive parameter handling.
- **Integration Semantics:** Synchronous request/response cycles, asynchronous campaign launch/dispatch triggers, streaming bulk ingestion, and external AI provider isolation.
- **Decision Governance:** Explicit documentation of DECIDED, RECOMMENDED, OPEN, DEFERRED, REQUIRES TESTING, and OUT OF SCOPE architectural decisions.

### 2.2 Out of Scope
- **Source Code Implementation:** Java classes, Spring `@RestController` definitions, `@Service` logic, DTO Java classes, JPA entities, and Spring Data repositories.
- **Persistence DDL/Migrations:** SQL schema definitions, Liquibase/Flyway change logs, and index creation scripts (governed by `docs/design/Database-Design.md`).
- **Excluded Technologies:** PostgreSQL, MongoDB, Kafka, RabbitMQ, Redis caching (Cache-Aside), and Resilience4j are strictly excluded from the v1 architecture.
- **Generic Audit Table:** A generic relational `audit_logs` table is strictly `[OUT OF SCOPE]` (confirmed by DBD-16). Operational auditing is handled via SLF4J/MDC structured logging.
- **Worker Internal Mechanics:** Redis Stream consumer group internals, XREADGROUP/XACK loops, and thread pool executor tuning (governed by Async/Delivery Runtime Design).

---

## 3. References and Baselines

This specification is directly governed by and strictly subordinate to the following approved project baselines:

1. **[docs/SRS.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/SRS.md) (Version 1.0.1 — Approved Baseline):**
   - Functional requirements: `FR-CUST-*`, `FR-UPLOAD-*`, `FR-SEG-*`, `FR-AI-*`, `FR-CAMP-*`, `FR-DEL-*`, `FR-SEC-*`, `FR-REPORT-*`.
   - Non-functional requirements: `NFR-PERF-*`, `NFR-SCALE-*`, `NFR-SEC-*`, `NFR-REL-*`, `NFR-MAINT-*`, `NFR-DATA-*`.
2. **[docs/design/System-Architecture.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/System-Architecture.md) (Version 1.0.0 — Approved Baseline):**
   - Layered architectural boundaries, DTO/Entity isolation, stateless Spring Security, asynchronous Redis Stream dispatch, dynamic Criteria API compilation, and external AI isolation.
3. **[docs/design/Database-Design.md](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/Database-Design.md) (Version 1.0.0 — Approved Baseline):**
   - 8 canonical relational tables (`users`, `customers`, `customer_tags`, `segments`, `campaigns`, `campaign_delivery_records`, `upload_history`, `ai_segment_audits`).
   - Soft-delete semantics (`deleted_at`), `READ COMMITTED` isolation, database constraints, and persistent state invariants.
4. **API Design Step 1 Analysis & Surgical Correction Pass:**
   - Evaluated candidate endpoint inventory, zero-audience campaign launch invariants, error envelope alternatives, pagination governance, and traceability classification.

### Hierarchy of Authority
```
docs/SRS.md (v1.0.1) 
       └── docs/design/System-Architecture.md (v1.0.0) 
                └── docs/design/Database-Design.md (v1.0.0) 
                         └── docs/design/API-Design.md (v1.0.0)
```
*Rule:* Lower-level design artifacts must never silently override or contradict higher-level approved baselines. Any unresolved design choice is explicitly classified as an Open or Deferred Decision.

---

## 4. API Design Principles

The API layer is designed in accordance with enterprise-grade REST architecture principles:

1. **RESTful Resource Modeling:** URIs identify nouns representing domain resources (e.g., `/customers`, `/segments`, `/campaigns`). HTTP verbs (`GET`, `POST`, `PATCH`, `DELETE`) express operations against those resources.
2. **Stateless Request Execution:** The server retains zero client session state between requests. Every HTTP request carries all context necessary for authentication, authorization, and execution via cryptographically signed JWT bearer tokens (`NFR-SEC-001`).
3. **Strict Layer Decoupling (DTO vs. Entity):** Persistence entities (JPA) are never exposed through the API. Dedicated Request and Response Data Transfer Objects (DTOs) enforce contract immutability, data hiding, and independent schema evolution.
4. **Authoritative Persistence (MySQL 8.x):** MySQL is the sole persistent source of truth. The API layer reads and writes authoritative state exclusively through relational transactions.
5. **Transient Asynchronous Transport (Redis 7.x):** Redis is utilized strictly as transient messaging infrastructure for background delivery execution. Redis contains zero persistent business state and no API-level cache-aside layers.
6. **External AI Isolation:** AI integration (Google Gemini via Spring AI) is strictly encapsulated within dedicated endpoints (`/api/v1/ai/*`). Core CRM functionality (CRUD, segmentation, campaign creation, delivery tracking) remains fully operational even during complete external AI provider outages.
7. **At-Least-Once Asynchronous Delivery Semantics:** Campaign launch triggers asynchronous dispatch. Database constraints (`UNIQUE(campaign_id, customer_id)`) guarantee persistent delivery record deduplication, but the system does not claim end-to-end exactly-once external message delivery.
8. **Explicit Zero-Audience Rejection:** A campaign evaluated against an audience of zero customers is rejected at launch. The campaign remains in `DRAFT` status and never transitions to `RUNNING`.
9. **Conservative Scope Governance:** No external messaging systems (Kafka/RabbitMQ), unapproved databases (PostgreSQL), resilience libraries (Resilience4j), or generic audit log tables are introduced.

---

## 5. API Base Path and Versioning

### 5.1 Base Path
All platform REST APIs are anchored under the uniform base path:
```
/api/v1/
```

### 5.2 Versioning Strategy
- **URI Path Versioning:** Major API versions are encoded directly in the URI path (`/v1/`). This guarantees transparent routing, simple gateway filtering, and unambiguous client targeting.
- **Backward Compatibility:** Additive changes (e.g., optional request fields, new response fields) within major version 1 will not increment the version identifier.
- **Breaking Changes:** Any breaking change (e.g., removing fields, renaming endpoints, altering data types) mandates a new version path (e.g., `/api/v2/`).

---

## 6. API Domain Organization

The platform API is organized into exactly **eight canonical business domains**, mapped directly to business functional areas and underlying persistence aggregates:

```
                      ┌─────────────────────────────────┐
                      │    API Base: /api/v1/           │
                      └────────────────┬────────────────┘
                                       │
      ┌──────────────┬─────────────────┼─────────────────┬──────────────┐
      │              │                 │                 │              │
┌─────▼─────┐  ┌─────▼─────┐     ┌─────▼─────┐     ┌─────▼─────┐  ┌─────▼─────┐
│ 1. Auth   │  │2. Customer│     │ 3. Upload │     │4. Segment │  │5. Campaign│
│  & Users  │  │ Management│     │ Ingestion │     │ Definition│  │ Execution │
└─────┬─────┘  └─────┬─────┘     └─────┬─────┘     └─────┬─────┘  └─────┬─────┘
      │              │                 │                 │              │
      └──────────────┼─────────────────┼─────────────────┼──────────────┘
                     │                 │                 │
               ┌─────▼─────┐     ┌─────▼─────┐     ┌─────▼─────┐
               │6. Delivery│     │   7. AI   │     │8.Reports &│
               │  Tracking │     │ Assistant │     │ Analytics │
               └───────────┘     └───────────┘     └───────────┘
```

1. **Authentication Domain (`auth` / `users`):** Authentication token issuance, user administration, credential management, and role assignment.
2. **Customer Domain (`customers`):** Customer profile management, spending/order metrics, tagging, and filtered directory retrieval.
3. **Upload Domain (`uploads`):** High-throughput bulk data ingestion (CSV/XLSX), row-level validation, and ingestion audit history.
4. **Segment Domain (`segments`):** Dynamic audience definition via Boolean AST rule trees, audience preview, and membership resolution.
5. **Campaign Domain (`campaigns`):** Marketing campaign authoring, message templating, lifecycle management, and launch execution.
6. **Delivery Domain (`deliveries`):** Asynchronous dispatch status monitoring, per-customer delivery logs, and delivery metrics aggregation.
7. **AI Domain (`ai`):** Natural-language-to-AST translation via Google Gemini, AI response validation, and AI generation audit trail.
8. **Reporting Domain (`reports`):** Business analytics, campaign performance summaries, customer directory aggregations, and AI operational summaries.

---

## 7. Endpoint Inventory

The platform provides an inventory of **36 endpoints across the eight canonical API domains**. Every endpoint is classified by its architectural origin:
- `[EXPLICIT SRS]`: Capability and interaction explicitly mandated by `docs/SRS.md`.
- `[CHOSEN REPRESENTATION]`: Capability mandated by SRS, with the specific URI, method, or payload representation selected during API design.
- `[DESIGN DECISION]`: Architectural design choice introduced to support system operations, security, or data integrity.

| Domain | HTTP Method | Endpoint URI | Description | Classification |
| :--- | :--- | :--- | :--- | :--- |
| **Authentication** | `POST` | `/api/v1/auth/login` | Authenticate credentials and issue JWT bearer token | `[EXPLICIT SRS]` |
| | `POST` | `/api/v1/users` | Create a new administrative or marketer user account | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/users` | Retrieve paginated list of system users | `[CHOSEN REPRESENTATION]` |
| | `GET` | `/api/v1/users/{id}` | Retrieve specific user profile details | `[CHOSEN REPRESENTATION]` |
| | `PATCH` | `/api/v1/users/{id}/role` | Update user role (`ROLE_ADMIN` vs `ROLE_MARKETER`) | `[EXPLICIT SRS]` |
| | `PATCH` | `/api/v1/users/{id}/deactivate` | Deactivate user account (revoke platform access) | `[EXPLICIT SRS]` |
| | `PATCH` | `/api/v1/users/{id}/password` | Update user password credential | `[DESIGN DECISION]` |
| **Customer** | `POST` | `/api/v1/customers` | Create a single customer record | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/customers/{id}` | Retrieve customer profile by ID | `[EXPLICIT SRS]` |
| | `PATCH` | `/api/v1/customers/{id}` | Update customer profile details | `[EXPLICIT SRS]` |
| | `DELETE` | `/api/v1/customers/{id}` | Soft-delete a customer record (`deleted_at`) | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/customers` | Retrieve paginated, filtered customer directory | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/customers/count` | Retrieve total count of active customers | `[CHOSEN REPRESENTATION]` |
| **Upload** | `POST` | `/api/v1/uploads/bulk` | Ingest bulk customer data via multipart CSV/XLSX | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/uploads/history` | Retrieve paginated bulk upload audit history | `[EXPLICIT SRS]` |
| **Segment** | `POST` | `/api/v1/segments` | Create dynamic audience segment with rule tree | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/segments/{id}` | Retrieve segment definition and rule tree | `[EXPLICIT SRS]` |
| | `PATCH` | `/api/v1/segments/{id}` | Update segment definition or rule tree | `[EXPLICIT SRS]` |
| | `DELETE` | `/api/v1/segments/{id}` | Delete segment (subject to campaign association checks) | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/segments` | Retrieve paginated list of segment definitions | `[EXPLICIT SRS]` |
| | `POST` | `/api/v1/segments/{id}/preview` | Evaluate segment audience size against live MySQL data | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/segments/{id}/members` | Retrieve paginated member list matching segment rules | `[EXPLICIT SRS]` |
| **Campaign** | `POST` | `/api/v1/campaigns` | Create new campaign in `DRAFT` status | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/campaigns/{id}` | Retrieve campaign details, template, and status | `[EXPLICIT SRS]` |
| | `PATCH` | `/api/v1/campaigns/{id}` | Update `DRAFT` campaign properties | `[EXPLICIT SRS]` |
| | `DELETE` | `/api/v1/campaigns/{id}` | Delete campaign record | `[CHOSEN REPRESENTATION]` |
| | `GET` | `/api/v1/campaigns` | Retrieve paginated list of campaigns | `[EXPLICIT SRS]` |
| | `POST` | `/api/v1/campaigns/{id}/launch` | Launch campaign (evaluates audience, transitions state) | `[EXPLICIT SRS]` |
| **Delivery** | `GET` | `/api/v1/campaigns/{id}/delivery-summary` | Retrieve aggregated delivery metrics for a campaign | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/campaigns/{id}/deliveries` | Retrieve paginated individual customer delivery logs | `[EXPLICIT SRS]` |
| **AI** | `POST` | `/api/v1/ai/segments/generate-rules` | Translate natural language requirement into AST rules | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/ai/segments/audits` | Retrieve paginated AI prompt/response audit records | `[EXPLICIT SRS]` |
| **Reporting** | `GET` | `/api/v1/reports/campaigns/{id}` | Retrieve comprehensive campaign performance report | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/reports/customers/overview` | Retrieve high-level customer demographic & spend KPIs | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/reports/campaigns/{id}/ai-summary` | Retrieve AI-generated narrative campaign summary | `[EXPLICIT SRS]` |
| | `GET` | `/api/v1/reports/campaigns/history` | Retrieve historical campaign metrics across all campaigns | `[CHOSEN REPRESENTATION]` |

---

## 8. Request and Response Contract Design

### 8.1 Request Contract Principles
- **JSON Serialization:** All request payloads must be encoded as UTF-8 JSON (`Content-Type: application/json`), except multipart uploads (`multipart/form-data`).
- **Strict DTO Binding:** Requests are deserialized into strongly typed Java DTOs with Jakarta Bean Validation annotations (`@NotNull`, `@Size`, `@Pattern`, `@Min`, `@DecimalMin`).
- **Unknown Field Rejection:** The JSON parser is configured to reject unmapped fields (`FAIL_ON_UNKNOWN_PROPERTIES = true`) to prevent accidental schema drift or parameter injection.

### 8.2 Recommended Success Response Contract
To provide a consistent client integration experience across all domains, the platform adopts a standardized generic success envelope:

```json
{
  "success": true,
  "data": { ... },
  "metadata": {
    "timestamp": "2026-09-19T10:15:30.123456Z",
    "requestId": "c4a7e8b2-3f1d-4e9a-8b1c-7d6e5f4a3b2c"
  }
}
```

For paginated collections, the `metadata` object is extended with page navigation context:
```json
{
  "success": true,
  "data": [ ... ],
  "metadata": {
    "timestamp": "2026-09-19T10:15:30.123456Z",
    "requestId": "c4a7e8b2-3f1d-4e9a-8b1c-7d6e5f4a3b2c",
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 1542,
      "totalPages": 78,
      "isFirst": true,
      "isLast": false
    }
  }
}
```

*Architectural Rationale:* A standardized success envelope simplifies client-side state handling, encapsulates pagination metadata uniformly, and correlates client interactions with backend request tracking (`requestId` mapped to MDC logging).

---

## 9. HTTP Status Code Policy

The platform adheres to semantic HTTP status codes reflecting the outcome of each operation:

| HTTP Status | Semantic Meaning | Application Usage in CS-CRM-2026 |
| :--- | :--- | :--- |
| **`200 OK`** | Standard successful response | Successful `GET`, `PATCH`, or non-resource-creation `POST` (e.g., login, preview, launch initiation). |
| **`201 Created`** | Resource successfully created | Successful resource creation (`POST /customers`, `/segments`, `/campaigns`, `/users`). Returns `Location` header. |
| **`204 No Content`** | Operation succeeded; no body | Successful deletion (`DELETE /customers/{id}`, `/segments/{id}`). |
| **`400 Bad Request`** | Syntactic or semantic client error | Malformed JSON, missing required headers, illegal argument, invalid query parameters, or controller-level validation failures. |
| **`401 Unauthorized`** | Authentication failure | Missing, expired, or cryptographically invalid JWT bearer token, or account deactivated (`users.is_active == false`). Emitted via Spring Security `AuthenticationEntryPoint`. |
| **`403 Forbidden`** | Authorization failure | Authenticated caller lacks required role authority (e.g., Marketer accessing Admin-only endpoint). Emitted via Spring Security `AccessDeniedHandler`. |
| **`404 Not Found`** | Resource does not exist | Targeted ID does not exist, or targeted customer has been soft-deleted (`deleted_at IS NOT NULL`). |
| **`409 Conflict`** | State conflict / unique violation | Email duplicate violation on customer create/update, or state transition violation (e.g., launching non-DRAFT campaign). |
| **`415 Unsupported Media Type`** | Invalid Content-Type header | Request payload is not `application/json` or `multipart/form-data`. |
| **`422 Unprocessable Entity`** | Well-formed JSON fails domain rules | Syntactically valid JSON failing business invariants (e.g., segment AST references unsupported operator). |
| **`500 Internal Server Error`** | Unhandled internal failure | Unexpected server-side bug or unhandled persistence fault. Masks internal stack traces. |
| **`503 Service Unavailable`** | External dependency unavailable | Google Gemini AI API unreachable or rate-limited; transient Redis transport unavailability. |

### 9.1 Zero-Audience Campaign Launch Status Policy
- **DECIDED Behavior:** When a campaign launch is requested, the system evaluates the bound segment against live MySQL data. If the audience evaluates to exactly zero (`audienceSize == 0`), the launch request **MUST BE REJECTED**. The campaign remains in `DRAFT` status and **MUST NOT** transition to `RUNNING`.
- **OPEN DESIGN DECISION (`ODD-API-02`):** The exact HTTP status code returned for zero-audience rejection is currently open between:
  - `400 Bad Request`: Treats launching an empty campaign as an illegal client command.
  - `422 Unprocessable Entity`: Treats the campaign as structurally valid but semantically unexecutable due to business data constraints.

### 9.2 Security Error Handling Architecture
Security error responses follow a strict separation of concerns between servlet filter chain handlers and the controller advice layer:
1. **`AuthenticationEntryPoint` (`401 Unauthorized`):** Invoked directly by the filter chain for unauthenticated access, invalid/expired tokens, or deactivated accounts.
2. **`AccessDeniedHandler` (`403 Forbidden`):** Invoked directly by the filter chain for unauthorized role access.
3. **`GlobalExceptionHandler` (`@RestControllerAdvice`):** Handles application-layer exceptions (`MethodArgumentNotValidException`, domain validation, resource not found).

---

## 10. Validation Strategy

Validation is enforced across three conceptual layers to ensure defense-in-depth:

```
┌─────────────────────────────────────────────────────────────────┐
│ Layer 1: API / Request Validation (Jakarta Validation)           │
│ - Format checks (@NotNull, @Email, @Size, @Pattern)             │
│ - JSON structure and type conformity                            │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│ Layer 2: Domain / Business Validation (Service Layer)            │
│ - Segment AST field/operator/type compatibility                 │
│ - Campaign state transition legality (DRAFT -> RUNNING only)    │
│ - Segment association checks prior to segment deletion          │
│ - Audience size non-zero verification                           │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│ Layer 3: Database Constraint Enforcement (MySQL InnoDB)         │
│ - Unique constraints (customers.email, campaigns.name)          │
│ - Foreign key integrity (RESTRICT on segment deletion)          │
│ - Column nullability and length boundaries                      │
└─────────────────────────────────────────────────────────────────┘
```

### 10.1 Dynamic Segment AST Validation
Dynamic audience segmentation rules are submitted as Abstract Syntax Tree (AST) JSON payloads. The service layer validates incoming ASTs against the following strict constraints:
1. **Allowed Fields:** Rule conditions may only reference approved customer attributes:
   - `totalSpend` (Numeric)
   - `orderCount` (Integer)
   - `lastOrderDate` (Date/Timestamp)
   - `location` (String)
   - `tags` (String collection)
2. **Allowed Operators:** Operators must match the data type:
   - Numeric/Date: `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`, `BETWEEN`.
   - String: `EQUALS`, `NOT_EQUALS`, `CONTAINS`, `STARTS_WITH`, `IN`.
   - Collection (`tags`): `CONTAINS`, `DOES_NOT_CONTAIN`.
3. **Logical Combinators:** Only `AND` and `OR` are permitted at group nodes. Negation is expressed via leaf-level `NOT_EQUALS` or `DOES_NOT_CONTAIN`.
4. **Structural Validity:**
   - Every condition group must contain at least one child node.
   - Values must be safely coercible to the target field's database type.
5. **No Direct SQL Execution:** The validated AST is compiled by the application into a parameterized JPA Criteria API query. The AST is **never** concatenated into raw SQL, completely eliminating SQL injection vectors.
6. **Complexity Limits (`ODD-API-08`):** Maximum AST nesting depth and maximum leaf node counts are marked as **`[DEFERRED / REQUIRES TESTING]`** and will be finalized after performance benchmarking.

---

## 11. Error Handling Contract

### 11.1 Architectural Status (`ODD-API-03`)
The exact error response envelope representation remains an **`[OPEN DESIGN DECISION]`**. Two enterprise patterns are under consideration:

#### Option A: Custom Standardized Error Envelope
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "The request payload contains 2 validation error(s).",
    "timestamp": "2026-09-19T10:15:30.123456Z",
    "requestId": "c4a7e8b2-3f1d-4e9a-8b1c-7d6e5f4a3b2c",
    "details": [
      {
        "field": "email",
        "rejectedValue": "invalid-email",
        "message": "Must be a well-formed email address."
      },
      {
        "field": "totalSpend",
        "rejectedValue": -10.50,
        "message": "Total spend must be greater than or equal to 0.00."
      }
    ]
  }
}
```

#### Option B: RFC 7807 Problem Details for HTTP APIs
```json
{
  "type": "https://api.cs-crm-2026.internal/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "The request payload contains 2 validation error(s).",
  "instance": "/api/v1/customers",
  "timestamp": "2026-09-19T10:15:30.123456Z",
  "requestId": "c4a7e8b2-3f1d-4e9a-8b1c-7d6e5f4a3b2c",
  "invalidParams": [
    {
      "name": "email",
      "reason": "Must be a well-formed email address."
    }
  ]
}
```

*Trade-off Analysis:* Option A mirrors the success envelope structure (`success: false`), providing symmetric parsing for client applications. Option B aligns with IETF RFC 7807 standards, simplifying integration with API gateways and third-party API clients. This decision will be finalized prior to implementation freeze.

### 11.2 Milestone 3 (M3) Baseline Implementation
During **M2**, an initial implementation of Option A was introduced as prerequisite scaffolding to enable customer CRUD operations. In **Milestone 3 (M3)**, this error envelope and validation layer were formalized and hardened as the platform-level baseline:

1. **Standardized Error Codes & HTTP Status Mapping:**
   - `VALIDATION_FAILED` (`400 Bad Request`): Bean validation failures (`MethodArgumentNotValidException`, `ConstraintViolationException`) with field-level details identifying the rejected attribute and message.
   - `MALFORMED_REQUEST` (`400 Bad Request`): Syntactically broken or unreadable JSON payloads (`HttpMessageNotReadableException`).
   - `INVALID_PARAMETER` (`400 Bad Request`): URI path variable or request parameter type mismatch (`MethodArgumentTypeMismatchException`).
   - `BAD_REQUEST` (`400 Bad Request`): General invalid requests or unapproved sort fields (`InvalidRequestException`).
   - `RESOURCE_NOT_FOUND` (`404 Not Found`): Entity lookup failures (`ResourceNotFoundException`) or unmapped endpoints (`NoResourceFoundException`).
   - `METHOD_NOT_ALLOWED` (`405 Method Not Allowed`): Unsupported HTTP verbs (`HttpRequestMethodNotSupportedException`).
   - `DUPLICATE_RESOURCE` (`409 Conflict`): Unique constraint violations or active duplicate email (`DuplicateResourceException`, `DataIntegrityViolationException`).
   - `UNSUPPORTED_MEDIA_TYPE` (`415 Unsupported Media Type`): Non-JSON / unsupported content types (`HttpMediaTypeNotSupportedException`).
   - `INTERNAL_SERVER_ERROR` (`500 Internal Server Error`): Catch-all fallback masking internal errors and stack traces (`Exception`).

2. **Error Sanitization Invariant:**
   The error handling layer (`@RestControllerAdvice`) strictly enforces that internal database exceptions (e.g., MySQL syntax errors, constraint names, stack traces) are masked from external API responses. Sensitive details are logged internally at ERROR level with an associated `requestId`.

3. **Deferred / Future Scope:**
   - Multi-language localization (i18n) of validation messages is deferred to future platform milestones; standard English messages are enforced in v1.
   - RFC 7807 problem details adapter support remains an open design option for future API gateway integration.

---

## 12. Pagination, Filtering, Sorting, and Query Parameters

### 12.1 Pagination Strategy
All collection endpoints (`GET /customers`, `GET /campaigns`, `GET /segments`, `GET /users`, `GET /uploads/history`, `GET /campaigns/{id}/deliveries`, `GET /ai/segments/audits`) enforce **bounded pagination** to prevent memory exhaustion and denial-of-service conditions:

- `page`: Zero-indexed page number (default: `0`).
- `size`: Number of records per page.
- `sort`: Comma-separated sort expressions (e.g., `sort=createdAt,desc&sort=name,asc`).

### 12.2 Bounded Page Size Governance (`ODD-API-04`)
- **APPROVED PRINCIPLE:** All collection queries must be bounded. Unbounded collection retrieval is strictly prohibited.
- **OPEN DESIGN DECISION:** The exact numeric defaults and upper bounds (e.g., default 20, max 100) are **`[OPEN DESIGN DECISION]`** pending empirical query latency benchmarking against populated database test fixtures.

### 12.3 Filtering and Sorting Conventions
Filtering is expressed via explicit, strongly typed query parameters corresponding to index-backed entity attributes:
- Customer Filtering: `?minSpend=100.00&maxSpend=500.00&location=Delhi&tags=VIP,Retail`
- Campaign Filtering: `?status=RUNNING&segmentId=42`
- Delivery Filtering: `?status=FAILED`

---

## 13. Authentication and Authorization

### 13.1 Authentication Architecture
Authentication is implemented using **Spring Security 6.x** with stateless **JWT Bearer Tokens** (`NFR-SEC-001`):
1. The client submits credentials to `POST /api/v1/auth/login`.
2. The authentication manager verifies credentials against the `users` table via BCrypt password hashing.
3. Upon success, the server issues a cryptographically signed HMAC-SHA256 JWT.
4. Subsequent requests pass the token via the standard HTTP header:
   ```
   Authorization: Bearer <jwt-token>
   ```

### 13.2 Role-Based Access Control (RBAC)
The system defines exactly **two canonical roles** (`docs/SRS.md` §3.1):
- **`ROLE_ADMIN`:** Full administrative superset privilege across all endpoints and administrative operations.
- **`ROLE_MARKETER`:** Business operational privilege covering customer management, segment authoring, campaign execution, and reporting.

### 13.3 Role-to-Endpoint Authorization Matrix

| Domain | Endpoint URI | Method | Permitted Roles | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `/api/v1/auth/login` | `POST` | `Anonymous` (PermitAll) | Public authentication endpoint |
| | `/api/v1/users` | `POST` | `ROLE_ADMIN` | User provisioning |
| | `/api/v1/users` | `GET` | `ROLE_ADMIN` | User listing |
| | `/api/v1/users/{id}` | `GET` | `ROLE_ADMIN` | User inspection |
| | `/api/v1/users/{id}/role` | `PATCH` | `ROLE_ADMIN` | Role elevation / demotion |
| | `/api/v1/users/{id}/deactivate`| `PATCH` | `ROLE_ADMIN` | Account deactivation |
| | `/api/v1/users/{id}/password`| `PATCH` | `ROLE_ADMIN` | Administrative password reset |
| **Customer**| `/api/v1/customers` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Customer creation |
| | `/api/v1/customers/{id}` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Customer lookup |
| | `/api/v1/customers/{id}` | `PATCH` | `ROLE_ADMIN`, `ROLE_MARKETER` | Customer update |
| | `/api/v1/customers/{id}` | `DELETE` | `ROLE_ADMIN` | Soft-delete restricted to Admin |
| | `/api/v1/customers` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Customer search & directory |
| | `/api/v1/customers/count` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Customer aggregate count |
| **Upload** | `/api/v1/uploads/bulk` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Ingest CSV/XLSX |
| | `/api/v1/uploads/history` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Audit history of ingestion |
| **Segment** | `/api/v1/segments` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Create segment |
| | `/api/v1/segments/{id}` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Inspect segment & rules |
| | `/api/v1/segments/{id}` | `PATCH` | `ROLE_ADMIN`, `ROLE_MARKETER` | Update segment rules |
| | `/api/v1/segments/{id}` | `DELETE` | `ROLE_ADMIN`, `ROLE_MARKETER` | Delete segment |
| | `/api/v1/segments` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | List segments |
| | `/api/v1/segments/{id}/preview`| `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Dynamic count evaluation |
| | `/api/v1/segments/{id}/members`| `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Paginated member preview |
| **Campaign**| `/api/v1/campaigns` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Create campaign |
| | `/api/v1/campaigns/{id}` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Campaign status/details |
| | `/api/v1/campaigns/{id}` | `PATCH` | `ROLE_ADMIN`, `ROLE_MARKETER` | Update draft campaign |
| | `/api/v1/campaigns/{id}` | `DELETE` | `ROLE_ADMIN` | Campaign deletion (Admin only)|
| | `/api/v1/campaigns` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | List campaigns |
| | `/api/v1/campaigns/{id}/launch`| `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Launch campaign |
| **Delivery**| `/api/v1/campaigns/{id}/delivery-summary` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Delivery metrics |
| | `/api/v1/campaigns/{id}/deliveries` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Individual delivery logs |
| **AI** | `/api/v1/ai/segments/generate-rules` | `POST` | `ROLE_ADMIN`, `ROLE_MARKETER` | Natural language to AST |
| | `/api/v1/ai/segments/audits` | `GET` | `ROLE_ADMIN` | Audit log inspection (Admin) |
| **Reports** | `/api/v1/reports/campaigns/{id}` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Campaign performance |
| | `/api/v1/reports/customers/overview` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Demographic KPIs |
| | `/api/v1/reports/campaigns/{id}/ai-summary` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Narrative campaign report |
| | `/api/v1/reports/campaigns/history` | `GET` | `ROLE_ADMIN`, `ROLE_MARKETER` | Historical campaign summary |

---

## 14. Domain API Specifications

---

### 14.1 Authentication APIs

#### 14.1.1 POST /api/v1/auth/login
- **Purpose:** Authenticate user credentials and issue a signed JWT bearer token.
- **SRS Traceability:** `FR-SEC-001`, `FR-SEC-002`, `NFR-SEC-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** None (`PermitAll`).
- **Request Headers:** `Content-Type: application/json`.
- **Request Body:**
  ```json
  {
    "username": "admin@crm.internal",
    "password": "Password123!"
  }
  ```
- **Login Identifier Semantics:**
  - The request JSON field is named `"username"`.
  - That field accepts either the user's canonical `username` OR their `email`.
  - Backend performs unified lookup (`findByUsernameOrEmail`).
  - The issued JWT `sub` claim is **ALWAYS** the canonical `username`.
- **Validation Rules:**
  - `username`: Required, non-blank string (accepts canonical username or email).
  - `password`: Required, non-blank string.
- **Success Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "user": {
        "id": 1,
        "username": "admin",
        "role": "ROLE_ADMIN"
      }
    }
  }
  ```
- **Token Invariants:**
  - Algorithm: `HS256` (HMAC-SHA256).
  - Lifetime: Exactly 1 hour (3,600,000 ms).
  - Issuer: `cs-crm-2026`.
  - Claims: `sub` (canonical username), `uid` (user ID), `role` (diagnostic claim), `iss`, `iat`, `exp`, `jti`.
  - Refresh tokens are **NOT** implemented in M4.
- **Failure Status Codes:**
  - `400 Bad Request`: Missing username or password.
  - `401 Unauthorized`: Invalid credentials or deactivated user account (`is_active == false`).
- **Security Invariant:** User account status (`is_active`) must be verified during credential authentication and re-verified against MySQL on every subsequent request.

---

#### 14.1.2 POST /api/v1/users
- **Purpose:** Provision a new administrative or marketer user account.
- **SRS Traceability:** `FR-SEC-001`, `FR-SEC-002`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Required Role: `ROLE_ADMIN`.
- **Request Body:**
  ```json
  {
    "username": "marketer_john",
    "email": "marketer.john@crm.internal",
    "password": "TemporaryPassword123!",
    "role": "ROLE_MARKETER"
  }
  ```
- **Validation Rules:**
  - `username`: Required, 1–50 characters. Unique in system.
  - `email`: Required, valid email format, max 255 characters. Unique in system.
  - `password`: Required, minimum 8 characters, maximum 72 characters, maximum 72 UTF-8 bytes.
  - `role`: Required, enum: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Success Response (`201 Created`):**
  ```json
  {
    "success": true,
    "data": {
      "id": 2,
      "username": "marketer_john",
      "role": "ROLE_MARKETER",
      "isActive": true,
      "createdAt": "2026-09-19T10:15:30.123456Z"
    }
  }
  ```
- **Failure Status Codes:**
  - `400 Bad Request`: Validation failure.
  - `403 Forbidden`: Caller lacks `ROLE_ADMIN`.
  - `409 Conflict`: Username or email already registered.

---

#### 14.1.3 GET /api/v1/users
- **Purpose:** Retrieve a paginated list of system user accounts.
- **SRS Traceability:** Derived from `FR-SEC-001` (User Administration).
- **Classification:** `[CHOSEN REPRESENTATION]`
- **Authentication:** Bearer JWT. Required Role: `ROLE_ADMIN`.
- **Query Parameters:** `page`, `size`, `sort`.
- **Success Response (`200 OK`):** Paginated collection of user summary DTOs.

---

#### 14.1.4 GET /api/v1/users/{id}
- **Purpose:** Retrieve specific user account profile.
- **SRS Traceability:** Derived from `FR-SEC-001`.
- **Classification:** `[CHOSEN REPRESENTATION]`
- **Authentication:** Bearer JWT. Required Role: `ROLE_ADMIN`.
- **Path Parameters:** `id` (Long, required).
- **Success Response (`200 OK`):** User profile DTO.
- **Failure Status Codes:** `404 Not Found` if user does not exist.

---

#### 14.1.5 PATCH /api/v1/users/{id}/role
- **Purpose:** Modify an existing user's platform authorization role.
- **SRS Traceability:** `FR-SEC-002`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Required Role: `ROLE_ADMIN`.
- **Request Body:**
  ```json
  {
    "role": "ROLE_ADMIN"
  }
  ```
- **Business Invariant:** An administrator cannot demote their own account to prevent lockout.

---

#### 14.1.6 PATCH /api/v1/users/{id}/deactivate
- **Purpose:** Deactivate a user account, instantly barring further authentication.
- **SRS Traceability:** `FR-SEC-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Required Role: `ROLE_ADMIN`.
- **Business Invariant:** An administrator cannot deactivate their own active account.

---

#### 14.1.7 PATCH /api/v1/users/{id}/password
- **Purpose:** Reset or update a user account password credential.
- **SRS Traceability:** System Administration Requirement (`FR-SEC-001`).
- **Classification:** `[DESIGN DECISION]`
- **Authentication:** Bearer JWT. Required Role: `ROLE_ADMIN`.
- **Request Body:**
  ```json
  {
    "password": "NewSecurePassword123!"
  }
  ```
- **Validation Rules:**
  - `password`: Required, minimum 8 characters, maximum 72 characters, maximum 72 UTF-8 bytes.
- **Security Invariants:**
  - Password updates do **NOT** revoke existing JWTs in M4; existing access tokens expire naturally after 1 hour (3,600,000 ms).
  - Immediate user termination must be executed via account deactivation (`PATCH /api/v1/users/{id}/deactivate`).
  - Passwords are encrypted using BCrypt (strength 12). Plaintext passwords are never logged or stored.
- **Success Response (`200 OK`):** Uniform success envelope confirming password update.
- **Failure Status Codes:**
  - `400 Bad Request`: Validation failure (length < 8, > 72 chars, > 72 UTF-8 bytes).
  - `403 Forbidden`: Caller lacks `ROLE_ADMIN`.
  - `404 Not Found`: User does not exist.

---

### 14.2 Customer APIs

#### 14.2.1 POST /api/v1/customers
- **Purpose:** Create a single customer profile in MySQL.
- **SRS Traceability:** `FR-CUST-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Request Body:**
  ```json
  {
    "firstName": "Aarav",
    "lastName": "Sharma",
    "email": "aarav.sharma@example.in",
    "phone": "+919876543210",
    "totalSpend": 12500.50,
    "orderCount": 5,
    "lastOrderDate": "2026-08-15T14:30:00Z",
    "location": "Mumbai",
    "tags": ["VIP", "FestivalShopper"]
  }
  ```
- **Validation Rules:**
  - `firstName`, `lastName`: Required, 1–50 characters.
  - `email`: Required, valid email format, max 100 characters.
  - `phone`: Optional, valid E.164 or national phone format, max 20 characters.
  - `totalSpend`: Required, decimal >= 0.00, scale 2.
  - `orderCount`: Required, integer >= 0.
  - `lastOrderDate`: Optional ISO-8601 timestamp.
  - `location`: Optional string, max 100 characters.
  - `tags`: Optional array of distinct string tokens (each max 50 chars).
- **Success Response (`201 Created`):** Returns created customer DTO and `Location` header (`/api/v1/customers/{id}`).
- **Failure Status Codes:**
  - `400 Bad Request`: Format/field validation failure.
  - `409 Conflict`: Customer with this email already exists in an active state.

---

#### 14.2.2 GET /api/v1/customers/{id}
- **Purpose:** Retrieve a customer profile by primary key ID.
- **SRS Traceability:** `FR-CUST-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Path Parameter:** `id` (Long, required).
- **Success Response (`200 OK`):** Customer profile DTO including normalized tags.
- **Failure Status Codes:**
  - `404 Not Found`: Customer does not exist or has been soft-deleted (`deleted_at IS NOT NULL`).

---

#### 14.2.3 PATCH /api/v1/customers/{id}
- **Purpose:** Update mutable attributes of an existing customer.
- **SRS Traceability:** `FR-CUST-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Request Body:** Partial update fields (any valid customer field).
- **Success Response (`200 OK`):** Updated customer DTO.
- **Failure Status Codes:**
  - `400 Bad Request`: Validation failure on updated attributes.
  - `404 Not Found`: Customer does not exist or is soft-deleted.
  - `409 Conflict`: New email conflicts with another active customer.

---

#### 14.2.4 DELETE /api/v1/customers/{id}
- **Purpose:** Soft-delete a customer record from active platform operations.
- **SRS Traceability:** `FR-CUST-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Required Role: `ROLE_ADMIN`.
- **Path Parameter:** `id` (Long, required).
- **Behavior & Persistence Impact:**
  - Customer DELETE performs the approved soft-delete operation by setting deleted_at to the current UTC timestamp.
  - The record is preserved in MySQL to maintain referential integrity with historical `campaign_delivery_records`.
  - Excluded from all future dynamic segment queries and customer directory listings via `WHERE deleted_at IS NULL`.
  - Under MySQL `UNIQUE(email)`, the soft-deleted email remains reserved in v1 (`docs/design/Database-Design.md` §6.2).
- **Distinction Between Customer and Campaign Deletion:** Customer deletion soft-delete behavior is aligned with Database Design's `deleted_at` mechanism. In contrast, campaign deletion semantics remain an OPEN DESIGN DECISION (`ODD-API-06`) and must not be silently treated as customer-style soft delete.
- **Success Response (`204 No Content`):** No body returned.
- **Failure Status Codes:** `404 Not Found` if customer does not exist or is already soft-deleted.

---

#### 14.2.5 GET /api/v1/customers
- **Purpose:** Search and retrieve a paginated directory of active customers.
- **SRS Traceability:** `FR-CUST-001`, `FR-CUST-002`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Query Parameters:**
  - `page`, `size`, `sort`
  - `search`: Case-insensitive substring match against `firstName`, `lastName`, or `email`.
  - `location`: Exact match against customer location.
  - `minSpend`, `maxSpend`: Range filtering on `totalSpend`.
  - `minOrders`, `maxOrders`: Range filtering on `orderCount`.
  - `tag`: Filter by presence of specific tag.
- **Success Response (`200 OK`):** Paginated customer collection envelope.

---

#### 14.2.6 GET /api/v1/customers/count
- **Purpose:** Retrieve the total number of active, non-deleted customers in the platform.
- **SRS Traceability:** Operational reporting utility derived from `FR-CUST-001`.
- **Classification:** `[CHOSEN REPRESENTATION]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Success Response (`200 OK`):** `{"success": true, "data": {"totalActiveCustomers": 45120}}`.

---

### 14.3 Upload APIs

#### 14.3.1 POST /api/v1/uploads/bulk
- **Purpose:** Ingest customer records in bulk via multipart file upload.
- **SRS Traceability:** `FR-UPLOAD-001` through `FR-UPLOAD-006`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Request Encoding:** `multipart/form-data`.
- **Request Part:** `file` (Binary payload, CSV or XLSX format).
- **Interaction Pattern:** Synchronous HTTP request with streaming-oriented backend parsing and validation (`docs/design/System-Architecture.md` §6).
- **Processing Semantics:**
  - Row-by-row validation of customer fields.
  - **Partial Success Supported (`FR-UPLOAD-004`):** Valid rows are committed in database batches; invalid rows are recorded with specific row indices and error reasons.
  - Ingestion audit record created in `upload_history` table (`FR-UPLOAD-006`).
- **Success Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "uploadId": 14,
      "fileName": "q3_customers.csv",
      "status": "PARTIAL_SUCCESS",
      "totalRecords": 1000,
      "successfulRecords": 985,
      "failedRecords": 15,
      "errors": [
        {
          "row": 42,
          "email": "bad-email",
          "reason": "Invalid email format"
        }
      ]
    }
  }
  ```
- **Deferred / Open Thresholds:**
  - Exact maximum file size limit: **`[DEFERRED / REQUIRES TESTING]`** (`ODD-API-10`).
  - Exact maximum row count per upload: **`[DEFERRED / REQUIRES TESTING]`** (`ODD-API-10`).
  - Response error array truncation cap: **`[OPEN DESIGN DECISION]`** (`ODD-API-07`).

---

#### 14.3.2 GET /api/v1/uploads/history
- **Purpose:** Retrieve the paginated audit trail of historical bulk file ingestions.
- **SRS Traceability:** `FR-UPLOAD-006`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Query Parameters:** `page`, `size`, `sort`.
- **Success Response (`200 OK`):** Paginated list of `upload_history` audit summaries.

---

### 14.4 Segment APIs

#### 14.4.1 POST /api/v1/segments
- **Purpose:** Create a dynamic customer segment defined by an AST rule tree.
- **SRS Traceability:** `FR-SEG-001`, `FR-SEG-002`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Request Body:**
  ```json
  {
    "name": "High Value North Shoppers",
    "description": "Customers in Delhi/Punjab spending > 10,000",
    "ruleTree": {
      "combinator": "AND",
      "conditions": [
        {
          "field": "totalSpend",
          "operator": "GREATER_THAN",
          "value": 10000.00
        },
        {
          "combinator": "OR",
          "conditions": [
            {
              "field": "location",
              "operator": "EQUALS",
              "value": "Delhi"
            },
            {
              "field": "location",
              "operator": "EQUALS",
              "value": "Punjab"
            }
          ]
        }
      ]
    }
  }
  ```
- **Validation Rules:**
  - `name`: Required, unique, max 100 characters.
  - `ruleTree`: Required valid AST conforming to Section 10.1 constraints.
- **Success Response (`201 Created`):** Created segment DTO and `Location` header.
- **Failure Status Codes:**
  - `400 Bad Request`: AST syntax error or missing mandatory fields.
  - `409 Conflict`: Segment name already exists.
  - `422 Unprocessable Entity`: AST references unsupported field or illegal operator.

---

#### 14.4.2 GET /api/v1/segments/{id}
- **Purpose:** Retrieve segment definition, metadata, and structured rule tree.
- **SRS Traceability:** `FR-SEG-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Success Response (`200 OK`):** Segment entity DTO.

---

#### 14.4.3 PATCH /api/v1/segments/{id}
- **Purpose:** Update segment name, description, or rule tree.
- **SRS Traceability:** `FR-SEG-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Business Rule:** Updates affect subsequent evaluations; active running campaigns are unaffected as their audiences were authoritatively materialized at launch.

---

#### 14.4.4 DELETE /api/v1/segments/{id}
- **Purpose:** Delete a segment definition.
- **SRS Traceability:** `FR-SEG-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Business Invariant & Persistence Impact:**
  - Database enforces `ON DELETE RESTRICT` on `campaigns.segment_id`.
  - If any campaign (past or present) references this segment, the delete request **MUST BE REJECTED** with `409 Conflict` to preserve historical campaign integrity.
- **Success Response (`204 No Content`):** Segment deleted.

---

#### 14.4.5 POST /api/v1/segments/{id}/preview
- **Purpose:** Dynamically evaluate the segment rule tree against live MySQL customer data and return the matching audience count without creating a campaign.
- **SRS Traceability:** `FR-SEG-003`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Execution Mechanism:** Compiles AST into a `SELECT COUNT(*)` JPA Criteria query applying `deleted_at IS NULL`.
- **Success Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "segmentId": 5,
      "segmentName": "High Value North Shoppers",
      "matchedAudienceCount": 1420,
      "evaluatedAt": "2026-09-19T10:15:30.123456Z"
    }
  }
  ```

---

#### 14.4.6 GET /api/v1/segments/{id}/members
- **Purpose:** Retrieve a paginated list of actual customer members currently satisfying the segment rules.
- **SRS Traceability:** `FR-SEG-003`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Query Parameters:** `page`, `size`, `sort`.
- **Success Response (`200 OK`):** Paginated customer DTO envelope.

---

### 14.5 Campaign APIs

#### 14.5.1 POST /api/v1/campaigns
- **Purpose:** Author a new marketing campaign in `DRAFT` status.
- **SRS Traceability:** `FR-CAMP-001`, `FR-CAMP-002`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Request Body:**
  ```json
  {
    "name": "Diwali VIP Flash Sale",
    "segmentId": 5,
    "messageTemplate": "Hi {{firstName}}, enjoy an exclusive 25% discount today!"
  }
  ```
- **Validation Rules:**
  - `name`: Required, unique, max 100 characters.
  - `segmentId`: Required, must reference an existing valid segment.
  - `messageTemplate`: Required, non-empty, max 1000 characters.
- **Initial State:** Always created in `DRAFT` status.
- **Success Response (`201 Created`):** Created campaign DTO and `Location` header.

---

#### 14.5.2 GET /api/v1/campaigns/{id}
- **Purpose:** Retrieve campaign details, message template, status, and targeted audience metrics.
- **SRS Traceability:** `FR-CAMP-001`, `FR-CAMP-005`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Success Response (`200 OK`):** Campaign profile DTO.

---

#### 14.5.3 PATCH /api/v1/campaigns/{id}
- **Purpose:** Modify campaign properties (name, segment binding, template).
- **SRS Traceability:** `FR-CAMP-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Business Rule:** Updates permitted **ONLY** when `status == DRAFT`. Attempting to update a campaign in `RUNNING`, `COMPLETED`, or `FAILED` status yields `409 Conflict`.

---

#### 14.5.4 DELETE /api/v1/campaigns/{id}
- **Purpose:** Delete a campaign record.
- **SRS Traceability:** Derived from Campaign Management (`FR-CAMP-001`).
- **Classification:** `[CHOSEN REPRESENTATION]`
- **Authentication:** Bearer JWT. Required Role: `ROLE_ADMIN`.
- **Architectural Status (`ODD-API-06`):**
  - Campaign deletion semantics remain an **`[OPEN DESIGN DECISION]`**. The exact allowed campaign states and persistence behavior for deletion will be finalized in a later design decision.
  - *Distinction:* Customer deletion soft-delete behavior is aligned with Database Design's `deleted_at` mechanism. Conversely, campaign deletion semantics remain OPEN and must not be silently treated as customer-style soft delete.

---

#### 14.5.5 GET /api/v1/campaigns
- **Purpose:** Retrieve a paginated list of campaigns with status filtering.
- **SRS Traceability:** `FR-CAMP-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Query Parameters:** `page`, `size`, `sort`, `status` (optional filter).
- **Success Response (`200 OK`):** Paginated campaign summaries.

---

#### 14.5.6 POST /api/v1/campaigns/{id}/launch
- **Purpose:** Trigger immediate campaign execution.
- **SRS Traceability:** `FR-CAMP-003`, `FR-CAMP-004`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Lifecycle & Execution Invariants:**
  1. Campaign must currently be in `DRAFT` status (enforced via row lock `SELECT ... FOR UPDATE`).
  2. The bound segment rule tree is compiled and authoritatively evaluated against live MySQL data (`WHERE deleted_at IS NULL`).
  3. **Zero-Audience Rejection:** If `audienceSize == 0`, launch is rejected. Campaign **MUST REMAIN IN DRAFT**. It does **NOT** transition to `RUNNING`. Status returned is `400` or `422` per `ODD-API-02`.
  4. If `audienceSize > 0`:
     - MySQL atomic transaction records `target_audience_size = audienceSize`, transitions `status = RUNNING`, and bulk-inserts `campaign_delivery_records` records in `PENDING` status.
     - Asynchronous delivery dispatch is initiated via Redis Streams (`docs/design/System-Architecture.md` §6).
- **Success Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "campaignId": 8,
      "status": "RUNNING",
      "targetAudienceSize": 1420,
      "message": "Campaign transitioned to RUNNING and delivery processing has been initiated.",
      "launchedAt": "2026-09-19T10:15:30.123456Z"
    }
  }
  ```
- **Strict Contract Clarification:** The HTTP `200 OK` response signifies that the campaign successfully transitioned to `RUNNING` in MySQL and dispatch was initiated. It does **NOT** guarantee that all messages have been enqueued into Redis or delivered to recipients.

---

### 14.6 Delivery APIs

#### 14.6.1 GET /api/v1/campaigns/{id}/delivery-summary
- **Purpose:** Retrieve real-time aggregated delivery status counts and completion progress for a campaign.
- **SRS Traceability:** `FR-DEL-004`, `FR-CAMP-005`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Authoritative Source:** Aggregated directly from MySQL `campaign_delivery_records` and `campaigns` table.
- **Success Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "campaignId": 8,
      "campaignStatus": "RUNNING",
      "targetAudienceSize": 1420,
      "pendingCount": 120,
      "sentCount": 1285,
      "failedCount": 15,
      "completionPercentage": 91.55,
      "isTerminal": false
    }
  }
  ```

---

#### 14.6.2 GET /api/v1/campaigns/{id}/deliveries
- **Purpose:** Retrieve a paginated, filterable collection of individual customer delivery records.
- **SRS Traceability:** `FR-DEL-004`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Query Parameters:**
  - `page`, `size`, `sort`
  - `status`: Optional filter (`PENDING`, `SENT`, `FAILED`).
- **Success Response (`200 OK`):** Paginated delivery log records.
- **Architectural Boundary:** Exposes persistent business delivery statuses (`PENDING`, `SENT`, `FAILED`). Transient worker states (e.g., consumer group PEL, Redis stream message IDs) are **never** exposed over the API.

---

### 14.7 AI APIs

#### 14.7.1 POST /api/v1/ai/segments/generate-rules
- **Purpose:** Translate a natural-language segment requirement into a structured, validated Boolean AST rule tree via Google Gemini.
- **SRS Traceability:** `FR-AI-SEG-001`, `FR-AI-SEG-002`, `FR-AI-SEG-004`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Request Body:**
  ```json
  {
    "prompt": "Target customers living in Mumbai who have spent over 15000 rupees and ordered at least 3 times."
  }
  ```
- **Validation Rules:**
  - `prompt`: Required string, 10–500 characters.
- **Processing Pipeline:**
  1. Service enriches prompt with strict JSON schema constraints and domain attributes (minimizing sensitive data).
  2. Invokes Google Gemini via Spring AI client.
  3. Deserializes and validates the received JSON AST against platform rule constraints (Section 10.1).
  4. Records the raw prompt, raw response, and validation status in `ai_segment_audits` (`FR-AI-SEG-004`).
- **Response Contract Design (`ODD-API-05`):**
  - Returns the strictly validated AST rule tree.
  - `estimatedCount`: Marked as an **`[OPEN DESIGN DECISION]`**. If omitted, audience sizing is deferred to the explicit `/preview` endpoint.
- **Success Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "prompt": "Target customers living in Mumbai who have spent over 15000 rupees and ordered at least 3 times.",
      "ruleTree": {
        "combinator": "AND",
        "conditions": [
          {
            "field": "location",
            "operator": "EQUALS",
            "value": "Mumbai"
          },
          {
            "field": "totalSpend",
            "operator": "GREATER_THAN",
            "value": 15000.00
          },
          {
            "field": "orderCount",
            "operator": "GREATER_THAN_OR_EQUAL",
            "value": 3
          }
        ]
      },
      "isValidated": true
    }
  }
  ```
- **Failure Status Codes:**
  - `400 Bad Request`: Prompt is empty or malformed.
  - `422 Unprocessable Entity`: Gemini returned an unparseable or logically illegal rule tree failing AST validation.
  - `503 Service Unavailable`: Google Gemini API is unreachable, timed out, or rate-limited.

---

#### 14.7.2 GET /api/v1/ai/segments/audits
- **Purpose:** Inspect the historical audit trail of AI segment generation requests.
- **SRS Traceability:** `FR-AI-SEG-004`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Required Role: `ROLE_ADMIN`.
- **Query Parameters:** `page`, `size`, `sort`.
- **Success Response (`200 OK`):** Paginated records from `ai_segment_audits` table.

---

### 14.8 Reporting APIs

#### 14.8.1 GET /api/v1/reports/campaigns/{id}
- **Purpose:** Retrieve comprehensive delivery performance metrics and breakdown for a completed or active campaign.
- **SRS Traceability:** `FR-REPORT-001`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Success Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "campaignId": 8,
      "campaignName": "Diwali VIP Flash Sale",
      "status": "COMPLETED",
      "targetAudienceSize": 1420,
      "metrics": {
        "sent": 1405,
        "failed": 15,
        "deliveryRatePercentage": 98.94
      },
      "timeline": {
        "launchedAt": "2026-09-19T10:15:30Z",
        "completedAt": "2026-09-19T10:17:45Z",
        "durationSeconds": 135
      }
    }
  }
  ```

---

#### 14.8.2 GET /api/v1/reports/customers/overview
- **Purpose:** Retrieve high-level demographic, spending, and ordering KPIs across the active customer base.
- **SRS Traceability:** `FR-REPORT-002`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Success Response (`200 OK`):** Aggregated metrics including total active customers, gross customer spend, average spend per customer, and top locations.

---

#### 14.8.3 GET /api/v1/reports/campaigns/{id}/ai-summary
- **Purpose:** Generate an AI-powered natural language narrative summarizing campaign performance, engagement trends, and delivery outcomes.
- **SRS Traceability:** `FR-REPORT-003`.
- **Classification:** `[EXPLICIT SRS]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Execution:** Gathers campaign metrics from MySQL, formats summary prompt, and queries Google Gemini.
- **Failure Resilience:** If Gemini is unavailable, returns `503 Service Unavailable`. Non-AI campaign metrics (`/reports/campaigns/{id}`) remain 100% operational.

---

#### 14.8.4 GET /api/v1/reports/campaigns/history
- **Purpose:** Retrieve aggregated historical performance metrics across all launched campaigns.
- **SRS Traceability:** Operational reporting utility derived from `FR-REPORT-001`.
- **Classification:** `[CHOSEN REPRESENTATION]`
- **Authentication:** Bearer JWT. Permitted Roles: `ROLE_ADMIN`, `ROLE_MARKETER`.
- **Query Parameters:** `page`, `size`, `sort`.
- **Success Response (`200 OK`):** Paginated summary list of past campaign metrics.

---

## 15. Campaign Lifecycle and State Rules

### 15.1 Approved Lifecycle State Machine
Campaign execution adheres to a strict, non-reversible deterministic state machine:

```
┌──────────────┐
│    DRAFT     │ <───────────── Initial state upon authoring (POST /campaigns)
└──────┬───────┘
       │ Launch command (POST /campaigns/{id}/launch)
       │ - Segment evaluated against MySQL (WHERE deleted_at IS NULL)
       │ - If audience == 0 -> REJECTED (Campaign remains DRAFT)
       │ - If audience > 0  -> Atomic transition to RUNNING
       ▼
┌──────────────┐
│   RUNNING    │ <───────────── Delivery processing initiated
└──────┬───────┘
       │
       ├───────────────────────────────────────────┐
       │ All delivery obligations terminal:        │ Unrecoverable platform
       │ targetAudienceSize > 0 AND                │ or execution fault
       │ PENDING == 0 AND                          │
       │ (SENT + FAILED) == targetAudienceSize     │
       ▼                                           ▼
┌──────────────┐                            ┌──────────────┐
│  COMPLETED   │                            │    FAILED    │
└──────────────┘                            └──────────────┘
```

### 15.2 State Transition Invariants
1. **Immediate Execution Only:** All launches are immediate. Campaign scheduling (future timed execution) is strictly excluded from v1.
2. **Zero-Audience Invariant (`[DECIDED]`):** If the bound segment evaluates to zero matching active customers, the launch request is rejected. The campaign **MUST REMAIN IN DRAFT**. It does **NOT** transition to `RUNNING`.
3. **Immutability of Running/Terminal Campaigns:** Once a campaign transitions to `RUNNING`, `COMPLETED`, or `FAILED`, its template, name, and segment binding are completely immutable.
4. **Campaign Completion Invariant (`docs/design/Database-Design.md` §6.1):** A campaign transitions to `COMPLETED` if and only if:
   $$\text{status} = \text{RUNNING} \land \text{targetAudienceSize} > 0 \land \text{PENDING} = 0 \land (\text{SENT} + \text{FAILED}) = \text{targetAudienceSize}$$
5. **Terminality:** `COMPLETED` and `FAILED` are terminal states. A campaign cannot be relaunched.

---

## 16. Delivery and Asynchronous API Semantics

### 16.1 Asynchronous Decoupling Architecture
Delivery execution is strictly asynchronous to ensure API responsiveness under high audience volumes (`docs/design/System-Architecture.md` §5.2):

```
Client               API Gateway / Controller            MySQL 8.x               Redis Stream               Worker Pool
  │                             │                            │                         │                         │
  ├─ POST /campaigns/{id}/launch ─>│                            │                         │                         │
  │                             ├─ Evaluate Audience Size ──>│                         │                         │
  │                             │<─ audienceCount (N > 0) ───┤                         │                         │
  │                             ├─ Begin Transaction ───────>│                         │                         │
  │                             │  - status = RUNNING        │                         │                         │
  │                             │  - Bulk INSERT N PENDING   │                         │                         │
  │                             ├─ Commit Transaction ──────>│                         │                         │
  │                             ├─ Enqueue Delivery Tasks ────────────────────────────>│                         │
  │<─ 200 OK (status=RUNNING) ──┤                            │                         │                         │
  │                             │                            │                         │<─ XREADGROUP Consume ───┤
  │                             │                            │                         │                         ├─ Simulate Send
  │                             │                            │<─ UPDATE SENT/FAILED ─────────────────────────────┤
  │                             │                            │                         ├─ XACK Task ─────────────┤
```

### 16.2 Database Invariants vs. Delivery Semantics
- **Database-Level Deduplication:** The composite unique index `UNIQUE(campaign_id, customer_id)` on `campaign_delivery_records` guarantees that exactly one persistent delivery record exists per customer per campaign.
- **At-Least-Once Delivery Semantics:** Background delivery processing operates with at-least-once dispatch semantics. Network retries or consumer recoveries may cause duplicate message delivery attempts to external channels. The system does **NOT** guarantee end-to-end exactly-once external delivery.
- **Persistent Obligation State:** `PENDING` represents an unfulfilled delivery obligation in MySQL. The platform **does not introduce a persistent `PROCESSING` state** in the database; in-flight states are managed transiently within the Redis consumer group PEL (`Pending Entries List`).
- **MySQL Authoritative State:** Campaign completion is determined by querying MySQL `campaign_delivery_records`, **never** by reading volatile Redis counters.

---

## 17. Bulk Upload API Semantics

### 17.1 Interaction Model
- **Endpoint:** `POST /api/v1/uploads/bulk` (`multipart/form-data`).
- **Synchronous HTTP Lifecycle:** The HTTP connection remains open while the server streams, parses, and validates the file, returning an immediate ingestion summary.
- **Memory Safety:** Ingestion utilizes streaming parsers (e.g., Apache Commons CSV, Excel event-driven streaming) to process records line-by-line without buffering full files in heap memory (`NFR-SCALE-002`).

### 17.2 Row Validation & Partial Success (`FR-UPLOAD-004`)
- Each row is validated independently against customer schema constraints.
- **Partial Ingestion:** If a file of 1,000 rows contains 15 invalid rows, the 985 valid rows are committed to MySQL in transactional batches. The file status is recorded as `PARTIAL_SUCCESS`.
- Full diagnostic details (row numbers, failed email, reason) are serialized into `upload_history.error_details` (`docs/design/Database-Design.md` §4.7).

### 17.3 Deferred Upload Thresholds (`ODD-API-10`)
To avoid premature constraint freezing, the following limits are marked **`[DEFERRED / REQUIRES TESTING]`**:
- Maximum upload file size (bytes).
- Maximum total row count per upload file.
- Batch chunk insert size.

---

## 18. AI API Boundaries

### 18.1 Architectural Isolation of AI Services
Google Gemini AI integration is strictly isolated behind dedicated endpoints (`/api/v1/ai/*` and `/api/v1/reports/*/ai-summary`):
1. **Zero Core CRM Dependency:** Failure, timeout, rate-limiting, or outage of Google Gemini has **zero impact** on customer management, dynamic segment compilation, campaign authoring, or delivery execution.
2. **Decoupled Audience Evaluation:** `POST /api/v1/ai/segments/generate-rules` translates natural language to an AST rule tree. It **does not evaluate the audience size**. Audience sizing is strictly performed by querying MySQL via `/api/v1/segments/{id}/preview`.
3. **Data Minimization:** AI prompts transmit only structural schemas and user prompt text. Customer Personally Identifiable Information (PII) is **never** sent to the external AI provider.
4. **Mandatory Application Validation:** AI-generated rule trees are never trusted blindly; they must pass strict internal AST validation (Section 10.1) before being returned to the client.
5. **Auditing:** Every AI generation interaction (prompt, raw response, status) is persistently logged in `ai_segment_audits` (`FR-AI-SEG-004`).

---

## 19. Idempotency and Concurrency

### 19.1 Concurrency Controls
- **Campaign Launch Race Conditions:** To prevent duplicate launches from simultaneous client clicks or concurrent requests, `POST /campaigns/{id}/launch` acquires an explicit row-level lock (`SELECT ... FOR UPDATE`) on the campaign record inside a `READ COMMITTED` transaction. If the campaign is already `RUNNING`, the second request is immediately rejected with `409 Conflict`.
- **Delivery Log Deduplication:** `UNIQUE(campaign_id, customer_id)` prevents concurrent worker threads or retried launch routines from creating duplicate delivery obligations.
- **Customer Email Concurrency:** Active customer email uniqueness is enforced by MySQL InnoDB's unique index. Concurrent customer creation requests with the same email result in one transaction succeeding and the other failing with `409 Conflict`.

### 19.2 API-Level Idempotency (`ODD-API-01`)
- **Architectural Status:** Client-supplied header-based idempotency (`Idempotency-Key: <UUID>`) remains an **`[OPEN DESIGN DECISION]`**.
- **Scope Boundary:** No specific idempotency persistence mechanism (Redis TTL vs. MySQL table) is finalized in this step. In v1, operations rely on database unique constraints and state-machine preconditions to guarantee data integrity against duplicate requests.

---

## 20. Security Considerations

1. **Transport Layer Security (HTTPS/TLS):** HTTPS/TLS is required for deployed environments (`NFR-SEC-002`). The exact TLS version and deployment-level TLS configuration are deferred to Security Design and deployment configuration. Unencrypted HTTP traffic is rejected.
2. **Stateless JWT Validation:** Every protected request is validated against signature forgery, token expiration (`exp`), and algorithm tampering (`alg: none` rejected) via Spring Security filter chains.
3. **Principal Propagation:** User identity and granted authorities (`ROLE_ADMIN`, `ROLE_MARKETER`) are extracted from token claims and populated into the `SecurityContextHolder`.
4. **Input Sanitization & Injection Prevention:**
   - SQL Injection is prevented by utilizing Spring Data JPA and parameterized JPA Criteria API queries.
   - AST Rule validation ensures dynamic query compilation only references whitelisted customer attributes and operators.
5. **Least Privilege Principle:** Marketers are restricted from account administration, user management, customer deletion, and AI audit inspection.
6. **Information Disclosure Prevention:** Production configurations suppress stack traces, database error messages, and framework version headers in API responses.

---

## 21. OpenAPI / Swagger Documentation Strategy

1. **Tooling Standard:** API documentation will be generated using **SpringDoc OpenAPI 2.x, providing OpenAPI 3.x-compatible API documentation for Spring Boot 3.x** (`springdoc-openapi-starter-webmvc-ui`).
2. **Annotation Strategy:** Controller methods will be annotated with standard OpenAPI annotations:
   - `@Operation(summary = "...", description = "...")`
   - `@ApiResponse(responseCode = "200", description = "...")`
   - `@SecurityRequirement(name = "BearerAuth")`
3. **Swagger UI Path:** The exact Swagger UI and OpenAPI JSON endpoints are deferred to implementation configuration (`docs/SRS.md` requires OpenAPI documentation but does not freeze specific URI paths).

---

## 22. Traceability to SRS

The following matrix documents the precise traceability between `docs/SRS.md` requirements and the API Design specifications:

| SRS Requirement ID | Requirement Summary | API Domain | Endpoint / API Representation | Classification | Architectural Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`FR-CUST-001`** | Customer CRUD & Validation | Customer | `POST /api/v1/customers`<br>`GET /api/v1/customers/{id}`<br>`PATCH /api/v1/customers/{id}`<br>`DELETE /api/v1/customers/{id}` | `[EXPLICIT SRS]` | Soft-delete implemented via `deleted_at`; unique email enforced. |
| **`FR-CUST-002`** | Customer Listing & Search | Customer | `GET /api/v1/customers` | `[EXPLICIT SRS]` | Multi-field filtering; bounded pagination. |
| **`FR-UPLOAD-001`** | Bulk Upload Support | Upload | `POST /api/v1/uploads/bulk` | `[EXPLICIT SRS]` | Multipart form-data supporting CSV/XLSX. |
| **`FR-UPLOAD-002`** | Streaming Parse & Batching | Upload | `POST /api/v1/uploads/bulk` | `[EXPLICIT SRS]` | Streaming parser avoids full-file heap loading. |
| **`FR-UPLOAD-003`** | Ingestion Validation | Upload | `POST /api/v1/uploads/bulk` | `[EXPLICIT SRS]` | Row-level data validation before persistence. |
| **`FR-UPLOAD-004`** | Partial Success Processing | Upload | `POST /api/v1/uploads/bulk` | `[EXPLICIT SRS]` | Commits valid rows; records failed row indices. |
| **`FR-UPLOAD-005`** | Ingestion Feedback | Upload | `POST /api/v1/uploads/bulk` | `[EXPLICIT SRS]` | Response payload contains success/failure counts. |
| **`FR-UPLOAD-006`** | Ingestion Audit History | Upload | `GET /api/v1/uploads/history` | `[EXPLICIT SRS]` | Backed by `upload_history` MySQL table. |
| **`FR-SEG-001`** | Dynamic Segment Definition | Segment | `POST /api/v1/segments`<br>`GET /api/v1/segments/{id}`<br>`PATCH /api/v1/segments/{id}`<br>`DELETE /api/v1/segments/{id}` | `[EXPLICIT SRS]` | JSON AST rule tree; RESTRICT on campaign FK. |
| **`FR-SEG-002`** | Criteria API Compilation | Segment | Internal Service Compilation | `[EXPLICIT SRS]` | Application compiles AST to Criteria query. |
| **`FR-SEG-003`** | Dynamic Audience Evaluation | Segment | `POST /api/v1/segments/{id}/preview`<br>`GET /api/v1/segments/{id}/members` | `[EXPLICIT SRS]` | Live evaluation against MySQL without persistence. |
| **`FR-CAMP-001`** | Campaign CRUD & Template | Campaign | `POST /api/v1/campaigns`<br>`GET /api/v1/campaigns/{id}`<br>`PATCH /api/v1/campaigns/{id}`<br>`GET /api/v1/campaigns` | `[EXPLICIT SRS]` | Initial state `DRAFT`; template max 1000 chars. |
| **`FR-CAMP-002`** | Campaign-Segment Binding | Campaign | `POST /api/v1/campaigns` | `[EXPLICIT SRS]` | Foreign key binding to `segments.id`. |
| **`FR-CAMP-003`** | Immediate Campaign Launch | Campaign | `POST /api/v1/campaigns/{id}/launch` | `[EXPLICIT SRS]` | Immediate execution; no scheduling feature. |
| **`FR-CAMP-004`** | Zero-Audience Rejection | Campaign | `POST /api/v1/campaigns/{id}/launch` | `[EXPLICIT SRS]` | Decided: rejects launch, remains in `DRAFT`. |
| **`FR-CAMP-005`** | Campaign Status Tracking | Campaign / Delivery | `GET /api/v1/campaigns/{id}`<br>`GET /api/v1/campaigns/{id}/delivery-summary` | `[EXPLICIT SRS]` | Reflects real-time MySQL counts and status. |
| **`FR-DEL-001`** | Async Message Dispatch | Delivery | Background Redis Worker | `[EXPLICIT SRS]` | Asynchronous dispatch via Redis Streams. |
| **`FR-DEL-002`** | Simulated Delivery Worker | Delivery | Background Worker | `[EXPLICIT SRS]` | 90% success, 10% failure simulation. |
| **`FR-DEL-003`** | Delivery Status Updates | Delivery | Background Worker | `[EXPLICIT SRS]` | Updates MySQL `campaign_delivery_records` terminal state. |
| **`FR-DEL-004`** | Delivery Logging & Querying | Delivery | `GET /api/v1/campaigns/{id}/deliveries`<br>`GET /api/v1/campaigns/{id}/delivery-summary` | `[EXPLICIT SRS]` | Paginated logs and aggregated metrics. |
| **`FR-AI-SEG-001`** | Natural Language Query Input | AI | `POST /api/v1/ai/segments/generate-rules` | `[EXPLICIT SRS]` | Accepts natural language prompt (10–500 chars). |
| **`FR-AI-SEG-002`** | AST JSON Output via Gemini | AI | `POST /api/v1/ai/segments/generate-rules` | `[EXPLICIT SRS]` | Uses Spring AI / Gemini to return JSON AST. |
| **`FR-AI-SEG-003`** | Fallback & Error Handling | AI | Service Layer | `[EXPLICIT SRS]` | Isolation; returns 503 when AI unavailable. |
| **`FR-AI-SEG-004`** | AI Generation Auditing | AI | `GET /api/v1/ai/segments/audits` | `[EXPLICIT SRS]` | Backed by `ai_segment_audits` MySQL table. |
| **`FR-SEC-001`** | Authentication & User Mgmt | Auth | `POST /api/v1/auth/login`<br>`POST /api/v1/users`<br>`PATCH /api/v1/users/{id}/deactivate` | `[EXPLICIT SRS]` | BCrypt hashed credentials; JWT tokens. |
| **`FR-SEC-002`** | Role-Based Access Control | Auth / Cross-cutting | Spring Security Method Interceptors | `[EXPLICIT SRS]` | Strictly two roles: `ROLE_ADMIN`, `ROLE_MARKETER`. |
| **`FR-REPORT-001`**| Campaign Performance Report | Reporting | `GET /api/v1/reports/campaigns/{id}`<br>`GET /api/v1/reports/campaigns/history` | `[EXPLICIT SRS]` | Delivery rates, audience counts, duration. |
| **`FR-REPORT-002`**| Customer Demographic Overview| Reporting | `GET /api/v1/reports/customers/overview` | `[EXPLICIT SRS]` | Customer KPIs, spend distributions. |
| **`FR-REPORT-003`**| AI Narrative Summary | Reporting | `GET /api/v1/reports/campaigns/{id}/ai-summary` | `[EXPLICIT SRS]` | Narrative report via Gemini. |
| **`NFR-PERF-001`** | Bounded API Latency Intent | Cross-cutting | Architectural Optimization | `[DESIGN DECISION]` | Direct index lookups; non-blocking async dispatch. |
| **`NFR-SEC-001`** | Stateless Session Security | Auth / Cross-cutting | JWT Bearer Authentication | `[EXPLICIT SRS]` | Stateless request handling via Spring Security. |
| **`NFR-SEC-002`** | Transport Layer Encryption | Cross-cutting | HTTPS/TLS Deployment | `[EXPLICIT SRS]` | HTTPS/TLS required in deployed environments; exact TLS version deferred to Security Design. |

---

## 23. Open and Deferred API Decisions

The following formal register documents all decisions that remain open, deferred, or dependent on empirical testing:

| Decision ID | Area | Current Status | Description & Trade-off | Resolution Plan |
| :--- | :--- | :--- | :--- | :--- |
| **`ODD-API-01`** | Idempotency | **`OPEN DESIGN DECISION`** | **API-Level Idempotency-Key Header:** Whether mutating endpoints (`POST /campaigns/{id}/launch`, `POST /customers`) should support client-supplied `Idempotency-Key: <UUID>`. Avoids duplicate executions if network drops. Trade-off: Requires persistence store for keys vs. relying on database unique constraints. | Resolve in Security/API hardening pass prior to implementation freeze. |
| **`ODD-API-02`** | Campaign Launch | **`OPEN DESIGN DECISION`** | **Zero-Audience Rejection HTTP Status:** Behavior is DECIDED (launch rejected, campaign remains `DRAFT`). Exact status code is open between `400 Bad Request` (illegal command) vs. `422 Unprocessable Entity` (semantic domain failure). | Resolve during implementation freeze. |
| **`ODD-API-03`** | Error Contract | **`OPEN DESIGN DECISION`** | **Error Response Representation:** Custom Standardized Error Envelope (`success: false, error: {...}`) vs. RFC 7807 Problem Details (`application/problem+json`). Trade-off: Client uniformity vs. IETF standardization. | Resolve during implementation freeze. |
| **`ODD-API-04`** | Pagination | **`OPEN DESIGN DECISION`** | **Default & Maximum Page-Size Values:** Bounded pagination is approved. Exact numeric defaults and maximum page caps across collections remain open to avoid unbenchmarked limits. | Resolve after query performance testing on large datasets. |
| **`ODD-API-05`** | AI Response | **`OPEN DESIGN DECISION`** | **AI Rule Generation Response Envelope:** Whether `POST /ai/segments/generate-rules` returns strictly the validated AST, or optionally includes an `estimatedCount` by executing a preview query. Trade-off: Endpoint purity vs. client convenience. | Resolve during frontend/UI API contract review. |
| **`ODD-API-06`** | Campaign Delete | **`OPEN DESIGN DECISION`** | **Campaign Deletion Semantics:** Campaign deletion semantics remain an OPEN DESIGN DECISION. The exact allowed campaign states and persistence behavior for deletion will be finalized in a later design decision. Unlike Customer deletion (DECIDED as `deleted_at` soft-delete), campaign deletion semantics must not be silently treated as customer-style soft delete. | Resolve during implementation freeze. |
| **`ODD-API-07`** | Bulk Upload | **`OPEN DESIGN DECISION`** | **Bulk Upload Error Response Bounding:** In files with thousands of row failures, whether to truncate the HTTP response `errors` array (e.g., returning first $N$ errors while persisting all errors to `upload_history.error_details`). | Resolve during testing with large corrupt CSV files. |
| **`ODD-API-08`** | Segment AST | **`DEFERRED / REQUIRES TESTING`**| **AST Complexity Constraints:** Numeric upper bounds on maximum nesting depth and leaf condition count. Trade-off: Expressiveness vs. JPA Criteria generation overhead and MySQL execution plan complexity. | Benchmark against deeply nested ASTs before finalizing limits. |
| **`ODD-API-09`** | AI Personalization| **`DEFERRED`** | **AI Personalization Fallback Mechanics:** Specific fallback template behaviors if dynamic per-message AI personalization fails or times out during background delivery simulation. | Defer to Async/Delivery Runtime Design. |
| **`ODD-API-10`** | Bulk Upload | **`DEFERRED / REQUIRES TESTING`**| **Bulk File Size & Row Thresholds:** Maximum allowed file upload size (MB) and maximum rows per file. Trade-off: Ingestion throughput vs. multipart parsing heap overhead and request timeout limits. | Benchmark with streaming upload tests before freezing numbers. |

---

## 24. API Risks and Design Trade-offs

### 24.1 API-Level Architectural Risks & Mitigations

1. **Large Collection Payloads (Memory Exhaustion):**
   - *Risk:* Unbounded collection requests could retrieve tens of thousands of customer or delivery records, exhausting server heap memory and saturating network bandwidth.
   - *Mitigation:* Bounded pagination is mandatory on all collection endpoints (`ODD-API-04`). Unpaged collection queries are prohibited at the controller layer.
2. **Concurrent Campaign Launch Race Conditions:**
   - *Risk:* Multiple rapid clicks or concurrent client requests could attempt to launch the same `DRAFT` campaign simultaneously, creating duplicate Redis tasks.
   - *Mitigation:* Database row-level locking (`SELECT ... FOR UPDATE`) within a `READ COMMITTED` transaction ensures only one thread transitions the campaign to `RUNNING`. Subsequent threads detect non-DRAFT status and fail with `409 Conflict`.
3. **Asynchronous State Visibility Gap:**
   - *Risk:* Clients launching a campaign may immediately query delivery logs before background workers have begun processing, seeing zero progress.
   - *Mitigation:* The API contract explicitly clarifies that `POST /campaigns/{id}/launch` initiates processing. The client is instructed to poll `GET /campaigns/{id}/delivery-summary` to track live progress.
4. **External AI Provider Latency and Outages:**
   - *Risk:* High latency, rate-limiting, or service disruption from Google Gemini could degrade API responsiveness or block core CRM operations.
   - *Mitigation:* AI operations are strictly isolated in `/api/v1/ai/*`. Timeouts (enforced at the Spring AI HTTP client level) prevent thread pool exhaustion, failing fast with `503 Service Unavailable`. Core CRM features operate completely independently of Gemini.
5. **AI-Generated Malformed AST Injection:**
   - *Risk:* An external LLM might hallucinate non-existent database columns, unsupported operators, or malformed Boolean structures.
   - *Mitigation:* All AI responses pass through an internal AST Validator before reaching the client or database. Dynamic queries are compiled via parameterized Criteria API, preventing SQL injection.
6. **Large Multipart Ingestion Resource Saturation:**
   - *Risk:* High-volume multipart file uploads could exhaust disk space or memory if buffered inappropriately.
   - *Mitigation:* Ingestion relies on streaming parsers with row-by-row validation. File size thresholds (`ODD-API-10`) will be enforced at the gateway/servlet container level.

---

## 25. API Design Summary

The API Design for the **Enterprise AI-CRM Platform (`CS-CRM-2026`)** provides a robust, scalable, and secure RESTful contract across 36 endpoints across the eight canonical API domains. 

### Key Architectural Strengths:
- **Strict Layer Decoupling:** Complete separation between external DTO representations and internal relational persistence entities.
- **Authoritative State Consistency:** MySQL 8.x is the sole persistent source of truth, enforcing business invariants (campaign state transitions, zero-audience rejection, customer uniqueness).
- **Transient Async Processing:** Background message delivery is decoupled through Redis Streams, ensuring sub-second API response times during campaign launch.
- **Resilient AI Boundary:** Google Gemini AI capabilities are isolated, ensuring platform survivability during external AI outages.
- **Enterprise Traceability:** 100% of approved functional and non-functional requirements from `docs/SRS.md` v1.0.1 are mapped to concrete endpoint specifications without scope creep or unapproved technology additions.

---

## 26. Decision Register

| Category | Decision Identifiers & Architectural Summaries |
| :--- | :--- |
| **`DECIDED`** | 1. **Base Path & Versioning:** Anchored at `/api/v1/` using URI path versioning.<br>2. **Stateless Security:** Spring Security 6.x with stateless JWT Bearer tokens.<br>3. **Canonical Domains:** Exactly 8 domains (Authentication, Customer, Upload, Segment, Campaign, Delivery, AI, Reporting).<br>4. **Role Taxonomy:** Exactly two roles (`ROLE_ADMIN`, `ROLE_MARKETER`); ADMIN is the superset (`ROLE_ADMIN > ROLE_MARKETER`).<br>5. **Persistence Authority:** MySQL 8.x is the sole persistent store; Redis is strictly transient async transport.<br>6. **Zero-Audience Campaign Launch:** Launch request is rejected; campaign remains in `DRAFT` and does not transition to `RUNNING`.<br>7. **Campaign Lifecycle:** `DRAFT` $\rightarrow$ `RUNNING` $\rightarrow$ `COMPLETED` / `FAILED`. All launches immediate (no scheduling).<br>8. **Campaign Completion Invariant:** Handled authoritatively by MySQL when all obligations are terminal (`SENT` + `FAILED` == targetAudienceSize, `PENDING` == 0).<br>9. **Delivery Semantics:** At-least-once asynchronous processing; `UNIQUE(campaign_id, customer_id)` provides persistent record deduplication (not end-to-end exactly-once external delivery).<br>10. **Delivery State:** `PENDING` is the persistent obligation state. No persistent `PROCESSING` state in MySQL.<br>11. **Customer Soft-Delete:** Enforced via `customers.deleted_at` timestamp; active queries filter `WHERE deleted_at IS NULL`.<br>12. **AI Decoupling:** AI rule generation is isolated from audience evaluation; audience evaluation belongs strictly to preview/execution against MySQL.<br>13. **Bulk Ingestion Support:** Multipart CSV and XLSX support with partial success capability.<br>14. **M4 Security Baseline (Frozen):** JWT algorithm `HS256` with mandatory `JWT_SECRET` (>= 32 bytes); 1h token lifetime; refresh tokens omitted in M4; login endpoint accepts username OR email in `username` field; JWT `sub` is canonical username; live DB active & role authority check on each request; password policy 8–72 characters / 72 UTF-8 bytes with BCrypt strength 12; password changes do not revoke existing JWTs; initial admin bootstrap executes only when `users` table is empty (`count == 0`). |
| **`RECOMMENDED`** | 1. **Success Envelope:** Uniform `{"success": true, "data": ..., "metadata": ...}` response contract across all domains.<br>2. **Customer Tags:** Normalized child table `customer_tags` (`customer_id`, `tag`) with `UNIQUE(customer_id, tag)`.<br>3. **OpenAPI Tooling:** SpringDoc OpenAPI 2.x, providing OpenAPI 3.x-compatible API documentation for Spring Boot 3.x. |
| **`OPEN`** | 1. **`ODD-API-01`:** API-Level `Idempotency-Key` header support and storage strategy.<br>2. **`ODD-API-02`:** Zero-audience campaign launch rejection status code (`400 Bad Request` vs `422 Unprocessable Entity`).<br>3. **`ODD-API-03`:** Error response envelope representation (Custom Envelope vs RFC 7807 Problem Details).<br>4. **`ODD-API-04`:** Default and maximum numeric values for collection pagination.<br>5. **`ODD-API-05`:** AI rule generation response envelope (strict AST vs optional `estimatedCount`).<br>6. **`ODD-API-06`:** Campaign deletion semantics (allowed campaign states and persistence behavior remain open).<br>7. **`ODD-API-07`:** Bulk upload error response bounding in HTTP payload. |
| **`DEFERRED`** | 1. **`ODD-API-09`:** AI personalization fallback mechanics during worker delivery simulation. |
| **`REQUIRES TESTING`**| 1. **`ODD-API-08`:** Segment AST complexity constraints (maximum depth and node count limits).<br>2. **`ODD-API-10`:** Bulk upload maximum file size and row thresholds. |
| **`OUT OF SCOPE`** | 1. **Alternative Persistence:** PostgreSQL, MongoDB, Cassandra, SQLite.<br>2. **Distributed Message Brokers:** Apache Kafka, RabbitMQ.<br>3. **Caching Layer:** Redis cache-aside / entity caching.<br>4. **Resilience Frameworks:** Resilience4j circuit breakers.<br>5. **Generic Audit Table:** Generic `audit_logs` table (DBD-16 confirmed OUT OF SCOPE; operational logging via SLF4J/MDC).<br>6. **Campaign Scheduling:** Timed/cron future campaign execution.<br>7. **End-to-End Exactly-Once Processing:** External channel delivery is simulated at-least-once. |

---

## 27. Revision History

| Version | Date | Author | Description | Status |
| :--- | :--- | :--- | :--- | :--- |
| 1.0.0 | 2026-09-19 | Senior Enterprise Software Architect & API Design Team | Initial API Design Specification baseline. | Approved |
| 1.0.1 | 2026-09-20 | Senior Enterprise Software Architect & API Design Team | M4 Security Baseline Freeze: Added Section 9.2 delineating AuthenticationEntryPoint (401), AccessDeniedHandler (403), and GlobalExceptionHandler; updated POST /api/v1/auth/login to document username OR email lookup and canonical username sub claim; updated POST /api/v1/users and PATCH /api/v1/users/{id}/password with 8–72 char / 72 UTF-8 byte password policy; recorded non-revocation of JWTs on password update. | Approved |
