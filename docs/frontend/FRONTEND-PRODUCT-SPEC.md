# Enterprise AI-CRM Platform (`CS-CRM-2026`)
# Frontend Product Specification
# Document Version: 2.0.0 — Status: Approved Verified Baseline

---

## 1. Executive Summary & Purpose

The **Enterprise AI-CRM Platform (`CS-CRM-2026`)** is an enterprise-grade customer relationship management and intelligent campaign orchestration platform. The backend is complete, verified, and frozen (Java 21, Spring Boot 3.3.3, Spring Security 6.x, MySQL 8.4, Redis 7.x Streams, Spring AI with Google Gemini, JWT-based RESTful APIs).

This document establishes the **Frontend Product Specification** for a professional **Java-based server-side web frontend using Vaadin Flow 24.4.x**. The project does not use a separate React, Next.js, Vite, Angular, or Vue SPA.

### 1.1 Non-Negotiable Frontend Constraints
1. **Framework & Architecture:** Built in Java 21 using **Vaadin Flow**, utilizing server-side Java component definitions, server-side UI state, and type-safe routing.
2. **No Separate JavaScript SPA:** No React, Next.js, Vite, Angular, or Vue. Browser-side rendering is handled by the Vaadin Flow server-side engine.
3. **Backend Protection & Freeze:** The backend is 100% frozen. No backend refactoring, no Spring Boot upgrades (frozen at 3.3.3), and no schema modifications.
4. **Mandatory REST Boundary:** The frontend interacts with the platform strictly by consuming the authoritative REST API (`/api/v1/*`) using bearer JWT authentication. Direct injection of backend repositories or services is strictly prohibited.
5. **Aesthetics & Ergonomics:** Designed as a serious, dense, utilitarian business software system. Muted graphite/charcoal foundations, warm amber accents, compact enterprise spacing, high data density, and zero generic "AI dashboard" templates.

---

## 2. Target Personas & User Roles

The platform enforces strict Role-Based Access Control (RBAC) with a hierarchical role relationship: `ROLE_ADMIN > ROLE_MARKETER`.

```
               +-------------------------------------------+
               |                 ROLE_ADMIN                |
               | (System Administration & Full Governance) |
               +-------------------------------------------+
                                     |
                                     v inherits all permissions
               +-------------------------------------------+
               |               ROLE_MARKETER               |
               |   (Audience, Campaigns, Data Ingestion)   |
               +-------------------------------------------+
```

### 2.1 Persona 1: Sarah — Senior Growth Marketer (`ROLE_MARKETER`)
- **Profile:** Responsible for managing customer records, building targeted audience segments, launching marketing campaigns, and inspecting campaign outcomes.
- **Key Goals:**
  - Import customer cohorts from spreadsheets (CSV/XLSX) without engineering assistance.
  - Construct dynamic audience segments using visual logic or natural-language AI prompts.
  - Review accurate recipient counts and member rosters before sending campaigns.
  - Author campaign messages with personalized placeholders (e.g., `{firstName}`, `{city}`).
  - Launch campaigns with safety confirmations and monitor delivery progress via near-real-time polling.
  - Review campaign delivery metrics (sent, failed, delivery rate) and on-demand AI executive summaries.
- **Pain Points Solved by UI:**
  - Eliminates reliance on SQL or IT tickets to query customer databases.
  - Prevents erroneous campaign launches with zero-audience validation and confirmation dialogs.
  - Replaces manual JSON rule authoring with an intuitive visual query builder.
  - Surfaces row-level ingestion errors during bulk uploads.

### 2.2 Persona 2: Marcus — Platform Administrator (`ROLE_ADMIN`)
- **Profile:** System administrator and IT operations lead responsible for platform security, user provisioning, access control, audit compliance, and system health.
- **Key Goals:**
  - Provision and manage staff user accounts, assign roles (`ROLE_MARKETER` vs `ROLE_ADMIN`), reset credentials, and deactivate departing employees.
  - Manage customer data and perform soft deletion when required.
  - Inspect AI segment generation audit logs to review user prompts, generated rules, and user actions.
  - Monitor asynchronous campaign deliveries and infrastructure throughput.
