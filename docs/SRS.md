# Software Requirements Specification (SRS)

---

| Field              | Value                                      |
|--------------------|---------------------------------------------|
| **Document Title** | Software Requirements Specification         |
| **Project Name**   | Enterprise AI-CRM Platform                  |
| **Project Code**   | CS-CRM-2026                                 |
| **Version**        | 1.1.0                                       |
| **Status**         | Approved — Complete Backend Implementation  |
| **SDLC Phase**     | Phase 3 — Backend Verification              |
| **Prepared By**    | Engineering Team                            |
| **Date**           | 2026-09-23                                  |

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Project Overview](#2-project-overview)
3. [System Scope](#3-system-scope)
4. [Actors and Roles](#4-actors-and-roles)
5. [Functional Requirements](#5-functional-requirements)
   - 5.1 [Customer Management](#51-customer-management)
   - 5.2 [Bulk Data Ingestion](#52-bulk-data-ingestion)
   - 5.3 [Audience Segmentation](#53-audience-segmentation)
   - 5.4 [AI-Powered Segmentation](#54-ai-powered-segmentation)
   - 5.5 [Campaign Management](#55-campaign-management)
   - 5.6 [AI-Powered Campaign Personalization](#56-ai-powered-campaign-personalization)
   - 5.7 [Asynchronous Campaign Delivery Simulation](#57-asynchronous-campaign-delivery-simulation)
   - 5.8 [Authentication and Authorization](#58-authentication-and-authorization)
   - 5.9 [Reporting and AI Summaries](#59-reporting-and-ai-summaries)
   - 5.10 [REST API Requirements](#510-rest-api-requirements)
6. [Non-Functional Requirements](#6-non-functional-requirements)
7. [Technology Stack](#7-technology-stack)
8. [Out-of-Scope Features](#8-out-of-scope-features)
9. [Assumptions and Constraints](#9-assumptions-and-constraints)
10. [Revision History](#10-revision-history)

---

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification (SRS) defines the complete functional and non-functional requirements for the **Enterprise AI-CRM Platform** (Project Code: **CS-CRM-2026**). It serves as the authoritative reference document for all stakeholders — including developers, architects, QA engineers, and project managers — throughout the Software Development Life Cycle (SDLC).

This document governs **SDLC Phase 1: Requirements Analysis**. No source code, database schemas, dependency configurations, or implementation artefacts are produced in this phase.

### 1.2 Scope of This Document

This SRS covers:

- Project purpose and business objectives
- System boundaries and scope
- Actor definitions and role-based access model
- All functional requirements (FR-*) with unique identifiers
- All non-functional requirements (NFR-*) with unique identifiers
- Confirmed technology stack decisions
- Explicit out-of-scope features
- Assumptions and open design decisions deferred to later SDLC phases

### 1.3 Intended Audience

| Audience              | Usage                                                        |
|-----------------------|--------------------------------------------------------------|
| Product Owner         | Validate that requirements reflect business intent           |
| Solution Architect    | Design system architecture aligned with requirements         |
| Backend Engineers     | Implement features against this specification                |
| QA / Test Engineers   | Derive test cases and acceptance criteria                    |
| DevOps Engineers      | Understand infrastructure and operational requirements       |
| Security Engineers    | Identify authentication, authorization and data requirements |

### 1.4 Definitions, Acronyms, and Abbreviations

| Term      | Definition                                                                |
|-----------|---------------------------------------------------------------------------|
| CRM       | Customer Relationship Management                                          |
| AI        | Artificial Intelligence                                                   |
| LLM       | Large Language Model                                                      |
| SRS       | Software Requirements Specification                                       |
| SDLC      | Software Development Life Cycle                                           |
| FR        | Functional Requirement                                                    |
| NFR       | Non-Functional Requirement                                                |
| RBAC      | Role-Based Access Control                                                 |
| JWT       | JSON Web Token                                                            |
| REST      | Representational State Transfer                                           |
| API       | Application Programming Interface                                         |
| CSV       | Comma-Separated Values                                                    |
| XLSX      | Microsoft Excel Open XML Spreadsheet format                               |
| POI       | Apache POI — Java library for Microsoft Office file formats               |
| JPA       | Jakarta Persistence API                                                   |
| JDBC      | Java Database Connectivity                                                |
| ORM       | Object-Relational Mapping                                                 |
| TBD       | To Be Defined (in a subsequent SDLC phase)                                |
| ADMIN     | System Administrator role                                                 |
| MARKETER  | Marketing user role                                                       |
| Segment   | A named group of customers matching a defined rule set                    |
| Campaign  | A targeted communication effort directed at one or more audience segments |
| Delivery  | The simulated dispatch of campaign messages to segment members            |

### 1.5 Document Conventions

- Each requirement is assigned a **unique, stable identifier** in the format `FR-<DOMAIN>-<NNN>` or `NFR-<DOMAIN>-<NNN>`.
- Requirements marked **[OPEN]** are confirmed in scope but whose detailed specification is deferred to the appropriate design phase.
- Priority levels: `P1 - Critical`, `P2 - High`, `P3 - Medium`.

### 1.6 References

| Reference | Description                            |
|-----------|----------------------------------------|
| REF-01    | Spring Boot 3.x Official Documentation |
| REF-02    | Spring AI Documentation                |
| REF-03    | Google Gemini API Documentation        |
| REF-04    | Spring Security 6.x Reference Manual   |
| REF-05    | SpringDoc OpenAPI 2.x Documentation    |
| REF-06    | Apache POI Documentation               |
| REF-07    | Redis Documentation                    |
| REF-08    | MySQL 8.x Reference Manual             |
| REF-09    | Hibernate 6.x User Guide               |
| REF-10    | RFC 7519 — JSON Web Token (JWT)        |

---

## 2. Project Overview

### 2.1 Background

Modern enterprises require CRM systems that go beyond simple contact management. Marketing teams need intelligent tools to understand their customer base, identify meaningful segments, orchestrate personalised campaigns at scale, and measure outcomes — all within a unified, secure platform.

This project delivers an **Enterprise AI-CRM Platform** that integrates traditional CRM capabilities with cutting-edge AI features powered by **Spring AI** and **Google Gemini**, enabling natural-language audience targeting, AI-driven campaign personalisation, and automated reporting summaries.

### 2.2 Business Objectives

| ID     | Objective                                                                                                   |
|--------|-------------------------------------------------------------------------------------------------------------|
| OBJ-01 | Provide a centralised, secure platform for managing enterprise customer data                                |
| OBJ-02 | Reduce time to build audience segments using AI-assisted natural-language rule generation                   |
| OBJ-03 | Enable marketing teams to create, personalise, and deliver campaigns without requiring engineering support   |
| OBJ-04 | Provide measurable campaign analytics with AI-generated plain-language performance summaries                 |
| OBJ-05 | Scale to large customer datasets through efficient bulk ingestion and async processing                       |
| OBJ-06 | Enforce strict role-based access control to protect sensitive customer and campaign data                     |
| OBJ-07 | Deliver a fully documented, testable, and containerised system suitable for enterprise deployment            |

### 2.3 Project Identification

| Field          | Value                       |
|----------------|-----------------------------|
| Project Name   | Enterprise AI-CRM Platform  |
| Project Code   | CS-CRM-2026                 |
| Domain         | CRM / MarTech / AI          |
| Delivery Model | Backend REST API platform   |

---

## 3. System Scope

### 3.1 In Scope

The Enterprise AI-CRM Platform encompasses the following capabilities:

1. **Customer Data Management** — Full lifecycle CRUD operations on customer profiles, including attributes used for segmentation and personalisation.
2. **Bulk Customer Ingestion** — Import of customer records in CSV and XLSX formats with field validation and error reporting.
3. **Audience Segmentation** — Rule-based dynamic audience builder supporting AND/OR logical combinations on customer attributes.
4. **AI-Assisted Segmentation** — Conversion of free-text natural-language descriptions into structured segmentation rule trees via Spring AI + Gemini.
5. **Campaign Management** — Creation, configuration, targeting, and lifecycle management (Create → Configure → Target → Dispatch → Track) of marketing campaigns directed at one or more audience segments. Automated/time-triggered campaign scheduling is not in scope.
6. **AI-Powered Campaign Personalisation** — Generation of context-aware, customer-profile-informed message variations for campaign content using Gemini.
7. **Asynchronous Campaign Delivery Simulation** — Redis-buffered, async worker-based simulation of campaign message dispatch with delivery status tracking.
8. **Authentication and Authorisation** — JWT-based authentication with RBAC enforcing ADMIN and MARKETER access boundaries across all API endpoints.
9. **Reporting and Analytics** — Campaign performance statistics with AI-generated natural-language summary reports.
10. **REST API** — Versioned, documented REST API (`/api/v1/`) with full SpringDoc OpenAPI / Swagger UI integration.

### 3.2 System Boundary

```
+------------------------------------------------------------------+
|                    Enterprise AI-CRM Platform                    |
|                                                                  |
|  +-------------+   +--------------+   +----------------------+  |
|  |  Customer   |   | Segmentation |   |  Campaign            |  |
|  |  Management |   |  Engine      |   |  Management          |  |
|  +-------------+   +--------------+   +----------------------+  |
|  +-------------+   +--------------+   +----------------------+  |
|  |  Bulk       |   |  Spring AI   |   |  Async Delivery      |  |
|  |  Ingestion  |   |  + Gemini    |   |  Simulation (Redis)  |  |
|  +-------------+   +--------------+   +----------------------+  |
|  +-------------+   +--------------+   +----------------------+  |
|  |  Auth/RBAC  |   |  Reporting   |   |  REST API /          |  |
|  |  (JWT)      |   |  + AI Summary|   |  OpenAPI / Swagger   |  |
|  +-------------+   +--------------+   +----------------------+  |
+------------------------------------------------------------------+
         |                    |                    |
    MySQL 8.x            Redis               Gemini API
  (Persistent Store)  (Async Queue)       (AI Provider)
```

### 3.3 Out of Scope

Refer to [Section 8](#8-out-of-scope-features) for a full list of explicitly excluded features.

---

## 4. Actors and Roles

### 4.1 Role Overview

The system implements **Role-Based Access Control (RBAC)** with exactly two defined roles at the requirements level. ADMIN is a superset of MARKETER — an ADMIN can perform all MARKETER actions plus administrative functions.

### 4.2 ADMIN

| Attribute   | Description                                                                                   |
|-------------|-----------------------------------------------------------------------------------------------|
| Role ID     | `ROLE_ADMIN`                                                                                  |
| Description | System administrator responsible for platform configuration and user management               |
| Permissions | Full access to all system features, including user provisioning and all MARKETER capabilities |

**ADMIN Capabilities:**

- Create, update, deactivate, and delete user accounts
- Assign and modify user roles
- Access all customer, segment, campaign, and reporting features
- View system-level audit logs and operational metrics
- Manage platform configuration (where applicable)
- Perform all actions available to MARKETER

### 4.3 MARKETER

| Attribute   | Description                                                                                |
|-------------|--------------------------------------------------------------------------------------------|
| Role ID     | `ROLE_MARKETER`                                                                            |
| Description | Marketing professional responsible for audience management and campaign execution           |
| Permissions | Access to customer data, segmentation, campaign management, AI features, and reporting     |

**MARKETER Capabilities:**

- View, create, update, and manage customer records
- Upload customer data via CSV/XLSX
- Build audience segments using the rule-based segment builder
- Use AI-assisted natural-language segmentation
- Create, configure, and launch campaigns
- Request AI-generated campaign message personalisation
- Trigger campaign delivery simulation
- View campaign analytics and AI-generated reports

### 4.4 Role Capability Matrix

| Capability                       | ADMIN | MARKETER |
|----------------------------------|:-----:|:--------:|
| Manage Users                     |  YES  |    NO    |
| Assign Roles                     |  YES  |    NO    |
| Manage Customers (CRUD)          |  YES  |   YES    |
| Bulk Upload (CSV/XLSX)           |  YES  |   YES    |
| Build Audience Segments          |  YES  |   YES    |
| AI Natural-Language Segmentation |  YES  |   YES    |
| Create & Manage Campaigns        |  YES  |   YES    |
| AI Campaign Personalisation      |  YES  |   YES    |
| Trigger Campaign Delivery        |  YES  |   YES    |
| View Reports & AI Summaries      |  YES  |   YES    |
| View Audit Logs                  |  YES  |    NO    |
| System Configuration             |  YES  |    NO    |

---

## 5. Functional Requirements

---

### 5.1 Customer Management

**Domain Prefix:** `FR-CUST`

Customer management forms the foundational data layer of the CRM. Every other feature — segmentation, campaigns, delivery, and reporting — depends on a well-structured and accurately maintained customer dataset.

---

#### FR-CUST-001 — Customer Record Structure

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

The system shall maintain a customer record consisting of at minimum the following attributes:

| Field             | Type        | Description                              | Required |
|-------------------|-------------|------------------------------------------|----------|
| `customerId`      | UUID / Long | System-generated unique identifier       | Yes      |
| `firstName`       | String      | Customer first name                      | Yes      |
| `lastName`        | String      | Customer last name                       | Yes      |
| `email`           | String      | Unique, validated email address          | Yes      |
| `phone`           | String      | Contact phone number                     | No       |
| `city`            | String      | City of residence                        | No       |
| `country`         | String      | Country of residence                     | No       |
| `totalSpend`      | Decimal     | Cumulative spend amount                  | No       |
| `visitCount`      | Integer     | Number of recorded visits/interactions   | No       |
| `lastActiveDate`  | Date        | Date of most recent activity             | No       |
| `tags`            | List/String | Freeform classification tags             | No       |
| `createdAt`       | Timestamp   | Record creation timestamp (system-set)   | Yes      |
| `updatedAt`       | Timestamp   | Last modification timestamp (system-set) | Yes      |

> Additional fields may be added during System Design to support segmentation rules and personalisation context.

---

#### FR-CUST-002 — Create Customer

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

The system shall provide an API endpoint to create a new customer record.

- The `email` field must be unique; duplicates are rejected with `409 Conflict`.
- All mandatory fields must be validated on input.
- `customerId`, `createdAt`, and `updatedAt` are system-generated and must not be accepted from the client.

---

#### FR-CUST-003 — Retrieve Customer by ID

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

API endpoint to retrieve a single customer by `customerId`. Returns `404 Not Found` for unknown IDs.

---

#### FR-CUST-004 — Update Customer

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

API endpoint supporting PATCH semantics — only supplied fields are modified. `customerId` and `createdAt` are immutable. `updatedAt` is auto-refreshed on every successful update. Email uniqueness is enforced on update.

---

#### FR-CUST-005 — Delete Customer

**Priority:** P2 - High | **Role:** ADMIN only

API endpoint to delete a customer record. Soft delete is preferred to preserve referential integrity with historical campaigns. Returns `404 Not Found` for unknown IDs.

> **[OPEN — Soft delete vs. hard delete strategy will be confirmed during System Design.]**

---

#### FR-CUST-006 — List and Search Customers

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Paginated API endpoint with filtering by `firstName`, `lastName`, `email`, `city`, `country`, `tags` and sorting by `createdAt`, `lastName`, `totalSpend`. Default sort: `createdAt` descending.

---

#### FR-CUST-007 — Customer Count

**Priority:** P3 - Medium | **Role:** ADMIN, MARKETER

API endpoint returning the total count of active customer records in the system.

---

### 5.2 Bulk Data Ingestion

**Domain Prefix:** `FR-UPLOAD`

---

#### FR-UPLOAD-001 — Supported File Formats

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

The system shall support bulk upload of customer records in:

- **CSV** (Comma-Separated Values, UTF-8 encoded) — parsed using a standard Java CSV parsing library (e.g., OpenCSV or equivalent).
- **XLSX** (Microsoft Excel Open XML format) — parsed using **Apache POI**.

Format detection shall be based on file extension and/or MIME type. Each format is routed to its dedicated parser.

---

#### FR-UPLOAD-002 — File Upload API Endpoint

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

A `multipart/form-data` REST endpoint accepting a single CSV or XLSX file per request. The system detects the file format and routes to the appropriate parser. Unsupported types are rejected with `415 Unsupported Media Type`.

---

#### FR-UPLOAD-003 — Column Mapping and Header Validation

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

The first row must be a header row. Missing required columns cause full upload rejection before any rows are processed. Column names are case-insensitive.

> **[OPEN — File size limits and maximum row counts will be defined during System Design and Performance Testing.]**

---

#### FR-UPLOAD-004 — Row-Level Validation

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Each row is individually validated (email format, required fields, data types). Invalid rows are collected and reported without blocking valid rows from being persisted. Duplicate emails are treated as invalid.

---

#### FR-UPLOAD-005 — Upload Result Response

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Structured response including:

- Total rows processed
- Number of rows successfully inserted
- Number of rows skipped / failed
- List of failed row indices with validation error messages

---

#### FR-UPLOAD-006 — Upload History

**Priority:** P3 - Medium | **Role:** ADMIN, MARKETER

The system should maintain a log of all past bulk upload operations (uploader identity, filename, timestamp, success/failure counts, status), accessible via API.

---

### 5.3 Audience Segmentation

**Domain Prefix:** `FR-SEG`

---

#### FR-SEG-001 — Segment Definition

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

A segment is a reusable, named filter that dynamically resolves to the set of customers matching its rule definition.

| Field          | Description                                |
|----------------|--------------------------------------------|
| `segmentId`    | System-generated unique identifier         |
| `name`         | Human-readable segment name                |
| `description`  | Optional description of segment intent     |
| `rules`        | Structured rule tree (AND/OR combinations) |
| `createdBy`    | User who created the segment               |
| `createdAt`    | Timestamp                                  |
| `updatedAt`    | Timestamp                                  |

---

#### FR-SEG-002 — Rule-Based Segment Builder

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Rules operate on customer attributes (e.g., `city`, `totalSpend`, `visitCount`, `lastActiveDate`, `tags`).

Supported operators: `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN_OR_EQUAL`, `CONTAINS`, `IN`, `NOT_IN`.

Rules combine using **AND** (all conditions must match) or **OR** (any condition must match). Nested AND/OR groups are supported. The rule structure is stored in MySQL.

**Example rule (conceptual JSON):**

```json
{
  "operator": "AND",
  "conditions": [
    { "field": "city", "op": "EQUALS", "value": "Mumbai" },
    {
      "operator": "OR",
      "conditions": [
        { "field": "totalSpend", "op": "GREATER_THAN", "value": 10000 },
        { "field": "visitCount", "op": "GREATER_THAN_OR_EQUAL", "value": 5 }
      ]
    }
  ]
}
```

> **[RESOLVED (M6) — Stored as native MySQL JSON column `segments.rules`. Validated in-memory via typed Rule AST and compiled dynamically into JPA Criteria API predicates for database execution.]**

---

#### FR-SEG-003 — Segment CRUD Operations

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Full lifecycle API: create (with rule definition), retrieve by ID, update (name/description/rules), delete, list (paginated).

---

#### FR-SEG-004 — Segment Customer Count Preview

**Priority:** P2 - High | **Role:** ADMIN, MARKETER

API endpoint evaluating a rule tree against the current dataset and returning the matching customer count (dry-run, no persistence). May optionally return a sample customer list.

---

#### FR-SEG-005 — Segment Member Retrieval

**Priority:** P2 - High | **Role:** ADMIN, MARKETER

Paginated API endpoint returning all customers currently matching a saved segment's rules.

---

#### FR-SEG-006 — Segment Reusability

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Segments are reusable across multiple campaigns. A single segment may be associated with one or more campaigns simultaneously.

---

### 5.4 AI-Powered Segmentation

**Domain Prefix:** `FR-AI-SEG`

---

#### FR-AI-SEG-001 — Natural-Language Segment Input

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Accepts a free-text natural-language audience description and returns a structured rule tree equivalent to a manually built segment.

- **Input:** Plain-text prompt (e.g., *Customers from Delhi who spent over 5,000 and visited at least 3 times in the last 6 months*)
- **Output:** Structured rule tree conforming to the FR-SEG-002 schema

---

#### FR-AI-SEG-002 — Spring AI + Gemini Integration

**Priority:** P1 - Critical | **Role:** System (Internal)

Uses **Spring AI** to interface with **Google Gemini**. A prompt engineering strategy constructs the system and user prompts. The Gemini response is parsed and validated against the rule schema. Invalid/unparseable AI responses are handled gracefully with appropriate error responses.

> **[OPEN — Prompt design, output parsing strategy, and error handling will be detailed during System Design.]**

---

#### FR-AI-SEG-003 — Save AI-Generated Segment

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

After receiving an AI-generated rule tree, the user may:

1. **Preview** the rule tree and estimated customer count (invokes FR-SEG-004).
2. **Save** the segment as-is, or modify the rules before saving.

Saved AI-generated segments use the same data model as manually built segments.

---

#### FR-AI-SEG-004 — AI Segmentation Audit Trail

**Priority:** P2 - High | **Role:** System (Internal)

Each AI segmentation request is recorded: original natural-language input, Gemini-generated output, and user action taken (saved/discarded). Supports transparency, debugging, and future model quality evaluation.

---

### 5.5 Campaign Management

**Domain Prefix:** `FR-CAMP`

---

#### FR-CAMP-001 — Campaign Record Structure

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

| Field               | Type      | Description                                              | Required |
|---------------------|-----------|----------------------------------------------------------|----------|
| `campaignId`        | UUID/Long | System-generated unique identifier                       | Yes      |
| `name`              | String    | Campaign name                                            | Yes      |
| `description`       | String    | Campaign description                                     | No       |
| `segmentId`         | Reference | Target audience segment                          | Yes      |
| `messageTemplate`   | String    | Base message template (may contain placeholders) | Yes      |
| `status`            | Enum      | DRAFT, RUNNING, COMPLETED, FAILED                | Yes      |
| `createdBy`         | Reference | User who created the campaign                    | Yes      |
| `createdAt`         | Timestamp | Creation timestamp                               | Yes      |
| `updatedAt`         | Timestamp | Last modification timestamp                      | Yes      |

---

#### FR-CAMP-002 — Campaign CRUD Operations

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Full lifecycle API: create (initial status: DRAFT), retrieve by ID, update (DRAFT only), delete (DRAFT only), list (paginated, filterable by status).

---

#### FR-CAMP-003 — Campaign Status Lifecycle

**Priority:** P1 - Critical | **Role:** System (Internal)

Enforced status transitions:

```
DRAFT --> RUNNING --> COMPLETED
                \--> FAILED
```

All campaign launches are immediate (DRAFT -> RUNNING). Campaigns in RUNNING, COMPLETED, or FAILED status are not editable. Transitions are triggered by explicit API actions or internal delivery processing.

---

#### FR-CAMP-004 — Campaign-Segment Association

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Each campaign is associated with exactly one audience segment. Segment rules are evaluated at launch time to determine the current audience.

---

#### FR-CAMP-005 — Campaign Launch

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

API endpoint to launch a campaign (DRAFT -> RUNNING). On launch: evaluate the segment, enqueue delivery jobs in Redis, and immediately transition status to RUNNING (non-blocking response).

---

### 5.6 AI-Powered Campaign Personalization

**Domain Prefix:** `FR-AI-CAMP`

---

#### FR-AI-CAMP-001 — Context-Aware Message Personalization

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Uses **Spring AI + Gemini** to generate context-aware, personalised message variations using the campaign base message template and relevant customer profile attributes as input.

- **Input:** Campaign base message template + customer profile attributes
- **Output:** Personalised message variation tailored to the customer's context

> **[OPEN — Batching strategy, API-call optimisation (e.g., per-customer vs. per-cohort), and prompt design will be defined during System Design to balance quality, cost, and performance.]**

---

#### FR-AI-CAMP-002 — Personalization Integration with Delivery

**Priority:** P1 - Critical | **Role:** System (Internal)

AI-generated personalised messages are used as the delivery content. Each delivery record stores the final personalised message. On AI failure for a specific customer, the system falls back to the base template and logs the failure.

---

#### FR-AI-CAMP-003 — Personalization Opt-Out per Campaign

**Priority:** P3 - Medium | **Role:** ADMIN, MARKETER

Per-campaign flag to enable or disable AI personalisation. When disabled, the base message template is used for all customers in the segment.

---

### 5.7 Asynchronous Campaign Delivery Simulation

**Domain Prefix:** `FR-DEL`

---

#### FR-DEL-001 — Asynchronous Delivery Architecture

**Priority:** P1 - Critical | **Role:** System (Internal)

Campaign delivery is performed **asynchronously** via a **Redis-buffered queue**. Customer delivery jobs are enqueued in Redis and consumed by background worker components. The campaign launch API returns immediately after enqueueing (non-blocking). Redis holds only transient, in-flight task data; MySQL is the persistent record of all delivery outcomes.

> **[OPEN — Specific Redis data structure (e.g., List, Stream), worker concurrency model, and retry/backoff strategy will be defined during System Design.]**

---

#### FR-DEL-002 — Delivery Record

**Priority:** P1 - Critical | **Role:** System (Internal)

For each campaign-customer pair, a delivery log record is persisted in MySQL:

| Field            | Type      | Description                                    |
|------------------|-----------|------------------------------------------------|
| `deliveryId`     | UUID/Long | Unique identifier                              |
| `campaignId`     | Reference | Associated campaign                            |
| `customerId`     | Reference | Target customer                                |
| `message`        | String    | Final message (personalised or base template)  |
| `status`         | Enum      | PENDING, SENT, FAILED                          |
| `failureReason`  | String    | Reason for failure (if applicable)             |
| `processedAt`    | Timestamp | Timestamp when delivery was processed          |

---

#### FR-DEL-003 — Delivery Simulation Logic

**Priority:** P1 - Critical | **Role:** System (Internal)

Delivery is **simulated** (no real email/SMS integration). Workers mark records SENT or FAILED based on a configurable probabilistic success rate set via application properties. Simulation is clearly identified in system logs.

---

#### FR-DEL-004 — Delivery Status Tracking

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

API endpoints providing:

- Aggregated delivery stats per campaign (total sent, failed, pending)
- Paginated individual delivery records for a campaign, filterable by status

---

#### FR-DEL-005 — Campaign Completion Detection

**Priority:** P1 - Critical | **Role:** System (Internal)

The system auto-transitions campaign status to COMPLETED when all delivery jobs are processed (even if some individual deliveries failed). Transitions to FAILED on an unrecoverable delivery process error.

---

### 5.8 Authentication and Authorization

**Domain Prefix:** `FR-SEC`

---

#### FR-SEC-001 — User Registration (ADMIN only)

**Priority:** P1 - Critical | **Role:** ADMIN

Only ADMIN may create new user accounts. Fields: username, email, initial password, role (ROLE_ADMIN or ROLE_MARKETER). Passwords must be hashed — never stored in plaintext.

---

#### FR-SEC-002 — User Login

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Public (unauthenticated) API endpoint accepting username/email in request field `"username"` and password. On success: issues JWT access token (refresh tokens are omitted in M4 per the frozen Security Baseline; access token lifetime is 1 hour). On failure: `401 Unauthorized` with a generic message (do not reveal whether username or password was wrong).

---

#### FR-SEC-003 — JWT-Based Authentication

**Priority:** P1 - Critical | **Role:** System (Internal)

All protected endpoints require `Authorization: Bearer <token>`. Requests with missing or expired tokens receive `401 Unauthorized`.

> **[RESOLVED & FROZEN in M4 Security Baseline: HS256 algorithm, 1-hour access token lifetime, refresh tokens omitted in M4, live database active & role authority check; zero Redis token denylist.]**

---

#### FR-SEC-004 — Role-Based Access Control (RBAC)

**Priority:** P1 - Critical | **Role:** System (Internal)

Access to each endpoint is restricted by role per the Role Capability Matrix (Section 4.4). A MARKETER accessing ADMIN-only endpoints receives `403 Forbidden`. The authoritative role is loaded from MySQL on each request; the role embedded in the JWT payload is diagnostic.

---

#### FR-SEC-005 — Password Management

**Priority:** P2 - High | **Role:** ADMIN

Password updates are administered via the ADMIN-only endpoint `PATCH /api/v1/users/{id}/password`. Passwords must satisfy: minimum 8 characters, maximum 72 characters, maximum 72 UTF-8 bytes, hashed with BCrypt strength 12. Password updates do not revoke existing JWTs (they naturally expire after 1 hour).

---

#### FR-SEC-006 — User Management (ADMIN)

**Priority:** P2 - High | **Role:** ADMIN

ADMIN can: list all users (paginated), retrieve user by ID, update user role, deactivate a user account, reset user password. Initial admin is provisioned via automatic bootstrap when `userRepository.count() == 0`.

---

### 5.9 Reporting and AI Summaries

**Domain Prefix:** `FR-REPORT`

---

#### FR-REPORT-001 — Campaign Performance Statistics

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

API endpoint returning per-campaign performance data:

- `campaignId`, `campaignName`
- `audienceSize` — customers targeted
- `sentCount`, `failedCount`, `pendingCount`
- `deliveryRate` — percentage successfully delivered
- `campaignStatus`, `startedAt`, `completedAt`

---

#### FR-REPORT-002 — Platform-Wide Customer Statistics

**Priority:** P2 - High | **Role:** ADMIN, MARKETER

API endpoint returning aggregate customer stats:

- Total customer count
- Customers added in the last 30 days
- Top cities / countries by customer count
- Average total spend across all customers

---

#### FR-REPORT-003 — AI-Generated Campaign Summary

**Priority:** P1 - Critical | **Role:** ADMIN, MARKETER

Uses **Spring AI + Gemini** to generate a natural-language performance summary for a campaign using its statistics as input. Output is a human-readable paragraph. The summary is persisted in MySQL and returned in the response. Regeneration (overwriting the previous summary) is supported.

---

#### FR-REPORT-004 — Campaign History

**Priority:** P2 - High | **Role:** ADMIN, MARKETER

Paginated, sortable API endpoint listing all campaigns (all statuses) with key statistics in a summary view.

---

### 5.10 REST API Requirements

**Domain Prefix:** `FR-API`

---

#### FR-API-001 — API Versioning

**Priority:** P1 - Critical | **Role:** System (Internal)

All REST API endpoints are versioned under the `/api/v1/` base path.

---

#### FR-API-002 — Consistent Response Structure

**Priority:** P1 - Critical | **Role:** System (Internal)

All responses conform to a consistent JSON response envelope:

- `status` — HTTP status code or success/error indicator
- `message` — Human-readable message
- `data` — Response payload (null on error)
- `errors` — Validation/processing errors (null on success)
- `timestamp` — Response generation timestamp

---

#### FR-API-003 — HTTP Method and Status Code Conventions

**Priority:** P1 - Critical | **Role:** System (Internal)

| Operation     | HTTP Method | Success Status |
|---------------|-------------|----------------|
| Create        | POST        | 201 Created    |
| Retrieve      | GET         | 200 OK         |
| Update (full) | PUT         | 200 OK         |
| Update (part) | PATCH       | 200 OK         |
| Delete        | DELETE      | 204 No Content |

---

#### FR-API-004 — Input Validation and Error Responses

**Priority:** P1 - Critical | **Role:** System (Internal)

Validation errors return `400 Bad Request` with a structured list of field-level error details.

---

#### FR-API-005 — OpenAPI / Swagger Documentation

**Priority:** P1 - Critical | **Role:** System (Internal)

Auto-generated OpenAPI documentation via **SpringDoc OpenAPI**, accessible at:

- **Swagger UI:** `/swagger-ui.html`
- **OpenAPI JSON/YAML spec:** `/v3/api-docs`

---

#### FR-API-006 — Global Exception Handling

**Priority:** P1 - Critical | **Role:** System (Internal)

A global exception handler intercepts all unhandled exceptions and returns structured error responses per FR-API-002. No raw stack traces are exposed in API responses.

---

## 6. Non-Functional Requirements

---

### 6.1 Performance

**Domain Prefix:** `NFR-PERF`

#### NFR-PERF-001 — API Response Time

Standard CRM CRUD operations shall respond within **500 milliseconds** under normal operating load.

#### NFR-PERF-002 — Bulk Upload Processing

Throughput and time bounds for bulk upload shall be defined during **System Design and Performance Testing** once file size and row count limits are established.

#### NFR-PERF-003 — Segment Evaluation

Segment rule evaluation shall complete within a threshold acceptable for interactive use. Specific thresholds defined during **System Design** based on expected dataset sizes.

#### NFR-PERF-004 — Asynchronous Delivery Throughput

Campaign delivery simulation throughput (messages per second) shall be defined during **System Design and Performance Testing**.

---

### 6.2 Scalability

**Domain Prefix:** `NFR-SCALE`

#### NFR-SCALE-001 — Customer Dataset Scale

The system shall accommodate at least **1,000,000 customer records** without degradation of core functionality.

#### NFR-SCALE-002 — Concurrent Users

The system shall support at least **50 concurrent authenticated users** without significant performance degradation.

#### NFR-SCALE-003 — Horizontal Scalability Design

The application layer shall be stateless (JWT auth + Redis for transient state), enabling horizontal scaling by adding instances behind a load balancer.

#### NFR-SCALE-004 — Campaign Size

The delivery simulation subsystem shall be capable of processing campaigns targeting up to **500,000 customers** per campaign via the Redis-buffered async queue.

---

### 6.3 Security

**Domain Prefix:** `NFR-SEC`

#### NFR-SEC-001 — Authentication Enforcement

All non-public API endpoints must be protected by JWT authentication. No authenticated resource is accessible without a valid, unexpired token.

#### NFR-SEC-002 — Password Storage

User passwords must be stored using a strong, adaptive one-way hashing algorithm (e.g., BCrypt). Plaintext passwords must never be persisted or logged.

#### NFR-SEC-003 — Sensitive Data in Logs

No sensitive data (passwords, JWT tokens, customer PII beyond identifiers) must appear in application logs.

#### NFR-SEC-004 — SQL Injection Prevention

All database queries must use parameterised queries or JPA/Hibernate named parameters. Dynamic query string construction with user input is prohibited.

#### NFR-SEC-005 — Input Sanitisation

All user-supplied inputs must be validated and sanitised before processing to prevent injection attacks.

#### NFR-SEC-006 — HTTPS Enforcement

In production deployment, all API traffic must be served over HTTPS/TLS.

#### NFR-SEC-007 — Principle of Least Privilege

Each role must only have access to the minimum set of endpoints and data required to fulfil its responsibilities, per Section 4.4.

---

### 6.4 Reliability

**Domain Prefix:** `NFR-REL`

#### NFR-REL-001 — Async Delivery Resilience

The Redis-based delivery queue must handle temporary worker failures gracefully. In-flight delivery jobs must not be permanently lost due to a worker crash.

> **[OPEN — Retry policy, dead-letter handling, and at-least-once delivery guarantees will be defined during System Design.]**

#### NFR-REL-002 — Bulk Upload Atomicity per Row

Failure to insert one row during bulk upload must not roll back or affect the insertion of other valid rows. Each row is an independent unit.

#### NFR-REL-003 — AI Service Unavailability Handling

If the Gemini API is unavailable or returns an error:

- AI segmentation requests return a clear error response.
- AI campaign personalisation falls back to the base template (per FR-AI-CAMP-002).
- AI report summary requests return a clear error response.
- Core CRM functionality (CRUD, non-AI segmentation, non-AI campaigns) remains fully operational.

#### NFR-REL-004 — Database Connection Resilience

The system must use connection pooling and handle transient database connectivity issues with appropriate retry logic.

---

### 6.5 Maintainability

**Domain Prefix:** `NFR-MAINT`

#### NFR-MAINT-001 — Layered Architecture

The codebase must follow a clear layered architecture (Controller -> Service -> Repository) with well-defined boundaries between layers.

#### NFR-MAINT-002 — Code Standards

All code must follow established Java coding conventions with a consistent code style enforced via tooling (e.g., Checkstyle).

#### NFR-MAINT-003 — Logging

Structured, levelled logging (DEBUG, INFO, WARN, ERROR) must be implemented using SLF4J. Key business events (login, upload, campaign launch, delivery processing) must be logged at INFO level.

#### NFR-MAINT-004 — Configuration Externalisation

All environment-specific configuration (database credentials, Redis connection, Gemini API key, JWT secrets) must be externalised via environment variables or Spring profiles. No secrets hard-coded in source code.

#### NFR-MAINT-005 — Containerisation

The application must be deployable via **Docker** and **Docker Compose**, covering the full local development stack: application, MySQL 8.x, and Redis.

---

### 6.6 Testability

**Domain Prefix:** `NFR-TEST`

#### NFR-TEST-001 — Unit Testing

All service-layer business logic must have unit tests using **JUnit 5** and **Mockito**. Minimum coverage threshold defined during System Design.

#### NFR-TEST-002 — Integration Testing

Integration tests for API endpoints covering happy path and error scenarios using **Spring Boot Test** and an in-memory or test-container database.

#### NFR-TEST-003 — Test Independence

All tests must be independent, repeatable, and must not depend on external services (Gemini API, production Redis, production MySQL) — these must be mocked or stubbed.

#### NFR-TEST-004 — CI Compatibility

The test suite must be executable via `mvn test` and compatible with standard CI/CD pipeline execution.

---

### 6.7 Data Integrity

**Domain Prefix:** `NFR-DATA`

#### NFR-DATA-001 — Referential Integrity

Foreign key relationships (campaign -> segment, delivery -> campaign, delivery -> customer) must be enforced at the database level using MySQL foreign key constraints.

#### NFR-DATA-002 — Data Validation

All data entering the system (via API or bulk upload) must be validated before persistence. Invalid data is rejected with descriptive error messages.

#### NFR-DATA-003 — Audit Timestamps

All persistent entities must carry `createdAt` and `updatedAt` timestamps, automatically managed by the persistence layer.

#### NFR-DATA-004 — Unique Constraints

Business-level uniqueness rules (e.g., unique customer email, unique user email/username) must be enforced at both the application validation layer and the database constraint level.

#### NFR-DATA-005 — MySQL as Single Persistent Data Store

**MySQL 8.x is the sole persistent data store.** No other relational database shall be used. Redis is used exclusively for transient, in-flight data and asynchronous queuing.

---

### 6.8 API Documentation

**Domain Prefix:** `NFR-DOC`

#### NFR-DOC-001 — OpenAPI Specification

The system must auto-generate a complete and up-to-date OpenAPI 3.x specification for all API endpoints using **SpringDoc OpenAPI**.

#### NFR-DOC-002 — Swagger UI

Swagger UI must be available in development and staging environments for interactive API exploration and testing.

#### NFR-DOC-003 — Request/Response Examples

All API endpoints in the OpenAPI spec must include representative request and response examples.

#### NFR-DOC-004 — Authentication Documentation in Swagger

The OpenAPI spec must document the JWT Bearer token requirement. Swagger UI must provide an Authorise mechanism for supplying the token during manual testing.

---

## 7. Technology Stack

The following technology decisions are **confirmed and fixed** for this project. No alternatives shall be introduced without explicit approval.

### 7.1 Core Platform

| Category    | Technology  | Version / Notes         |
|-------------|-------------|-------------------------|
| Language    | Java        | **Java 21 LTS**         |
| Framework   | Spring Boot | **3.x** (latest stable) |
| Build Tool  | Maven       | Latest stable           |

### 7.2 Data Layer

| Category              | Technology  | Version / Notes                           |
|-----------------------|-------------|-------------------------------------------|
| Relational Database   | **MySQL**   | **8.x ONLY** — sole persistent data store |
| Persistence API       | JPA         | Via Spring Data JPA                       |
| Database Connectivity | JDBC        | Managed by Spring Boot                    |
| ORM                   | Hibernate   | **6.x** (managed via Spring Boot 3.x)     |
| Cache / Queue         | Redis       | Async buffering and transient data only   |

> **WARNING: PostgreSQL is NOT used in this project and must NOT be introduced at any stage.**

### 7.3 Security

| Category           | Technology            | Version / Notes           |
|--------------------|------------------------|---------------------------|
| Security Framework | Spring Security        | **6.x**                   |
| Authentication     | JWT (JSON Web Tokens)  | RFC 7519 compliant        |
| Authorisation      | RBAC                   | ROLE_ADMIN, ROLE_MARKETER |

### 7.4 AI Integration

| Category    | Technology    | Notes                                        |
|-------------|---------------|----------------------------------------------|
| AI Framework| Spring AI     | Spring Boot integration for LLM interactions |
| AI Provider | Google Gemini | LLM for segmentation and personalisation     |

### 7.5 File Processing

| Category    | Technology | Notes                               |
|-------------|------------|-------------------------------------|
| XLSX Parsing | Apache POI                            | Parses XLSX (Microsoft Excel Open XML) files for bulk upload |
| CSV Parsing  | OpenCSV or equivalent Java CSV library| Parses CSV (UTF-8) files for bulk upload; specific library TBD during System Design |

### 7.6 API Documentation

| Category          | Technology        | Notes                           |
|-------------------|-------------------|---------------------------------|
| API Documentation | SpringDoc OpenAPI | Auto-generates OpenAPI 3.x spec |
| Swagger UI        | Via SpringDoc     | Interactive API explorer        |

### 7.7 Testing

| Category     | Technology | Notes                                   |
|--------------|------------|-----------------------------------------|
| Unit Testing | JUnit 5    | Test framework                          |
| Mocking      | Mockito    | Mock/stub for unit and integration tests|

### 7.8 DevOps and Infrastructure

| Category         | Technology     | Notes                                  |
|------------------|----------------|----------------------------------------|
| Containerisation | Docker         | Application and dependency containers  |
| Orchestration    | Docker Compose | Local dev stack: app + MySQL + Redis   |
| Version Control  | Git            | Source code management                 |
| Repository       | GitHub         | Remote repository and collaboration    |

---

## 8. Out-of-Scope Features

The following features are explicitly **excluded** from scope. Any requests to implement these require formal scope change approval.

| #  | Feature                         | Reason for Exclusion                                                  |
|----|---------------------------------|-----------------------------------------------------------------------|
| 1  | Real Email Delivery             | Simulated only; no SMTP/SES/Mailgun integration                       |
| 2  | Real SMS / Push Notifications   | Only delivery simulation is in scope                                  |
| 3  | Mobile Application              | Backend REST API only; no iOS/Android app                             |
| 4  | Separate Frontend Framework     | No React, Angular, or Vue frontend is in current scope. REST API + OpenAPI/Swagger UI is the primary interface. A lightweight Thymeleaf view layer may be considered in a future phase only if explicitly required. |
| 5  | Payment / Billing               | No billing, subscription, or payment processing                       |
| 6  | Full Multi-Tenant Provisioning  | Multi-tenant organisation provisioning and management are out of current scope. The architecture considers user-level and data-level isolation; full multi-tenant provisioning is deferred and may be addressed in a future phase. |
| 7  | Social Media Integration        | No Twitter, Facebook, LinkedIn API integrations                       |
| 8  | Third-Party CRM Sync            | No Salesforce, HubSpot, or other CRM integration                      |
| 9  | PostgreSQL or other RDBMS       | MySQL 8.x is the sole persistent store; no alternative databases      |
| 10 | A/B Testing                     | No split-test campaign functionality                                  |
| 11 | Real-Time Analytics Dashboard   | No WebSocket / real-time push; REST polling only                      |
| 12 | Customer Self-Service Portal    | No customer-facing login or profile management                        |
| 13 | Automated Campaign Scheduling   | No cron-based or time-triggered campaign auto-launch                  |
| 14 | Unsubscribe / Preference Centre | No customer opt-out or communication preference management            |
| 15 | Data Export                     | No bulk export beyond API retrieval                                   |

---

## 9. Assumptions and Constraints

### 9.1 Confirmed Assumptions

| ID   | Assumption                                                                                                                                             |
|------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| A-01 | **MySQL 8.x is the sole persistent data store.** Redis is used exclusively for transient, asynchronous buffering and queueing.                         |
| A-02 | **RBAC has exactly two roles:** ROLE_ADMIN and ROLE_MARKETER. ADMIN is a superset of MARKETER.                                                         |
| A-03 | **Campaign delivery is simulated.** No real email, SMS, or push integration. Simulation marks delivery records as SENT/FAILED in MySQL.                |
| A-04 | **AI segmentation produces a structured rule tree** semantically identical to a manually built segment and stored in the same data model.              |
| A-05 | **The Gemini API is accessed via Spring AI** as the abstraction layer, enabling potential provider substitution in future.                              |
| A-06 | **Docker Compose** covers the complete local development environment: Spring Boot application, MySQL 8.x, and Redis.                                   |
| A-07 | **SpringDoc OpenAPI (v2.x, compatible with Spring Boot 3.x)** is used for API documentation.                                                          |
| A-08 | **This SRS is versioned.** All changes must be recorded in the Revision History (Section 10) and approved before implementation.                         |

### 9.2 Open Design Decisions (Deferred to Later SDLC Phases)

| ID    | Topic                           | Deferred To                          | Notes                                                                                             |
|-------|---------------------------------|--------------------------------------|---------------------------------------------------------------------------------------------------|
| OD-01 | AI Campaign Personalisation     | System Design                        | Batching strategy, API-call optimisation, and prompt design will be defined during System Design.  |
| OD-02 | Bulk Upload File Limits         | System Design & Performance Testing  | Maximum file size and row count limits established after performance testing.                      |
| OD-03 | JWT Token Lifetime & Refresh    | Security Design                      | Resolved & Frozen in M4: 1-hour access token lifetime, refresh tokens omitted in M4, live DB active & role authority check. |
| OD-04 | Redis Data Structure & Workers  | System Design                        | Specific Redis data structure, worker concurrency, and retry/backoff strategy to be designed.     |
| OD-05 | Soft Delete vs. Hard Delete     | System Design                        | Customer deletion strategy to be confirmed, considering referential integrity.                     |
| OD-06 | Minimum Test Coverage Threshold | System Design                        | Specific unit test coverage percentage target to be defined.                                      |
| OD-07 | Segment Rule Storage Format     | System Design                        | MySQL storage format for rule trees (JSON column vs. normalised tables) to be decided.            |
| OD-08 | Password Complexity Policy      | Security Design                      | Resolved & Frozen in M4: Minimum 8 characters, maximum 72 characters, maximum 72 UTF-8 bytes, BCrypt strength 12. |
| OD-09 | AI Delivery Retry / Dead-Letter | System Design                        | Retry policy and dead-letter handling for the async delivery queue to be specified.               |
| OD-10 | Performance Thresholds          | System Design & Performance Testing  | Specific NFR-PERF thresholds for bulk upload, segment evaluation, and delivery throughput.        |

### 9.3 Constraints

| ID   | Constraint                                                                                                               |
|------|--------------------------------------------------------------------------------------------------------------------------|
| C-01 | The system must use **Java 21 LTS** only. No earlier Java versions.                                                      |
| C-02 | The system must use **MySQL 8.x** as the only persistent database. PostgreSQL and other RDBMS are prohibited.            |
| C-03 | All AI features depend on the **Google Gemini API**. Core CRM functionality must operate independently of AI services.   |
| C-04 | The project follows a strict SDLC. No source code, schemas, or dependencies are to be created outside their phase.       |

---

## 10. Revision History

| Version | Date       | Author           | Description                                                                 | Status   |
|---------|------------|------------------|-----------------------------------------------------------------------------|----------|
| 1.0.0   | 2026-09-18 | Engineering Team | Initial SRS — Baseline requirements                                         | Approved |
| 1.0.1   | 2026-09-18 | Engineering Team | Scope-consistency corrections: Multi-Tenancy, Campaign Scheduling, Frontend | Approved |
| 1.0.2   | 2026-09-20 | Engineering Team | M4 Security Baseline Reconciliation: Resolved OD-03 (1h token lifetime, no refresh tokens) and OD-08 (password policy 8-72 chars / 72 UTF-8 bytes, BCrypt strength 12) per frozen Security Baseline. | Approved |
| 1.1.0   | 2026-09-23 | Engineering Team | M7-M12 Backend Completion: Streaming bulk upload (CSV/XLSX), Redis Streams delivery queue, concurrent campaign launch with pessimistic write locks, Spring AI Gemini natural language segment rule generation & narrative summaries, comprehensive reporting APIs, SLF4J/MDC request tracing, OpenAPI documentation, and production Docker containerization verified with 340 tests. | Approved |
| 1.2.0   | 2026-09-24 | Engineering Team | Backend Hardening Pass: Transactional Outbox pattern (`campaign_delivery_outbox`) for MySQL ↔ Redis atomicity, Redis Streams PEL stale message recovery & safe MINID trimming, DeliveryProvider abstraction with deterministic idempotency keys, deterministic email uniqueness & soft-delete duplicate rejection semantics, chunked audience materialization (500/page), AI fallback indicators (`isFallback`) & strict 503 unavailability for campaign narrative summaries, asynchronous correlation ID propagation via SLF4J MDC, database query index optimizations, and expanded test suite to 352 passing tests (0 failures, 0 errors, 0 skipped). | Approved |

---

*End of Software Requirements Specification — CS-CRM-2026 v1.2.0*

