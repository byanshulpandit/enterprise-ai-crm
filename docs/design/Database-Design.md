# Database Design Document
## Enterprise AI-CRM Platform (`CS-CRM-2026`)

---

### Document Metadata
- **Project Code:** CS-CRM-2026
- **System Name:** Enterprise AI-CRM Platform
- **Document Version:** 1.0.0
- **SDLC Phase:** Phase 2 — System Design (Database Design Specification)
- **Author:** System Architecture & Database Engineering Team
- **Date:** 2026-09-19
- **Primary Source of Truth:** [Software Requirements Specification (docs/SRS.md)](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/SRS.md) v1.0.1
- **Architectural Reference:** [System Architecture Document (docs/design/System-Architecture.md)](file:///c:/Users/ANSHUL%20GAUTAM/OneDrive/Desktop/CLG-CRM/docs/design/System-Architecture.md) v1.0.0
- **Target Audience:** Database Administrators, Backend Engineers, System Architects, Academic Evaluators

---

## 1. Purpose & Scope

This document specifies the authoritative relational database design for the **Enterprise AI-CRM Platform (`CS-CRM-2026`)**. It defines the persistent data model hosted on **MySQL 8.x (InnoDB)** supporting core customer management, streaming bulk ingestion, dynamic audience segmentation, asynchronous campaign execution, simulated message delivery tracking, and AI interaction governance.

### Scope Boundaries:
- **Persistent Data Store:** MySQL 8.x is the **sole persistent source of truth**.
- **Transient Infrastructure:** Redis 7.x operates strictly as transient asynchronous infrastructure for message queueing, buffering, and consumer group offset management. Redis contains zero persistent business state.
- **Excluded Technologies:** PostgreSQL, MongoDB, Cassandra, SQLite, distributed enterprise message brokers (Kafka/RabbitMQ), Redis caching/cache-aside, and external circuit breakers (Resilience4j) are strictly excluded from the v1 persistence scope.
- **Cross-Cutting Auditing Boundary:** A generic persistent entity mutation diff table (`audit_logs`) is **`[OUT OF SCOPE]`** for the MySQL database. System-level operational auditability is satisfied via structured application logging (SLF4J/Logback + MDC) per `NFR-MAINT-003`. Explicitly mandated domain audit requirements (`ai_segment_audits` per `FR-AI-SEG-004` and `upload_history` per `FR-UPLOAD-006`) are fully modeled as first-class persistent relational tables.

---

## 2. Authoritative References

This specification is directly derived from and strictly governed by:
1. **`docs/SRS.md` (Version 1.0.1):** Defines functional requirements (`FR-CUST-*`, `FR-UPLOAD-*`, `FR-SEG-*`, `FR-AI-*`, `FR-CAMP-*`, `FR-DEL-*`, `FR-SEC-*`, `FR-REPORT-*`) and non-functional data constraints (`NFR-DATA-001` through `NFR-DATA-005`, `NFR-SCALE-001` through `NFR-SCALE-004`).
2. **`docs/design/System-Architecture.md` (Version 1.0.0):** Defines architectural patterns, layered service boundaries, dynamic Criteria API compilation, Redis Stream integration invariants, and persistence technology splits.
3. **Database Design Step 1 Review & Scope Determinations:** Enforces confirmed determinations regarding primary keys, soft-delete mechanics, delivery idempotency, timestamp uniformity, and scope boundaries.

---

## 3. Database Technology & Governance

| Governance Dimension | Approved Standard | Architectural Justification |
| :--- | :--- | :--- |
| **RDBMS Engine** | **MySQL 8.x** | Confirmed by SRS §7.2 (`NFR-DATA-005`). Sole persistent store. |
| **Storage Engine** | **InnoDB** | Provides ACID transactionality, row-level locking, foreign key integrity, and crash-recovery logging. |
| **Character Set & Collation**| **`utf8mb4` / `utf8mb4_0900_ai_ci`** | Complete Unicode support (including multilingual customer names, symbols, emojis in campaign templates). |
| **Transaction Isolation** | **`READ COMMITTED`** | Predictable visibility across concurrent operations; reduces lock duration and avoids gap locking during bulk ingestion and delivery status updates. |
| **Timezone Management** | **UTC Alignment** | Application and database session connections operate consistently in UTC. `DATETIME(6)` stores the microsecond-precision date-time value without timezone metadata; the application layer enforces UTC alignment. |
| **Currency Representation** | **`DECIMAL(12,2)`** | Exact fixed-point numeric representation for customer spend (`total_spend`) preventing floating-point rounding errors. |

---

## 4. Final Entity Inventory

The confirmed v1 persistent database schema consists of exactly **8 relational tables**:

```
+----------------------------------------------------------------------------------------------------+
|                                    CONFIRMED ENTITY INVENTORY (v1)                                  |
+----------------------------------------------------------------------------------------------------+
| 1. users                      : Operator identities, authentication credentials, and RBAC roles.    |
| 2. customers                  : Authoritative customer demographic, financial, and contact records. |
| 3. customer_tags              : Normalized categorization tags associated with customer profiles.   |
| 4. segments                   : Reusable dynamic audience filter trees stored as native MySQL JSON. |
| 5. campaigns                  : Marketing campaigns, base templates, and execution lifecycles.      |
| 6. campaign_delivery_records  : Granular per-recipient message delivery tracking and status logs.   |
| 7. upload_history             : Audit ledger of bulk CSV/XLSX file ingestion jobs and row outcomes. |
| 8. ai_segment_audits          : Compliance audit trail for AI natural-language segmentation prompts.|
+----------------------------------------------------------------------------------------------------+
```

> [!IMPORTANT]
> A generic `audit_logs` table is explicitly **`[OUT OF SCOPE]`**. Operational and administrative events are captured via SLF4J/MDC structured application logs per `NFR-MAINT-003`. Domain-specific audits are persisted in `ai_segment_audits` and `upload_history`.

---

## 5. Entity Relationship Overview

```
                          +-------------------+
                          |       users       |
                          +-------------------+
                            | 1             | 1
                            |               |
             +--------------+ 1:N           | 1:N
             |                              v
             v                    +-------------------+
   +-------------------+          |   ai_segment_     |
   |     segments      |          |     audits        |
   +-------------------+          +-------------------+
     | 1            | 1                     |
     |              +---------+ 1:N         |
     | 1:N                    |             |
     v                        v             | (segment_id FK,
+-------------------+     +-----------------+  ON DELETE SET NULL)
|     campaigns     |
+-------------------+
     | 1
     |
     | 1:N
     v
+-------------------+     1:N     +-------------------+
| campaign_delivery |<------------|     customers     |
|      records      |             +-------------------+
+-------------------+                       | 1
                                            |
                                            | 1:N (Dependent)
                                            v
                                  +-------------------+
                                  |   customer_tags   |
                                  +-------------------+

   +-------------------+
   |  upload_history   |  (uploaded_by references users.id)
   +-------------------+
```

---

## 6. Detailed Table Specifications

### 6.1 `users` Table
- **Purpose:** Stores authenticated administrative and marketing operator accounts, credentials, and RBAC role assignments (`FR-SEC-001`, `FR-SEC-004`, `FR-SEC-006`).
- **Mutability:** Mutable business entity.

| Column | Data Type | Nullable | Default | Constraints | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | NO | `AUTO_INCREMENT` | `PRIMARY KEY` | Surrogate sequential identifier. |
| `username` | `VARCHAR(50)` | NO | None | `UNIQUE KEY uq_users_username` | Unique system handle for authentication. |
| `email` | `VARCHAR(255)` | NO | None | `UNIQUE KEY uq_users_email` | Validated corporate email address. |
| `password_hash` | `VARCHAR(255)` | NO | None | None | Adaptive one-way hash (e.g., BCrypt). |
| `role` | `VARCHAR(20)` | NO | None | `CHECK (role IN ('ROLE_ADMIN', 'ROLE_MARKETER'))` | RBAC authorization role. |
| `is_active` | `BOOLEAN` | NO | `TRUE` | None | Account lifecycle flag (soft deactivation). |
| `created_at` | `DATETIME(6)` | NO | None | None | System creation timestamp (UTC). |
| `updated_at` | `DATETIME(6)` | NO | None | None | Record modification timestamp (UTC). |

- **Foreign Keys:** None.
- **Delete Behavior:** Cannot be physically deleted if referenced by child records (`ON DELETE RESTRICT` from child tables). Accounts are soft-deactivated via `is_active = FALSE`.
- **Update Behavior:** Monitored via application persistence lifecycle; `updated_at` refreshed on modification.

---

### 6.2 `customers` Table
- **Purpose:** Authoritative repository of customer profiles, contact coordinates, demographic locations, cumulative financial metrics, and activity recency (`FR-CUST-001` to `FR-CUST-007`).
- **Mutability:** Mutable business entity with logical soft deletion.

| Column | Data Type | Nullable | Default | Constraints | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | NO | `AUTO_INCREMENT` | `PRIMARY KEY` | Clustered primary key; sequential 64-bit integer. |
| `first_name` | `VARCHAR(100)` | NO | None | None | Customer given name. |
| `last_name` | `VARCHAR(100)` | NO | None | None | Customer family name. |
| `email` | `VARCHAR(255)` | NO | None | `UNIQUE KEY uq_customers_email` | Unique customer email; primary business key. |
| `phone` | `VARCHAR(30)` | YES | `NULL` | None | Contact telephone (E.164 formatted string). |
| `city` | `VARCHAR(100)` | YES | `NULL` | None | Residential city for geographic segmentation. |
| `country` | `VARCHAR(100)` | YES | `NULL` | None | Country of residence. |
| `total_spend` | `DECIMAL(12,2)` | NO | `0.00` | None | Cumulative lifetime spend amount. |
| `visit_count` | `INT UNSIGNED` | NO | `0` | None | Total recorded visits / interactions. |
| `last_active_date` | `DATE` | YES | `NULL` | None | Date of most recent customer activity. |
| `status` | `VARCHAR(20)` | NO | `'ACTIVE'` | None | Operational status (`ACTIVE`, `INACTIVE`). |
| `created_at` | `DATETIME(6)` | NO | None | None | Registration timestamp (UTC). |
| `updated_at` | `DATETIME(6)` | NO | None | None | Last modification timestamp (UTC). |
| `deleted_at` | `DATETIME(6)` | YES | `NULL` | None | Soft-delete marker (`NULL` = active, non-null = deleted). |

- **Foreign Keys:** None.
- **Delete Behavior:** Soft deletion exclusively (`FR-CUST-005`). Customer records are never physically removed during standard operations, permanently preserving foreign key referential integrity with delivery records.
- **Update Behavior:** Monitored via Spring Data JPA auditing; `updated_at` refreshed on modification.

---

### 6.3 `customer_tags` Table
- **Purpose:** Normalized storage of freeform classification tags assigned to customer profiles (`FR-CUST-001`, `FR-SEG-002`).
- **Mutability:** Effectively append-only association table; associations are created or deleted.

| Column | Data Type | Nullable | Default | Constraints | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | NO | `AUTO_INCREMENT` | `PRIMARY KEY` | Surrogate primary key. |
| `customer_id` | `BIGINT UNSIGNED` | NO | None | `FOREIGN KEY` | References parent `customers.id`. |
| `tag` | `VARCHAR(50)` | NO | None | None | Classification string (e.g., `VIP`, `LEAD`). |
| `created_at` | `DATETIME(6)` | NO | None | None | Record association timestamp (UTC). |
| `updated_at` | `DATETIME(6)` | NO | None | None | Timestamp satisfying `NFR-DATA-003`; initialized equal to `created_at`. |

- **Foreign Keys:**
  - `fk_cust_tags_customer`: `customer_id` $\to$ `customers(id)` `ON DELETE CASCADE ON UPDATE RESTRICT`.
- **Unique Constraints:** `UNIQUE KEY uq_customer_tag (customer_id, tag)` prevents duplicate tag assignments per customer.
- **Delete Behavior:** Cascades physically if parent customer record is purged in maintenance/test environments.
- **Update Behavior:** Tag associations are discrete; updates are modeled as delete/insert pairs. `updated_at` is initialized on creation.

---

### 6.4 `segments` Table
- **Purpose:** Stores reusable dynamic audience segmentation rules compiled from visual builders or AI natural language inputs (`FR-SEG-001` to `FR-SEG-006`).
- **Mutability:** Mutable business entity.

| Column | Data Type | Nullable | Default | Constraints | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | NO | `AUTO_INCREMENT` | `PRIMARY KEY` | Sequential surrogate identifier. |
| `name` | `VARCHAR(100)` | NO | None | None | Human-readable segment title (non-unique at DB level). |
| `description` | `VARCHAR(500)` | YES | `NULL` | None | Business intent and descriptive notes. |
| `rules` | `JSON` | NO | None | None | Abstract Syntax Tree (AST) of dynamic boolean rules. |
| `created_by` | `BIGINT UNSIGNED` | NO | None | `FOREIGN KEY` | References creating operator `users.id`. |
| `created_at` | `DATETIME(6)` | NO | None | None | Segment definition timestamp (UTC). |
| `updated_at` | `DATETIME(6)` | NO | None | None | Last rule modification timestamp (UTC). |

- **Foreign Keys:**
  - `fk_segments_created_by`: `created_by` $\to$ `users(id)` `ON DELETE RESTRICT ON UPDATE RESTRICT`.
- **Delete Behavior:** Deletion is blocked (`ON DELETE RESTRICT`) if the segment is bound to any campaign (`campaigns.segment_id`).
- **Update Behavior:** Permitted when segment is not locked by an active campaign execution.

---

### 6.5 `campaigns` Table
- **Purpose:** Manages marketing campaign configurations, base message templates, segment linkage, execution status lifecycles, and AI-generated performance summaries (`FR-CAMP-001` to `FR-CAMP-005`, `FR-REPORT-001`, `FR-REPORT-003`).
- **Mutability:** Mutable business entity transitioning through an enforced state machine.

| Column | Data Type | Nullable | Default | Constraints | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | NO | `AUTO_INCREMENT` | `PRIMARY KEY` | Sequential surrogate identifier. |
| `name` | `VARCHAR(150)` | NO | None | None | Human-readable campaign title (non-unique at DB level). |
| `description` | `VARCHAR(500)` | YES | `NULL` | None | Marketing campaign summary notes. |
| `segment_id` | `BIGINT UNSIGNED` | NO | None | `FOREIGN KEY` | References target audience `segments.id`. |
| `message_template` | `TEXT` | NO | None | None | Base communication copy with variable tokens. |
| `status` | `VARCHAR(20)` | NO | `'DRAFT'` | `CHECK (status IN ('DRAFT', 'RUNNING', 'COMPLETED', 'FAILED'))` | Enforced campaign lifecycle status. |
| `personalization_enabled`| `BOOLEAN` | NO | `FALSE` | None | Flag toggling context-aware AI personalization. |
| `ai_summary` | `TEXT` | YES | `NULL` | None | Natural-language performance narrative from Gemini. |
| `created_by` | `BIGINT UNSIGNED` | NO | None | `FOREIGN KEY` | References campaign creator `users.id`. |
| `started_at` | `DATETIME(6)` | YES | `NULL` | None | Timestamp when state transitioned to `RUNNING`. |
| `completed_at` | `DATETIME(6)` | YES | `NULL` | None | Timestamp when all delivery tasks reached terminal state. |
| `created_at` | `DATETIME(6)` | NO | None | None | Record creation timestamp (UTC). |
| `updated_at` | `DATETIME(6)` | NO | None | None | State change or modification timestamp (UTC). |

- **Foreign Keys:**
  - `fk_campaigns_segment`: `segment_id` $\to$ `segments(id)` `ON DELETE RESTRICT ON UPDATE RESTRICT`.
  - `fk_campaigns_created_by`: `created_by` $\to$ `users(id)` `ON DELETE RESTRICT ON UPDATE RESTRICT`.
- **Delete Behavior:** Deletion permitted in application service only when `status = 'DRAFT'`. Database blocks deletion if child delivery records exist.
- **Update Behavior:** Permitted only while `status = 'DRAFT'`. Atomic conditional updates govern lifecycle transitions.

---

### 6.6 `campaign_delivery_records` Table
- **Purpose:** Authoritative ledger of per-recipient message deliveries; records the final rendered message copy, delivery outcome, and failure diagnostic (`FR-DEL-001` to `FR-DEL-005`, `FR-AI-CAMP-002`).
- **Mutability:** High-volume transactional entity updating from `PENDING` to terminal state (`SENT` or `FAILED`).

| Column | Data Type | Nullable | Default | Constraints | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | NO | `AUTO_INCREMENT` | `PRIMARY KEY` | Sequential surrogate identifier. |
| `campaign_id` | `BIGINT UNSIGNED` | NO | None | `FOREIGN KEY` | References parent `campaigns.id`. |
| `customer_id` | `BIGINT UNSIGNED` | NO | None | `FOREIGN KEY` | References recipient `customers.id`. |
| `message` | `TEXT` | NO | None | None | Final resolved message (personalized or fallback). |
| `status` | `VARCHAR(20)` | NO | `'PENDING'` | `CHECK (status IN ('PENDING', 'SENT', 'FAILED'))` | Delivery state obligation. |
| `failure_reason` | `VARCHAR(500)` | YES | `NULL` | None | Diagnostic error string upon terminal delivery failure. |
| `processed_at` | `DATETIME(6)` | YES | `NULL` | None | Timestamp when worker finalized delivery simulation. |
| `created_at` | `DATETIME(6)` | NO | None | None | Ingestion/staging timestamp (UTC). |
| `updated_at` | `DATETIME(6)` | NO | None | None | Status transition timestamp (UTC). |

- **Foreign Keys:**
  - `fk_deliv_campaign`: `campaign_id` $\to$ `campaigns(id)` `ON DELETE RESTRICT ON UPDATE RESTRICT`.
  - `fk_deliv_customer`: `customer_id` $\to$ `customers(id)` `ON DELETE RESTRICT ON UPDATE RESTRICT`.
- **Unique Constraints:** `UNIQUE KEY uq_campaign_customer (campaign_id, customer_id)` enforces strict database-level record deduplication per campaign run.
- **Delete Behavior:** Physical deletion blocked via `ON DELETE RESTRICT`. Preserves legal and regulatory communication records.
- **Update Behavior:** Updated atomically by delivery worker threads transitioning `status` from `PENDING` to `SENT` or `FAILED`.

---

### 6.7 `upload_history` Table
- **Purpose:** Persistent operational audit log recording bulk customer ingestion operations, file parameters, summary metrics, and structured error payloads (`FR-UPLOAD-005`, `FR-UPLOAD-006`).
- **Mutability:** Effectively append-only operational log.

| Column | Data Type | Nullable | Default | Constraints | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | NO | `AUTO_INCREMENT` | `PRIMARY KEY` | Surrogate primary key. |
| `uploaded_by` | `BIGINT UNSIGNED` | NO | None | `FOREIGN KEY` | References operating user `users.id`. |
| `file_name` | `VARCHAR(255)` | NO | None | None | Original name of the uploaded CSV or XLSX file. |
| `file_type` | `VARCHAR(10)` | NO | None | `CHECK (file_type IN ('CSV', 'XLSX'))` | Parsed file format. |
| `total_rows` | `INT UNSIGNED` | NO | `0` | None | Total rows parsed from file stream. |
| `success_count`| `INT UNSIGNED` | NO | `0` | None | Valid rows committed to `customers` table. |
| `failure_count`| `INT UNSIGNED` | NO | `0` | None | Invalid or rejected rows collected during parsing. |
| `status` | `VARCHAR(20)` | NO | None | `CHECK (status IN ('SUCCESS', 'PARTIAL_SUCCESS', 'FAILED'))` | Ingestion job outcome. |
| `error_details`| `JSON` | YES | `NULL` | None | Array of row indices and validation error messages. |
| `created_at` | `DATETIME(6)` | NO | None | None | Ingestion completion timestamp (UTC). |
| `updated_at` | `DATETIME(6)` | NO | None | None | Timestamp satisfying `NFR-DATA-003`; initialized equal to `created_at`. |

- **Foreign Keys:**
  - `fk_upload_user`: `uploaded_by` $\to$ `users(id)` `ON DELETE RESTRICT ON UPDATE RESTRICT`.
- **Delete Behavior:** Deletion restricted; maintains permanent administrative upload records.
- **Update Behavior:** Written once upon completion of the streaming upload pipeline. `updated_at` is initialized on creation and not normally modified.

---

### 6.8 `ai_segment_audits` Table
- **Purpose:** Immutable compliance ledger logging natural-language audience segmentation prompts, AI-generated rule trees from Google Gemini, and user acceptance decisions (`FR-AI-SEG-004`).
- **Mutability:** Strictly append-only compliance log.

| Column | Data Type | Nullable | Default | Constraints | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | NO | `AUTO_INCREMENT` | `PRIMARY KEY` | Surrogate primary key. |
| `user_id` | `BIGINT UNSIGNED` | NO | None | `FOREIGN KEY` | References prompting operator `users.id`. |
| `prompt_text` | `TEXT` | NO | None | None | Raw natural-language audience query. |
| `generated_rules`| `JSON` | NO | None | None | Structured AST rule tree returned by Gemini. |
| `action_taken` | `VARCHAR(20)` | NO | None | `CHECK (action_taken IN ('SAVED', 'DISCARDED'))` | User decision taken after preview. |
| `segment_id` | `BIGINT UNSIGNED` | YES | `NULL` | `FOREIGN KEY` | References created `segments.id` (if saved). |
| `created_at` | `DATETIME(6)` | NO | None | None | Audit event timestamp (UTC). |
| `updated_at` | `DATETIME(6)` | NO | None | None | Timestamp satisfying `NFR-DATA-003`; initialized equal to `created_at`. |

- **Foreign Keys:**
  - `fk_ai_audit_user`: `user_id` $\to$ `users(id)` `ON DELETE RESTRICT ON UPDATE RESTRICT`.
  - `fk_ai_audit_segment`: `segment_id` $\to$ `segments(id)` `ON DELETE SET NULL ON UPDATE RESTRICT`.
- **Delete Behavior:** If a saved segment is subsequently deleted, `segment_id` is set to `NULL` (`ON DELETE SET NULL`), preserving the immutable prompt audit log.
- **Update Behavior:** Immutable append-only. `updated_at` is initialized equal to `created_at` and not modified.

---

## 7. Primary Keys & Identifier Strategy

- **Architectural Standard:** **`BIGINT UNSIGNED AUTO_INCREMENT`** is the decided standard across all 8 persistent tables (`DBD-02`).
- **Engine Rationale:** 
  - In MySQL InnoDB, tables are physically clustered along the primary key B+Tree. Monotonically increasing sequential integers ensure that new records are inserted at the rightmost page edge, avoiding random B+Tree page splits and fragmentation.
  - Foreign key references in high-volume tables (`campaign_delivery_records`) require only 8 bytes per pointer, minimizing index storage and memory footprint compared to 36-byte UUID strings or 16-byte binary UUIDs.
- **External Exposure:** Primary surrogate keys are internal identifiers. If non-sequential identifiers are required for public REST APIs, obfuscated tokens or public alternate UUIDs may be exposed at the application layer without altering internal clustered integer primary keys.

---

## 8. Foreign Keys & Referential Integrity

- **Database-Enforced Integrity:** All inter-table associations are strictly enforced via InnoDB `FOREIGN KEY` constraints (`NFR-DATA-001`).
- **Immutable Keys Policy:** Primary key values are immutable. Therefore, foreign keys use **`ON UPDATE RESTRICT`** across all relations, avoiding engine-level cascade propagation overhead.
- **Delete Action Policy:**
  - **`ON DELETE RESTRICT` (Default):** Enforced on all core business relationships (`users`, `segments`, `campaigns`, `customers`, `campaign_delivery_records`) to prevent accidental or unauthorized data loss.
  - **`ON DELETE CASCADE` (Exception):** Applied strictly to `customer_tags.customer_id`, because tags represent dependent weak attributes with no independent lifecycle.
  - **`ON DELETE SET NULL` (Exception):** Applied to `ai_segment_audits.segment_id`, preserving audit history if a created segment is subsequently decommissioned.

---

## 9. Nullability, Defaults & Constraints Summary

| Table | Column | Nullable | Default | Constraint Type | Business Rule / Invariant Enforced |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `users` | `username` | NO | None | `UNIQUE` | Unique login handle (`FR-SEC-001`). |
| `users` | `email` | NO | None | `UNIQUE` | Unique user identity email (`FR-SEC-001`). |
| `users` | `role` | NO | None | `CHECK` | Restricts role to `ROLE_ADMIN` or `ROLE_MARKETER`. |
| `customers` | `email` | NO | None | `UNIQUE` | Enforces global customer email uniqueness (`FR-CUST-002`). |
| `customers` | `total_spend` | NO | `0.00` | None | Non-null cumulative monetary total. |
| `customers` | `visit_count` | NO | `0` | None | Non-null cumulative interaction counter. |
| `customers` | `deleted_at` | YES | `NULL` | None | Soft-delete state marker (`NULL` = active). |
| `customer_tags` | `(customer_id, tag)` | NO | None | `UNIQUE` | Prevents duplicate tag assignments. |
| `campaigns` | `status` | NO | `'DRAFT'` | `CHECK` | Restricts lifecycle to `DRAFT`, `RUNNING`, `COMPLETED`, `FAILED`. |
| `delivery_records`| `(campaign_id, customer_id)`| NO | None | `UNIQUE` | **Idempotency Key:** Deduplicates dispatch obligations. |
| `delivery_records`| `status` | NO | `'PENDING'`| `CHECK` | Restricts delivery state to `PENDING`, `SENT`, `FAILED`. |
| `upload_history` | `file_type` | NO | None | `CHECK` | Restricts file format to `CSV` or `XLSX` (`FR-UPLOAD-001`). |
| `upload_history` | `status` | NO | None | `CHECK` | Restricts outcome to `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`. |
| `ai_segment_audits`| `action_taken` | NO | None | `CHECK` | Restricts user action to `SAVED` or `DISCARDED` (`FR-AI-SEG-004`).|

---

## 10. Status & Enum Value Definitions

All state domains are enforced using standard relational `VARCHAR(20)` columns governed by explicit SQL `CHECK` constraints. Proprietary MySQL `ENUM` types are avoided to ensure portability and seamless JPA mapping.

```
1. users.role:
   - 'ROLE_ADMIN'    : System administrator with full provisioning and operational access.
   - 'ROLE_MARKETER' : Marketing operator managing customers, segments, and campaigns.

2. campaigns.status:
   - 'DRAFT'         : Configuration mode; template and audience editable.
   - 'RUNNING'       : Dispatch initiated; delivery tasks active; configuration locked.
   - 'COMPLETED'     : Terminal state; all targeted audience delivery records are terminal.
   - 'FAILED'        : Terminal state; execution aborted due to unrecoverable system failure.

3. campaign_delivery_records.status:
   - 'PENDING'       : Persistent delivery obligation staged; awaiting worker execution.
   - 'SENT'          : Terminal state; delivery simulation succeeded.
   - 'FAILED'        : Terminal state; delivery simulation failed after all retries exhausted.

4. upload_history.file_type:
   - 'CSV'           : Comma-Separated Values text file.
   - 'XLSX'          : Microsoft Excel Open XML Spreadsheet format.

5. upload_history.status:
   - 'SUCCESS'       : All parsed rows passed validation and committed to database.
   - 'PARTIAL_SUCCESS': Valid rows committed; one or more invalid rows logged in error_details.
   - 'FAILED'        : File rejected before row persistence (e.g., malformed headers).

6. ai_segment_audits.action_taken:
   - 'SAVED'         : AI-generated AST preview was accepted and saved as a segment.
   - 'DISCARDED'     : AI-generated AST preview was discarded without saving.
```

*(Note: Any concept of "PROCESSING" is strictly an in-memory/transient worker state in Redis PEL and is **not** a persistent MySQL status).*

---

## 11. Soft Delete Strategy

### 11.1 Mechanics & Lifecycle
- **Column Definition:** `customers.deleted_at DATETIME(6) NULL DEFAULT NULL`.
- **State Representation:**
  - `deleted_at IS NULL`: Customer is **ACTIVE**.
  - `deleted_at IS NOT NULL`: Customer is **LOGICALLY DELETED**.
- **Physical Preservation:** The database never executes `DELETE FROM customers` during standard CRM operations (`FR-CUST-005`).

### 11.2 Query Governance
- Standard CRUD searches (`FR-CUST-003`, `FR-CUST-006`) and count endpoints (`FR-CUST-007`) are required by business rules to filter active customers. Soft deletion does not automatically filter queries at the database engine level; application repositories and Criteria queries must explicitly include `WHERE deleted_at IS NULL` where active customers are required.
- Dynamic audience segmentation queries generated by `SegmentQueryCompiler` unconditionally inject an active-customer predicate:
  ```java
  predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
  ```
- **Referential Integrity Preservation:** Because customer rows are preserved physically, foreign keys in `campaign_delivery_records` referencing `customer_id` remain valid across the entire system lifecycle. Historical campaign delivery statistics and reporting metrics remain mathematically consistent even after customer accounts are closed.

### 11.3 Email Uniqueness under Soft Delete
- In v1, customer email enforces strict global uniqueness via `UNIQUE KEY uq_customers_email (email)`.
- A composite `UNIQUE (email, deleted_at)` index is **NOT** used, because standard SQL NULL semantics would not provide the intended active-email uniqueness behavior.
- Soft-deleted customer emails remain reserved in the database. An attempt to register a new customer with a soft-deleted email is rejected by the database constraint with error code `1062 (23000)`. The application service layer intercepts this and provides appropriate conflict messaging or account reactivation options.

---

## 12. Customer Tags Design

### 12.1 Normalized Relational Structure
Customer tags are implemented as a **normalized child table** (`customer_tags`) containing `(id, customer_id, tag, created_at, updated_at)` with `UNIQUE (customer_id, tag)` (`DBD-06`):
1. **Relational Normalization:** Eliminates multi-valued repeating groups within the customer row, conforming to standard relational modeling principles.
2. **Indexing Suitability:** Individual tags can be indexed with standard B-Tree indexes, facilitating fast equality and set-membership filtering without table scans.
3. **Criteria API Integration:** Enables seamless composition of tag-based segmentation rules via standard JPA joins (`root.join(Customer_.tags)`) without requiring proprietary database-specific functions (e.g., MySQL `MEMBER OF()` or JSON multi-valued index casting).
4. **Rejection of Alternatives:** Avoids unindexed full table scans and substring collision errors inherent in comma-separated `VARCHAR` fields, and avoids complex multi-valued array indexing in JSON columns.

---

## 13. Segment Rule JSON / AST Design

### 13.1 Storage & Schema Representation
Segment rules are persisted in `segments.rules` as a native MySQL `JSON` document representing an Abstract Syntax Tree (AST) of boolean conditions:

```json
{
  "combinator": "AND",
  "rules": [
    {
      "field": "city",
      "operator": "EQUALS",
      "value": "Mumbai"
    },
    {
      "combinator": "OR",
      "rules": [
        {
          "field": "total_spend",
          "operator": "GREATER_THAN",
          "value": 5000.00
        },
        {
          "field": "visit_count",
          "operator": "GREATER_THAN_OR_EQUAL",
          "value": 3
        }
      ]
    }
  ]
}
```

### 13.2 Governance & Execution Model
1. **Storage vs. Execution:** The `JSON` column serves strictly as a persistent storage representation of the recursive rule tree. MySQL JSON query functions (`JSON_EXTRACT`, `JSON_TABLE`) are **not** the primary segmentation query engine.
2. **Application Validation Boundary:** The application service layer is solely responsible for validating the AST before persistence:
   - Mandatory field whitelist enforcement (`city`, `country`, `total_spend`, `visit_count`, `last_active_date`, `status`, `tag`).
   - Mandatory operator whitelist enforcement (`EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN_OR_EQUAL`, `IN`, `NOT_IN`, `CONTAINS`).
   - Compatible value type verification and structural AST integrity checks.
   - Configurable complexity constraints (such as nesting depth and leaf count) are governed by application configuration rather than arbitrary schema limits.
3. **AI Untrusted Boundary:** AI-generated rule trees from Gemini (`FR-AI-SEG-001`) are classified as untrusted input. They must pass full application schema and whitelist validation before being persisted to `segments.rules`.
4. **Criteria API Compilation:** The application deserializes the validated JSON AST into an object graph and compiles it into a type-safe **Spring Data JPA Criteria API** query executing against indexed relational columns in `customers` and `customer_tags`.

---

## 14. Campaign & Delivery Persistence Model

### 14.1 Campaign Launch & Delivery Flow
1. **Audience Evaluation:** Marketer triggers launch; application evaluates segment query.
2. **Zero-Audience Behavior (`DBD-14`):** If the evaluated segment returns zero customers, the launch request is rejected with an appropriate client error (`400 Bad Request` or `422 Unprocessable Entity`), and the campaign remains in `DRAFT` status.
3. **Dispatch Initialization:** For a valid non-zero audience, the campaign transitions to `RUNNING`, and delivery records are batch-inserted into MySQL as persistent delivery obligations with status `PENDING`.
4. **Async Transport:** Corresponding delivery tasks `{campaignId, customerId}` are enqueued into Redis Streams for background worker consumption.
5. **Worker Execution:** Workers simulate delivery, render context-aware personalization (or fallback), atomically update the MySQL record to terminal status (`SENT` or `FAILED`), and acknowledge (`XACK`) the Redis task.

### 14.2 The Five Campaign Completion Invariants
A campaign may transition from `RUNNING` to `COMPLETED` **if and only if** all five conditions are satisfied:
1. The campaign's current persistent status in MySQL is `RUNNING`;
2. The campaign has a valid, non-zero targeted audience;
3. All persisted delivery records for that campaign have reached a terminal state (`SENT` or `FAILED`);
4. Exactly zero delivery records associated with that campaign remain in `PENDING` status;
5. The status transition is executed using a concurrency-safe conditional update.

MySQL remains the authoritative persistent source of truth. Redis stream counters or transient queue lengths are **not** authoritative completion mechanisms.

### 14.3 Delivery Idempotency & Side-Effect Boundaries
- **Database-Level Deduplication:** `UNIQUE KEY uq_campaign_customer (campaign_id, customer_id)` prevents duplicate persistent delivery records for the same campaign/customer pair.
- **Side-Effect Reality:** Database-level record deduplication does not guarantee exactly-once external side effects. V1 operates with **at-least-once asynchronous processing**. If a worker crashes after simulated delivery but before database commit, the re-delivered task will update the record upon retry.

### 14.4 MySQL $\to$ Redis Consistency Invariant (`DBD-13`)
- **Invariant Statement:** MySQL `PENDING` delivery records are the authoritative persistent delivery obligations. If Redis enqueue fails, the record remains `PENDING` in MySQL, and the campaign cannot complete while the obligation remains unresolved.
- **Deferred Runtime Mechanism:** The exact runtime reconciliation/recovery mechanism (such as background reconciliation scanners, startup recovery tasks, or explicit reconciliation workers) is formally **`[DEFERRED]`** to the Async/Delivery Runtime Design phase. The database design freezes the invariant that MySQL state governs completion.

---

## 15. Bulk Upload Persistence Model

Bulk ingestion (`FR-UPLOAD-001` through `FR-UPLOAD-006`) implements **batch-level transaction boundaries with independent row-level validation**:
1. **File Parsing:** Streaming parsers (OpenCSV for CSV, Apache POI SAX for XLSX) read file streams iteratively without loading the entire file into JVM memory.
2. **Row-Level Application Validation (`FR-UPLOAD-004`):** The application layer performs row-level validation (email format, mandatory fields, types). The database supports validation through relational data types, NOT NULL constraints, foreign keys, and unique indexes.
3. **Partial Success Support (`NFR-REL-002`):** The ingestion pipeline does **not** treat the entire file as a single atomic transaction. Valid rows are persisted in bounded batches via Spring JDBC (`JdbcTemplate`), while invalid rows are captured as errors. The failure of one row does not roll back valid rows.
4. **Audit Commitment:** Upon completion, `upload_history` records the final execution outcome (`SUCCESS`, `PARTIAL_SUCCESS`, or `FAILED`) along with parsed row counts and the JSON `error_details` payload.

---

## 16. AI Audit Persistence Model

- **Segmentation Prompt Auditing (`FR-AI-SEG-004`):** Persisted in `ai_segment_audits`. Captures `prompt_text`, `generated_rules` (JSON AST), `action_taken` (`SAVED` or `DISCARDED`), `user_id`, and `segment_id` (nullable).
- **Campaign Personalization Message Separation:**
  - `campaigns.message_template`: Stores the base marketer-authored template with variable placeholders.
  - `campaign_delivery_records.message`: Stores the actual resolved, context-aware personalized message (or deterministic fallback) delivered to that recipient.
- **Campaign Performance Narrative:** The natural-language summary generated by Gemini is persisted directly in `campaigns.ai_summary` (`FR-REPORT-003`).

---

## 17. Timestamp & Auditing Strategy

### 17.1 Uniformity Standard (`NFR-DATA-003`)
In strict adherence to `NFR-DATA-003` ("All persistent entities must carry `createdAt` and `updatedAt` timestamps"), all 8 persistent tables include:
- `created_at DATETIME(6) NOT NULL`
- `updated_at DATETIME(6) NOT NULL`

### 17.2 Effectively Append-Only Entities
For entities that are effectively append-only (`customer_tags`, `upload_history`, `ai_segment_audits`):
- `updated_at` is initialized upon record creation equal to `created_at`.
- Application logic does not normally update these records, preserving audit integrity while satisfying uniform schema requirements.

---

## 18. Index Strategy

Indexes are strictly segregated into Mandatory Structural Indexes and Candidate Performance Indexes:

### 18.1 Mandatory Structural Indexes `[DECIDED]`
Required to enforce primary keys, unique constraints, and foreign key references:
- `users.PRIMARY KEY (id)`
- `users.UNIQUE KEY uq_users_username (username)`
- `users.UNIQUE KEY uq_users_email (email)`
- `customers.PRIMARY KEY (id)`
- `customers.UNIQUE KEY uq_customers_email (email)`
- `customer_tags.PRIMARY KEY (id)`
- `customer_tags.UNIQUE KEY uq_customer_tag (customer_id, tag)`
- `segments.PRIMARY KEY (id)`
- `segments.INDEX idx_seg_created_by (created_by)`
- `campaigns.PRIMARY KEY (id)`
- `campaigns.INDEX idx_camp_segment_id (segment_id)`
- `campaigns.INDEX idx_camp_created_by (created_by)`
- `campaign_delivery_records.PRIMARY KEY (id)`
- `campaign_delivery_records.UNIQUE KEY uq_campaign_customer (campaign_id, customer_id)` (Mandatory Idempotency Key)
- `campaign_delivery_records.INDEX idx_deliv_cust_id (customer_id)`
- `upload_history.PRIMARY KEY (id)`
- `upload_history.INDEX idx_upload_user (uploaded_by)`
- `ai_segment_audits.PRIMARY KEY (id)`
- `ai_segment_audits.INDEX idx_ai_audit_user (user_id)`
- `ai_segment_audits.INDEX idx_ai_audit_segment (segment_id)`

### 18.2 Candidate Performance Indexes `[REQUIRES TESTING]`
Planned index optimizations subject to query plan verification (`EXPLAIN FORMAT=JSON`) and load testing:
- `customers.INDEX idx_cust_del_spent (deleted_at, total_spend)`: Candidate for spend segmentation on active customers.
- `customers.INDEX idx_cust_del_activity (deleted_at, last_active_date)`: Candidate for recency segmentation on active customers.
- `customers.INDEX idx_cust_del_city (deleted_at, city)`: Candidate for demographic city segmentation on active customers.
- `customers.INDEX idx_cust_created_at (created_at)`: Candidate for customer list pagination sorting.
- `customer_tags.INDEX idx_tag (tag)`: Candidate for tag-based audience filtering.
- `campaigns.INDEX idx_camp_status_created (status, created_at)`: Candidate for campaign dashboard status filtering.
- `campaign_delivery_records.INDEX idx_deliv_camp_status (campaign_id, status)`: Candidate for delivery statistics aggregation (`COUNT(*) GROUP BY status`).
- `upload_history.INDEX idx_upload_created (created_at)`: Candidate for upload history paging.

> [!NOTE]
> Candidate indexes are not guaranteed final indexes; their creation will depend on empirical query execution plans and index maintenance overhead observed during performance testing.

---

## 19. Transaction & Concurrency Design

### 19.1 Isolation Level Governance
- **Engine Standard:** `READ COMMITTED` (`DBD-18`).
- **Concurrency Rule:** Isolation level alone does not guarantee state-transition correctness. Critical transitions enforce explicit row locking and conditional updates:
  - **Campaign Launch Guard:** `SELECT ... FOR UPDATE` on `campaigns` ensures only one operator can trigger launch from `DRAFT` to `RUNNING`.
  - **Campaign Completion Guard:** Concurrency-safe conditional update ensures atomic transition:
    ```sql
    UPDATE campaigns 
    SET status = 'COMPLETED', completed_at = NOW(6) 
    WHERE id = :campaignId 
      AND status = 'RUNNING' 
      AND NOT EXISTS (
          SELECT 1 FROM campaign_delivery_records 
          WHERE campaign_id = :campaignId AND status = 'PENDING'
      );
    ```
    *(Note: Exact SQL syntax is design guidance; implementation queries will be verified during testing).*

---

## 20. JPA vs. JDBC Responsibilities

| Workload Area | Technology Choice | Responsibilities & Design Justification |
| :--- | :--- | :--- |
| **Domain Entity CRUD** | **Spring Data JPA / Hibernate 6.x** | Manages `User`, `Customer`, `Segment`, `Campaign` entity lifecycles, auditing hooks, and object-relational mapping. |
| **Dynamic Segmentation Engine** | **Spring Data JPA Criteria API** | `SegmentQueryCompiler` compiles validated JSON AST rule trees into type-safe Criteria queries against indexed relational columns. |
| **Streaming Bulk Ingestion** | **Spring JDBC (`JdbcTemplate`)** | High-volume batch inserts bypass Hibernate session tracking overhead and use batch execution for streaming customer insertion. |
| **Reporting Aggregations** | **Spring Data Projections / JDBC** | Read-only statistical queries (`FR-REPORT-001`, `FR-REPORT-002`) execute projection queries (`COUNT`, `AVG`, `GROUP BY`) to bypass entity hydration overhead. |

---

## 21. Data Integrity & Failure Handling

| Failure Scenario | Database Protection Layer | Application Handling & Recovery |
| :--- | :--- | :--- |
| **Duplicate Customer Email** | `UNIQUE KEY uq_customers_email (email)` triggers error `1062`. | Application catches exception; returns `409 Conflict`. In bulk upload, row is logged as an error and parsing continues. |
| **Duplicate Delivery Task from Redis** | `UNIQUE KEY uq_campaign_customer (campaign_id, customer_id)`. | Database blocks duplicate row; worker detects existing record and skips redundant delivery. |
| **Stranded `PENDING` Records (Redis Outage)** | MySQL `PENDING` records remain authoritative delivery obligations. | Campaign remains `RUNNING`; cannot complete until deferred runtime recovery mechanism resolves stranded records. |
| **Invalid Foreign Key Reference** | Foreign key constraint raises error `1452`. | Application validation catches non-existent entities before persistence; returns `400 Bad Request`. |
| **Attempted Deletion of Active Segment** | `fk_campaigns_segment ON DELETE RESTRICT` raises error `1451`. | Service layer rejects delete request; returns `400 Bad Request` ("Segment is bound to campaigns"). |

---

## 22. Performance Considerations

1. **Clustered Index Optimization:** Monotonic sequential `BIGINT AUTO_INCREMENT` keys avoid mid-page B+Tree leaf splits during high-volume streaming customer insertion.
2. **Streaming Memory Safety:** File ingestion reads records iteratively via OpenCSV and Apache POI SAX, maintaining constant memory consumption regardless of file size.
3. **Empirical Validation:** Performance optimization relies on query plan analysis (`EXPLAIN FORMAT=JSON`), candidate index tuning, and load testing in Phase 3 rather than unverified numerical assumptions.

---

## 23. Normalization Strategy

The schema implements a **controlled hybrid 3NF model**:
- **Strict 3NF for Relational Domains:** `users`, `customers`, `customer_tags`, `campaigns`, and `campaign_delivery_records` are strictly normalized. Non-key attributes depend directly and exclusively on the primary key.
- **Controlled JSON Hybrid Exception:** `JSON` columns are applied strictly where schema polymorphism and recursive document hierarchy are required:
  - `segments.rules`: Stores recursive Abstract Syntax Trees (ASTs) for dynamic segmentation.
  - `upload_history.error_details`: Stores transient row-level error arrays.
  - `ai_segment_audits.generated_rules`: Stores snapshot of AI-generated AST.

---

## 24. SRS Traceability Matrix

| SRS Requirement ID | Requirement Topic | Target Database Table(s) | Enforced Constraint / Database Role |
| :--- | :--- | :--- | :--- |
| **`FR-CUST-001`** | Customer Record Structure | `customers`, `customer_tags` | Relational columns: `first_name`, `last_name`, `email`, `phone`, `city`, `country`, `total_spend` (`DECIMAL(12,2)`), `visit_count`, `last_active_date`. |
| **`FR-CUST-002`** | Unique Email Enforcement | `customers` | Database constraint: `UNIQUE KEY uq_customers_email (email)`. |
| **`FR-CUST-005`** | Soft Deletion | `customers` | Column: `deleted_at DATETIME(6) NULL`. Preserves FK integrity with delivery records. |
| **`FR-UPLOAD-004`** | Independent Row Validation | `customers`, `upload_history` | Application validates rows; DB enforces data types and constraints. Supports `PARTIAL_SUCCESS`. |
| **`FR-UPLOAD-005`** | Bulk Upload Reporting | `upload_history` | Columns: `total_rows`, `success_count`, `failure_count`, `error_details JSON`. |
| **`FR-UPLOAD-006`** | Bulk Upload History Log | `upload_history` | Persistent log with `file_name`, `uploaded_by`, `status`, `created_at`. |
| **`FR-SEG-001`** | Segment Definition | `segments` | Reusable audience filter linked to creator `users(id)`. |
| **`FR-SEG-002`** | Dynamic Rule Builder | `segments.rules` | MySQL native `JSON` column storing recursive AST. Evaluated via JPA Criteria API. |
| **`FR-AI-SEG-004`** | AI Segmentation Audit | `ai_segment_audits` | Compliance audit table logging `prompt_text`, `generated_rules JSON`, `action_taken`, `user_id`, `segment_id`. |
| **`FR-CAMP-001`** | Campaign Record | `campaigns` | Stores configuration, segment linkage (`segment_id`), base `message_template`, creator ownership. |
| **`FR-CAMP-003`** | Campaign Lifecycle | `campaigns` | Database constraint: `status CHECK (status IN ('DRAFT', 'RUNNING', 'COMPLETED', 'FAILED'))`. |
| **`FR-CAMP-004`** | Campaign-Segment Association | `campaigns.segment_id` | Foreign key referencing `segments(id)` with `ON DELETE RESTRICT`. |
| **`FR-DEL-002`** | Delivery Records | `campaign_delivery_records` | Granular log of recipient messages (`message TEXT`), status, failure diagnostic, and timestamps. |
| **`FR-DEL-005`** | Campaign Completion | `campaigns`, `delivery_records`| Invariant: Campaign completes only when all audience records reach terminal states (`SENT`/`FAILED`). |
| **`FR-SEC-001`** | User Credentials & RBAC | `users` | Secure hashed credentials (`password_hash`), constraint: `role CHECK (role IN ('ROLE_ADMIN', 'ROLE_MARKETER'))`. |
| **`FR-REPORT-001`** | Campaign Statistics | `campaigns`, `delivery_records`| Aggregated from `delivery_records.status` grouped by `campaign_id`; execution bounds via `started_at`, `completed_at`. |
| **`FR-REPORT-003`** | AI Campaign Summary | `campaigns.ai_summary` | Stores the natural-language performance summary generated by Gemini. |
| **`NFR-DATA-001`** | Referential Integrity | All Relational Tables | InnoDB Foreign Key constraints with `RESTRICT` on core business data. |
| **`NFR-DATA-003`** | Audit Timestamps | All 8 Relational Tables | Every table includes `created_at` and `updated_at` (`DATETIME(6) NOT NULL`). |
| **`NFR-DATA-005`** | Sole Persistent Store | MySQL 8.x (InnoDB) | Exclusive persistent engine; Redis is strictly transient. |

---

## 25. Database Decision Register

| Decision Identifier | Topic | Determination | Classification |
| :--- | :--- | :--- | :--- |
| **DBD-01** | Database Engine | **MySQL 8.x (InnoDB)** exclusively. Sole persistent source of truth. | **`[DECIDED]`** |
| **DBD-02** | Primary Key Strategy | **`BIGINT UNSIGNED AUTO_INCREMENT`** for all persistent tables. | **`[DECIDED]`** |
| **DBD-03** | Soft Delete Mechanics | Column `deleted_at DATETIME(6) NULL`. Preserves FK integrity. | **`[DECIDED]`** |
| **DBD-04** | Dynamic Rule Tree Storage | Abstract Syntax Tree persisted as native MySQL **`JSON`** column in `segments.rules`. | **`[DECIDED]`** |
| **DBD-05** | Delivery Record Idempotency | Enforced via `UNIQUE KEY (campaign_id, customer_id)` on `campaign_delivery_records`. | **`[DECIDED]`** |
| **DBD-06** | Customer Tags Architecture | **Normalized child table `customer_tags`** with `UNIQUE KEY (customer_id, tag)`. | **`[RECOMMENDED]`** |
| **DBD-07** | Bulk Ingestion Technology | **Spring JDBC (`JdbcTemplate`)** with batch rewrite for streaming uploads. | **`[DECIDED]`** |
| **DBD-08** | Dynamic Query Engine | **Spring Data JPA Criteria API** compiled in-memory from validated AST JSON. | **`[DECIDED]`** |
| **DBD-09** | AI Prompt Auditing Scope | Dedicated `ai_segment_audits` table; personalization logged directly in delivery records. | **`[DECIDED]`** |
| **DBD-10** | In-Memory / Database Caching | **No Redis caching in v1**; direct indexed MySQL queries exclusively. | **`[OUT OF SCOPE]`** |
| **DBD-11** | Bulk File Size & Row Limits | Operational thresholds deferred to Phase 3 empirical benchmark testing (`OD-UPLOAD-001`). | **`[DEFERRED]`** |
| **DBD-12** | Candidate Performance Indexes | Composite filtering indexes subject to `EXPLAIN` query plan analysis and load testing. | **`[REQUIRES TESTING]`** |
| **DBD-13** | MySQL $\to$ Redis Consistency | MySQL `PENDING` delivery records are the authoritative persistent delivery obligations. If Redis enqueue fails, the record remains `PENDING` and the campaign cannot complete while the obligation remains unresolved. The exact runtime reconciliation/recovery mechanism is `[DEFERRED]` to the Async/Delivery Runtime Design phase. | **`[DECIDED]`** |
| **DBD-14** | Zero-Audience Campaign Launch | Zero-audience segment evaluation rejects dispatch request (`400`/`422`); campaign remains `DRAFT`. | **`[DECIDED]`** |
| **DBD-15** | Timestamp Uniformity | Every table includes `created_at` and `updated_at` (`DATETIME(6) NOT NULL`) per `NFR-DATA-003`; effectively append-only tables populate `updated_at` once. | **`[DECIDED]`** |
| **DBD-16** | Generic `audit_logs` Scope | Generic persistent MySQL `audit_logs` table. | **`[OUT OF SCOPE]`** |
| **DBD-17** | Entity Name Uniqueness Scope | No global database unique constraints on `segments.name` or `campaigns.name`; identity governed by primary key ID. | **`[DECIDED]`** |
| **DBD-18** | Transaction Isolation Level | Standard **`READ COMMITTED`** isolation level for MySQL 8.x / InnoDB. | **`[DECIDED]`** |

---

## 26. Deferred & Testing-Dependent Decisions Summary

1. **Bulk Ingestion File Size and Row Limits (`DBD-11`):** Formally **`[DEFERRED]`** to Phase 3 performance testing (**`[REQUIRES TESTING]`**). The schema imposes no row caps; streaming parsers and JDBC batching will be benchmarked to establish empirical operational thresholds.
2. **Candidate Performance Indexes (`DBD-12`):** Formally classified as **`[REQUIRES TESTING]`**. Candidate composite indexes will be validated using `EXPLAIN FORMAT=JSON` query execution plans during load testing.
3. **Runtime Redis Reconciliation Mechanism (`DBD-13`):** The exact runtime reconciliation/recovery mechanism is formally **`[DEFERRED]`** to the Async/Delivery Runtime Design phase. The database design freezes the persistent invariant that MySQL `PENDING` state governs campaign completion.

---

## 27. Database Design Summary

The database design for **`CS-CRM-2026`** establishes an enterprise-grade, relational-first MySQL 8.x schema engineered for high-concurrency transactional consistency, streaming bulk ingestion, dynamic criteria segmentation, and reliable campaign delivery simulation.

- **Authoritative Integrity:** MySQL 8.x (InnoDB) is the sole persistent store; Redis is strictly transient transport.
- **Relational Rigor:** 8 normalized tables with database-enforced foreign key referential integrity (`RESTRICT`), surrogate `BIGINT AUTO_INCREMENT` clustered primary keys, and soft deletion preserving communication history.
- **Controlled Hybrid JSON:** Native MySQL `JSON` is applied strictly to Abstract Syntax Trees (`segments.rules`, `ai_segment_audits.generated_rules`) and validation error payloads (`upload_history.error_details`), evaluated in Java via Spring Data JPA Criteria API.
- **Operational Robustness:** Strict delivery deduplication via `UNIQUE(campaign_id, customer_id)`, zero-audience launch rejection, and authoritative campaign completion invariants eliminate data inconsistencies across async worker threads.