- **Pain Points Solved by UI:**
  - Clear administrative segregation: admin controls are completely invisible and inaccessible to marketers.
  - One-click account deactivation that immediately invalidates user access.
  - Centralized visibility into AI usage audits.

---

## 3. End-to-End User Journeys & Primary Workflows

### 3.1 Journey 1: Bulk Customer Ingestion & Data Hygiene
```
[ Marketer / Admin ]
        |
        v
1. Navigate to "Data Ingestion" (/uploads)
        |
        v
2. Select or drag-and-drop CSV or XLSX file (max 10MB)
        |
        v
3. Client-side file extension check (.csv, .xlsx)
        |
        v
4. Submit to POST /api/v1/uploads/bulk (MultipartFile)
        |
        v
5. Server performs transactional row-by-row parsing & validation:
   ├── Valid rows inserted/updated in MySQL
   └── Invalid rows recorded with row number, email & failure reason
        |
        v
6. UI receives UploadResultResponse (SUCCESS, PARTIAL_SUCCESS, FAILED)
        |
        v
7. UI displays Import Summary Card:
   - Total Rows Processed (`totalRecords`)
   - Successful Records Count (`successfulRecords`)
   - Failed Records Count (`failedRecords`)
   - Searchable/Sortable Error Detail Grid (`row`, `email`, `reason`)
        |
        v
8. Historical audit row appended to Upload History Grid (/api/v1/uploads/history)
```

### 3.2 Journey 2: Visual & AI-Assisted Audience Segmentation
```
[ Marketer / Admin ]
        |
        v
1. Navigate to "Segments" (/segments) -> Click "Create Segment"
        |
        v
2. Choose rule construction path:
   ├── PATH A: Visual AST Rule Builder
   │     - Define root logical group (AND / OR)
   │     - Add condition rows (Field, Operator, Value)
   │     - Add nested sub-groups with distinct combinator (up to depth 10)
   │
   └── PATH B: AI Natural Language Generator
         - Open "Generate with AI" modal
         - Enter prompt: "Customers living in Mumbai who visited at least 4 times and spent over 5000"
         - Submit to POST /api/v1/ai/segments/generate-rules
         - If AI succeeded: populates rule builder with generated AST
         - If fallback triggered: displays "Deterministic Fallback Engine Used" banner
        |
        v
3. Save Segment via POST /api/v1/segments
        |
        v
4. Inspect Evaluated Audience:
   - Trigger POST /api/v1/segments/{id}/preview -> Returns matchedAudienceCount
   - Browse paginated member roster via GET /api/v1/segments/{id}/members
```

### 3.3 Journey 3: Campaign Authoring, Safety Validation & Dispatch
```
[ Marketer / Admin ]
        |
        v
1. Navigate to "Campaigns" (/campaigns) -> Click "New Campaign"
        |
        v
2. Fill campaign metadata:
   - Campaign Name (required, max 100 chars)
   - Description (optional, max 500 chars)
   - Target Segment (dropdown populated from active segments)
   - Personalization toggle (Boolean: enable/disable)
   - Message Template with token inserter: "Dear {firstName}, enjoy 20% off in {city}!"
        |
        v
3. Save as DRAFT via POST /api/v1/campaigns
        |
        v
4. Click "Launch Campaign":
   ├── Safety Pre-flight Check:
   │     - Query segment preview count
   │     - If count == 0: Launch DISABLED, displays "Zero Audience: Cannot Launch" warning
   │     - If count > 0: Launch confirmation modal rendered
   │
   └── Confirmation Modal:
         - Displays Target Segment Name, Audience Count, Personalization status
         - Warning: "Dispatch is irreversible. Messages will be enqueued for delivery."
         - User explicitly confirms
        |
        v
5. Execute POST /api/v1/campaigns/{id}/launch
        |
        v
6. Backend transitions status DRAFT -> RUNNING, pushes tasks to Redis Streams
        |
        v
7. UI immediately redirects to Campaign Delivery Monitor (/campaigns/{id})
```

### 3.4 Journey 4: Near-Real-Time Delivery Monitoring & Post-Campaign Analytics
```
[ Campaign Delivery Monitor: /campaigns/{id} ]
        |
        v
1. Near-Real-Time Delivery Monitoring (polls every 2.5 seconds while status == RUNNING):
   - Query GET /api/v1/campaigns/{id}/delivery-summary
   - Progress bar updates: completionPercentage
   - Metric counters: Pending, Sent, Failed, Total Target
        |
        v
2. Recipient Audit Table:
   - Query GET /api/v1/campaigns/{id}/deliveries?page=0&size=20
   - Filter by status: ALL, PENDING, SENT, FAILED
   - Inspect failure reasons for failed deliveries
        |
        v
3. When isTerminal == true (Status transitions to COMPLETED or FAILED):
   - Periodic polling stops immediately (poll interval set to -1)
   - Status badge transitions to final state (Completed Emerald / Failed Red)
   - "Generate AI Performance Summary" button becomes active
        |
        v
4. Executive Analytics:
   - Query GET /api/v1/reports/campaigns/{id}
   - Delivery rate metric, duration seconds, dispatched timestamp
   - Query GET /api/v1/reports/campaigns/{id}/ai-summary -> displays AI narrative
```

### 3.5 Journey 5: Administrator Security & User Governance
```
[ Administrator Only: ROLE_ADMIN ]
        |
        v
1. Navigate to "User Management" (/admin/users)
        |
        v
2. Browse active and inactive staff accounts (paginated grid)
        |
        v
3. Administrative Actions:
   ├── Create User: POST /api/v1/users (username, email, password, role)
   ├── Change Role: PATCH /api/v1/users/{id}/role (promote to ADMIN / demote to MARKETER)
   ├── Deactivate Account: PATCH /api/v1/users/{id}/deactivate (immediate session revocation)
   └── Reset Password: PATCH /api/v1/users/{id}/password (set new compliant password)
        |
        v
4. AI Audits Center (/admin/ai-audits):
   - Query GET /api/v1/ai/segments/audits
   - Inspect user prompts, generated rules, and action taken (DISCARDED / CREATED)
```

---

## 4. Information Architecture & Navigation Hierarchy

### 4.1 Shell Layout Structure (`AppLayout`)
- **Top Utility Bar:**
  - Platform Branding ("Enterprise AI-CRM" with discrete build version tag `v1.0.0`)
  - Active Context Indicator (Breadcrumbs reflecting nested navigation)
  - Current User Chip (Username, Role Badge: `ADMIN` [Amber] / `MARKETER` [Slate])
  - Profile & Security Quick Menu (Password update, Session info)
  - Explicit "Sign Out" button (Destroys server session, clears tokens, redirects to `/login`)
- **Left Navigation Drawer:**
  - Grouped into distinct logical sections
  - Collapsible to compact icon-only mode for high-density widescreen workspaces
  - Dynamic role filtering: administrative navigation entries are never rendered for `ROLE_MARKETER`

```
+-----------------------------------------------------------------------------------------------+
| [Logo] Enterprise AI-CRM   |  Customers > Detail > #1042           | [Marcus (ADMIN)] [Logout]|
+-------------------+---------------------------------------------------------------------------+
| PRIMARY           |                                                                           |
|  [#] Dashboard    |  [ Main Viewport Area ]                                                   |
|  [U] Customers    |                                                                           |
|  [S] Segments     |                                                                           |
|  [C] Campaigns    |                                                                           |
|  [^] Ingestion    |                                                                           |
|  [R] Reports      |                                                                           |
|                   |                                                                           |
| GOVERNANCE (Admin)|                                                                           |
|  [A] User Admin   |                                                                           |
|  [@] AI Audits    |                                                                           |
|                   |                                                                           |
| SYSTEM            |                                                                           |
|  [<] Collapse Nav |                                                                           |
+-------------------+---------------------------------------------------------------------------+
```

### 4.2 Route & Navigation Tree
```
/
├── /login                                [Public] Authentication screen
├── /dashboard                            [Shared] Operational & executive overview
├── /customers                            [Shared] Customer directory & search
│   └── /customers/:id                    [Shared] Customer profile & spend inspector
├── /segments                             [Shared] Segment catalog
│   ├── /segments/new                     [Shared] Segment creation (Visual / AI builder)
│   ├── /segments/:id/edit                [Shared] Segment modification
│   └── /segments/:id/members             [Shared] Evaluated segment member inspection
├── /campaigns                            [Shared] Campaign portfolio & statuses
│   ├── /campaigns/new                    [Shared] Campaign authoring & template editor
│   ├── /campaigns/:id/edit               [Shared] Draft campaign parameter editing
│   └── /campaigns/:id                    [Shared] Near-real-time delivery monitor & audits
├── /uploads                              [Shared] Bulk customer file ingestion & logs
├── /reports                              [Shared] Analytics center & customer demographics
│   └── /reports/campaigns/:id            [Shared] Campaign deep-dive report & AI summary
├── /admin/users                          [ADMIN only] User provisioning & credential admin
├── /admin/ai-audits                      [ADMIN only] AI prompt & rule generation audit log
└── /error/:code                          [Public/Shared] Standardized error states (401/403/404/500)
```

---

## 5. Critical Domain States & Edge Cases

| Domain | Scenario / Edge Case | Expected System & UI Behavior |
| :--- | :--- | :--- |
| **Authentication** | JWT Token Expiration (1-hour TTL) | Backend returns 401 Unauthorized; frontend API interceptor clears stored JWT, terminates authentication state, displays "Session expired. Please log in again.", and redirects to `/login`. No refresh tokens exist. |
| **Authentication** | User Account Deactivated by Admin | When deactivated user makes any API call, backend returns 401/403; UI terminates active session, redirects to login with "Your account has been deactivated. Contact IT." |
| **Customer** | Soft-Deleted Customer Record | Soft-deleted records are filtered out of `/api/v1/customers` by default. Customer detail shows read-only banner if accessed by direct ID; deletion is restricted to ADMIN only. No historical transaction timeline exists in backend. |
| **Customer** | Duplicate Email during Create/Patch | Backend returns 409 Conflict (`ERR_RESOURCE_CONFLICT`); UI highlights Email input field with inline red error text: "A customer with this email address already exists." |
| **Segmentation** | Zero Customers Match Rule Tree | Backend returns `matchedAudienceCount = 0`; UI displays amber badge "0 Customers Match"; Member grid shows empty state; Campaign launch using this segment is blocked. |
| **Segmentation** | Nested Rule Exceeds Max Depth (10) | UI enforces client-side depth limiter (disables "Add Group" at depth 10); if backend returns validation error, UI displays clear error toast without crashing. |
| **AI Generation** | Fallback Engine Triggered (`isFallback=true`) | UI renders clear warning banner above the generated rule tree: "AI model unavailable. Deterministic fallback rules generated." Clearly distinguishes fallback from Gemini output. |
| **AI Generation** | Invalid Prompt Syntax (< 10 chars) | Input form prevents submission with inline validation message: "Prompt must be between 10 and 500 characters." |
| **Campaign** | Launch with 0 Audience | UI disables Launch button with tooltip. If forced, backend rejects with 400 Bad Request; campaign remains in `DRAFT` status and never transitions to `RUNNING`. |
| **Campaign** | Launching an Already Running Campaign | Launch button is hidden/disabled for non-DRAFT campaigns. Backend rejects duplicate launch with 400; UI shows warning toast. |
| **Bulk Upload** | File Format Not CSV or XLSX | UI file drop zone rejects file immediately; displays red alert: "Unsupported file type. Please upload a .csv or .xlsx file." Progress is client-side network progress. |
| **Bulk Upload** | Partial Success (e.g. 80 valid, 20 invalid) | Upload result badge displays amber "PARTIAL SUCCESS"; failure table lists exact row numbers and validation messages; valid records are committed. |
| **RBAC / Security** | MARKETER attempts Admin Action | Administrative buttons (Delete Customer, Delete Campaign, User Admin, AI Audits) are completely omitted from DOM for marketers. Server enforces 403 on API. |
