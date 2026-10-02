# Enterprise AI-CRM Platform (`CS-CRM-2026`)
# Frontend Screen Map & View Specifications
# Document Version: 2.0.0 — Status: Approved Verified Baseline

---

## 1. Overview & Screen Inventory

This document defines the authoritative screen inventory and interaction contract for the Enterprise AI-CRM Platform frontend. Every screen is strictly grounded in existing backend REST API capabilities and DTO definitions. No hypothetical, unsupported, or decorative screens or fields are included.

### Complete Screen Index (16 Views)
| Screen ID | Screen Name | Route | Authorized Role(s) | Primary Backend Endpoint(s) |
| :--- | :--- | :--- | :--- | :--- |
| **SCR-01** | Authentication / Login | `/login` | Public | `POST /api/v1/auth/login` |
| **SCR-02** | Executive & Operational Dashboard | `/dashboard` | ADMIN, MARKETER | `GET /api/v1/reports/customers/overview`, `GET /api/v1/campaigns` |
| **SCR-03** | Customer Directory & Filter Center | `/customers` | ADMIN, MARKETER | `GET /api/v1/customers`, `POST /api/v1/customers` |
| **SCR-04** | Customer Profile & Inspector | `/customers/:id` | ADMIN, MARKETER | `GET /api/v1/customers/{id}`, `PATCH /api/v1/customers/{id}`, `DELETE /api/v1/customers/{id}` |
| **SCR-05** | Audience Segment Catalog | `/segments` | ADMIN, MARKETER | `GET /api/v1/segments`, `DELETE /api/v1/segments/{id}` |
| **SCR-06** | Visual Segment Builder & AI Assistant | `/segments/new`, `/segments/:id/edit` | ADMIN, MARKETER | `POST /api/v1/segments`, `PATCH /api/v1/segments/{id}`, `POST /api/v1/ai/segments/generate-rules` |
| **SCR-07** | Segment Audience Preview & Member Roster | `/segments/:id/members` | ADMIN, MARKETER | `POST /api/v1/segments/{id}/preview`, `GET /api/v1/segments/{id}/members` |
| **SCR-08** | Marketing Campaign Portfolio | `/campaigns` | ADMIN, MARKETER | `GET /api/v1/campaigns`, `DELETE /api/v1/campaigns/{id}` |
| **SCR-09** | Campaign Authoring & Personalization Studio| `/campaigns/new`, `/campaigns/:id/edit`| ADMIN, MARKETER | `POST /api/v1/campaigns`, `PATCH /api/v1/campaigns/{id}`, `GET /api/v1/segments` |
| **SCR-10** | Campaign Delivery Monitor & Audit Console | `/campaigns/:id` | ADMIN, MARKETER | `GET /api/v1/campaigns/{id}`, `POST /api/v1/campaigns/{id}/launch`, `GET /api/v1/campaigns/{id}/delivery-summary`, `GET /api/v1/campaigns/{id}/deliveries` |
| **SCR-11** | Bulk Data Ingestion Hub | `/uploads` | ADMIN, MARKETER | `POST /api/v1/uploads/bulk`, `GET /api/v1/uploads/history` |
| **SCR-12** | Executive Analytics & Reporting Center | `/reports` | ADMIN, MARKETER | `GET /api/v1/reports/customers/overview`, `GET /api/v1/reports/campaigns/history` |
| **SCR-13** | Campaign Analytics Deep-Dive & AI Narrative| `/reports/campaigns/:id` | ADMIN, MARKETER | `GET /api/v1/reports/campaigns/{id}`, `GET /api/v1/reports/campaigns/{id}/ai-summary` |
| **SCR-14** | Admin User Governance & Credential Console | `/admin/users` | **ADMIN only** | `GET /api/v1/users`, `POST /api/v1/users`, `PATCH /api/v1/users/{id}/*` |
| **SCR-15** | AI Model Audit Log | `/admin/ai-audits` | **ADMIN only** | `GET /api/v1/ai/segments/audits` |
| **SCR-16** | System Error & Access Denied Views | `/error/:code` | Public / Shared | N/A (Client-side routing error boundary) |

---

## 2. Detailed Screen Specifications

---

### SCR-01: Authentication / Login
- **Route:** `/login`
- **Authorized Roles:** Public (Anonymous)
- **Purpose:** Authenticate users via username and password, acquire a 1-hour stateless JWT token, store credentials in `VaadinSession`, and redirect to `/dashboard`.
- **Backend Endpoint(s):**
  - `POST /api/v1/auth/login`
- **Data Model:**
  - Inbound: `LoginRequest { username: String, password: String }`
  - Outbound: `LoginResponse { token: String, tokenType: "Bearer", user: { id: Long, username: String, role: String } }`
- **Actions:**
  - Submit login form
  - Toggle password visibility
- **Validation:**
  - Username: Required, non-blank
  - Password: Required, non-blank
- **Loading State:** Sign-in button displays disabled spinner; inputs are read-only during authentication.
- **Empty State:** Clean, distraction-free authentication card with brand typography.
- **Error State:**
  - 401 Unauthorized (`ERR_UNAUTHORIZED`): Displays red banner "Invalid username or password."
  - Account Deactivated: Displays red alert "Account is deactivated. Contact system administrator."
  - Network Failure: Displays "Unable to connect to CRM authentication service."
- **Forbidden State:** N/A (Public view).
- **Responsive Behavior:** Centered card (max-width 420px) on desktop; full-width stacked card on mobile.

---

### SCR-02: Executive & Operational Dashboard
- **Route:** `/dashboard`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Authoritative real-time summary of business metrics, campaign statuses, active customer base size, and recent campaigns.
- **Backend Endpoint(s):**
  - `GET /api/v1/reports/customers/overview`
  - `GET /api/v1/campaigns?page=0&size=5&sort=id,desc`
- **Data Model:**
  - `CustomerOverviewReportResponse`: `totalActiveCustomers`, `grossCustomerSpend`, `averageSpendPerCustomer`, `totalVisits`, `averageVisitsPerCustomer`, `topLocations` (`city`, `customerCount`)
  - `List<CampaignResponse>`: Recent campaigns list
- **Actions:**
  - Quick action: "Launch New Campaign" (routes to `/campaigns/new`)
  - Quick action: "Create Segment" (routes to `/segments/new`)
  - Quick action: "Import Customers" (routes to `/uploads`)
  - Click campaign row to open Campaign Monitor (`/campaigns/:id`)
- **Validation:** None (Read-only data aggregation).
- **Loading State:** Skeleton loaders for 4 KPI stat cards and recent campaigns table.
- **Empty State:**
  - Zero customers: Displays "No customers active. Import records via Data Ingestion to begin."
  - Zero campaigns: Displays "No campaigns created yet."
- **Error State:** Red inline notification banner with "Retry Data Fetch" button if overview API fails.
- **Forbidden State:** Handled by route interceptor; unauthenticated users redirected to `/login`.
- **Responsive Behavior:** 4-column KPI grid on desktop -> 2-column grid on tablet -> 1-column stacked cards on mobile.

---

### SCR-03: Customer Directory & Filter Center
- **Route:** `/customers`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Paginated customer master list supporting multi-attribute filtering, sorting, fast search, customer creation, and soft-delete governance.
- **Backend Endpoint(s):**
  - `GET /api/v1/customers` (Query params: `firstName`, `lastName`, `email`, `city`, `country`, `tag`, `page`, `size`, `sort`)
  - `POST /api/v1/customers`
  - `DELETE /api/v1/customers/{id}` (ADMIN only)
- **Data Model:**
  - Request DTO: `CustomerRequestDto`
  - Response DTO: `List<CustomerResponseDto>` with `PageMetadata`
- **Actions:**
  - Filter by City, Country, Tag, Name, or Email
  - Sort by columns (`totalSpend`, `visitCount`, `lastActiveDate`, `createdAt`)
  - Change page size (10, 20, 50, 100)
  - Click "New Customer" to open creation modal
  - Click customer row to view details (`/customers/:id`)
  - Click "Delete" (Visible only to `ROLE_ADMIN`) with confirmation modal
- **Validation:**
  - Create: `firstName` (max 100), `lastName` (max 100), `email` (valid email format, max 255), `totalSpend` (>= 0.00), `visitCount` (>= 0).
- **Loading State:** Vaadin Grid loading bar active during page fetch.
- **Empty State:** "No customers found matching search criteria. Reset filters or create a customer."
- **Error State:** Grid displays inline error panel: "Failed to load customer records. [Retry Button]".
- **Forbidden State:** Delete action hidden for Marketer. Server enforces 403 on DELETE.
- **Responsive Behavior:** Tabular view on desktop; compact stacked card list on mobile.

---

### SCR-04: Customer Profile & Inspector
- **Route:** `/customers/:id`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** View and edit an individual customer's attributes (`totalSpend`, `visitCount`, `lastActiveDate`, `tags`, `phone`, `city`, `country`). Note: The backend provides current aggregated metrics; there is no historical transaction timeline.
- **Backend Endpoint(s):**
  - `GET /api/v1/customers/{id}`
  - `PATCH /api/v1/customers/{id}`
  - `DELETE /api/v1/customers/{id}` (ADMIN only)
- **Data Model:**
  - `CustomerResponseDto`: `id`, `firstName`, `lastName`, `email`, `phone`, `city`, `country`, `totalSpend`, `visitCount`, `lastActiveDate`, `tags`, `createdAt`, `updatedAt`
  - `CustomerPatchRequestDto`
- **Actions:**
  - Edit customer attributes (First Name, Last Name, Phone, City, Country, Spend, Visit Count, Tags, Last Active Date)
  - Save Changes (`PATCH /api/v1/customers/{id}`)
  - Soft Delete Customer (ADMIN only)
  - Back to Directory (`/customers`)
- **Validation:**
  - Email format validation
  - Total Spend must be positive decimal (>= 0.00)
  - Visit count must be non-negative integer (>= 0)
- **Loading State:** Centered loading spinner while resolving customer record.
- **Empty State / Not Found:** If customer does not exist or was soft-deleted: "Customer not found or has been deleted. [Back to Customers]".
- **Error State:** Field-level validation highlights; 409 Conflict if email is taken.
- **Forbidden State:** Delete button hidden for Marketer.
- **Responsive Behavior:** Two-column profile layout on desktop; single-column stacked layout on mobile.

---

### SCR-05: Audience Segment Catalog
- **Route:** `/segments`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Browse, search, and manage dynamic customer segments.
- **Backend Endpoint(s):**
  - `GET /api/v1/segments` (`page`, `size`, `sort`)
  - `DELETE /api/v1/segments/{id}`
- **Data Model:**
  - `SegmentResponse`: `id`, `name`, `description`, `rules` (JsonNode), `createdBy`, `createdByName`, `createdAt`, `updatedAt`
- **Actions:**
  - Click "Create Segment" -> routes to `/segments/new`
  - Click "Edit Rules" -> routes to `/segments/:id/edit`
  - Click "View Members" -> routes to `/segments/:id/members`
  - Click "Delete Segment" with confirmation modal
- **Validation:** Confirmation dialog on deletion.
- **Loading State:** Table skeleton loader during pagination.
- **Empty State:** "No audience segments created yet. Build dynamic segments to target your campaigns."
- **Error State:** Toast notification on network error.
- **Responsive Behavior:** Table collapses description column on screens < 900px.

---

### SCR-06: Visual Segment Builder & AI Assistant
- **Route:** `/segments/new` (Creation) and `/segments/:id/edit` (Editing)
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Construct complex, nested boolean rule trees (AST) visually or generate them via natural-language AI prompts without writing JSON.
- **Backend Endpoint(s):**
  - `POST /api/v1/segments` (Create)
  - `PATCH /api/v1/segments/{id}` (Update)
  - `POST /api/v1/ai/segments/generate-rules` (AI generation)
  - `GET /api/v1/segments/{id}` (Fetch existing rules for edit)
- **Data Model:**
  - Root AST: `LogicalRuleNode` (`operator`: "AND" | "OR", `conditions`: `[ RuleNode ]`)
  - Leaf AST: `ConditionRuleNode` (`field`, `op`, `value`)
  - `AiRuleGenerationRequest { prompt: String }`
  - `AiRuleGenerationResponse { prompt: String, ruleTree: JsonNode, isValidated: boolean, isFallback: boolean }`
- **Actions:**
  - Add atomic condition to group
  - Add nested logical group (AND/OR) with indentation
  - Select Field: `city`, `totalSpend`, `visitCount`, `lastActiveDate`, `tags`
  - Select Operator dynamically filtered by Field type
  - Enter Value with strict type controls (Currency for `totalSpend`, Integer for `visitCount`, DatePicker for `lastActiveDate`, Array/Token input for `IN`/`NOT_IN`)
  - Delete condition row or group
  - Click "Generate with AI" -> Opens AI Prompt Modal
  - Save Segment
- **Validation:**
  - Segment Name required (max 100 chars)
  - Root rule tree must have at least one valid condition
  - Max nesting depth 10 enforced in UI
  - Strict type checking per field
- **Loading State:** Full-page loader during segment load; modal spinner while AI generates rules.
- **Empty State:** Builder starts with a default root AND group containing one empty condition row.
- **Error State:**
  - AI failure: Modal displays error and offers manual entry.
  - Fallback used (`isFallback=true`): Prominent amber banner: "External AI unavailable. Deterministic fallback rules applied."
- **Responsive Behavior:** Indented tree layout adjusts padding on mobile; condition rows wrap cleanly.

---

### SCR-07: Segment Audience Preview & Member Roster
- **Route:** `/segments/:id/members`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Inspect the evaluated audience count of a saved segment and browse the paginated list of matching customers. Note: Evaluation requires a persistent segment ID.
- **Backend Endpoint(s):**
  - `POST /api/v1/segments/{id}/preview` -> `SegmentPreviewResponse { segmentId, segmentName, matchedAudienceCount, evaluatedAt }`
  - `GET /api/v1/segments/{id}/members` -> `Page<CustomerResponseDto>`
- **Data Model:**
  - `SegmentPreviewResponse`, `PageMetadata`, `List<CustomerResponseDto>`
- **Actions:**
  - Click "Re-evaluate Audience" to trigger fresh evaluation
  - Browse paginated member table
  - Click customer row to view individual customer profile
  - Back to Segments
- **Validation:** N/A (Read-only evaluation).
- **Loading State:** Metric card displays loader; member table shows loading overlay.
- **Empty State:** If `matchedAudienceCount == 0`: Displays amber alert "This segment currently matches 0 customers."
- **Error State:** Displays "Failed to evaluate segment criteria. [Retry]".
- **Responsive Behavior:** Metric card stays top-pinned; grid scrolls horizontally on mobile.

---

### SCR-08: Marketing Campaign Portfolio
- **Route:** `/campaigns`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** High-level dashboard of all marketing campaigns, showing lifecycle statuses (`DRAFT`, `RUNNING`, `COMPLETED`, `FAILED`), target segments, and delivery monitor links.
- **Backend Endpoint(s):**
  - `GET /api/v1/campaigns` (Filter by `status`, `page`, `size`, `sort`)
  - `DELETE /api/v1/campaigns/{id}` (ADMIN only)
- **Data Model:**
  - `CampaignResponse`: `id`, `name`, `segmentId`, `segmentName`, `status`, `personalizationEnabled`, `startedAt`, `completedAt`
- **Actions:**
  - Filter by Status: ALL, DRAFT, RUNNING, COMPLETED, FAILED
  - Click "New Campaign" -> routes to `/campaigns/new`
  - Click campaign row -> routes to Campaign Monitor (`/campaigns/:id`)
  - Click "Delete Campaign" (Visible to `ROLE_ADMIN` only)
- **Validation:** Non-draft campaigns cannot be edited; running campaigns show active pulse icon. Unsupported actions (pause, resume, duplicate, schedule) are omitted.
- **Loading State:** Table skeleton loader.
- **Empty State:** "No campaigns found matching filter. Create a new campaign to begin."
- **Error State:** Error toast with retry option.
- **Forbidden State:** Delete button hidden for Marketer; 403 on API attempt.
- **Responsive Behavior:** Table columns compress on mobile; status badge prioritized.

---

### SCR-09: Campaign Authoring & Personalization Studio
- **Route:** `/campaigns/new` (Creation) and `/campaigns/:id/edit` (Editing)
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Configure campaign parameters, select audience segment, craft message templates with variable tokens, and enable personalization.
- **Backend Endpoint(s):**
  - `POST /api/v1/campaigns`
  - `PATCH /api/v1/campaigns/{id}`
  - `GET /api/v1/segments` (populate segment dropdown)
  - `GET /api/v1/campaigns/{id}`
- **Data Model:**
  - `CampaignCreateRequest`, `CampaignUpdateRequest`, `CampaignResponse`
- **Actions:**
  - Enter Campaign Name and Description
  - Select Target Segment from dropdown
  - Toggle Personalization Enabled (`true`/`false`)
  - Write Message Template with token helper buttons (`{firstName}`, `{city}`)
  - Save as DRAFT
- **Validation:**
  - Campaign Name: Required, max 100 characters
  - Segment ID: Required, must select a valid segment
  - Message Template: Required, max 1000 characters
- **Loading State:** Disabled inputs during submit; segment list spinner while loading options.
- **Empty State:** Form defaults with empty template and placeholder tips.
- **Error State:** Red inline field errors if constraints violated.
- **Responsive Behavior:** Side-by-side Editor & Live Preview on desktop; tabbed on mobile.

---

### SCR-10: Campaign Delivery Monitor & Audit Console
- **Route:** `/campaigns/:id`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Campaign control console featuring pre-flight validation, launch confirmation, near-real-time delivery monitoring via periodic polling, and recipient-level delivery log inspection.
- **Backend Endpoint(s):**
  - `GET /api/v1/campaigns/{id}`
  - `POST /api/v1/campaigns/{id}/launch`
  - `GET /api/v1/campaigns/{id}/delivery-summary`
  - `GET /api/v1/campaigns/{id}/deliveries` (`status`, `page`, `size`)
  - `GET /api/v1/reports/campaigns/{id}/ai-summary`
- **Data Model:**
  - `CampaignResponse`, `CampaignLaunchResponse`, `DeliverySummaryResponse`, `Page<DeliveryRecordResponse>`, `AiCampaignSummaryResponse`
- **Actions:**
  - Launch Campaign (Disabled if audience == 0; opens confirmation modal)
  - Near-real-time delivery monitoring: Periodic polling runs every 2.5s while status == `RUNNING`; stops when `isTerminal == true`.
  - Filter recipient log by status: ALL, PENDING, SENT, FAILED
  - View failure reason for failed deliveries
  - Click "Generate AI Performance Summary" once campaign is completed
- **Validation:**
  - Launch requires confirmation checkbox: "I understand that dispatch is irreversible."
  - Backend strictly blocks launch if target audience is 0 (`ERR_ZERO_AUDIENCE`).
- **Loading State:** Progress bar reflects `completionPercentage`; recipient grid shows loading spinner.
- **Empty State:** Before launch: "Campaign is in DRAFT. Review settings and launch when ready."
- **Error State:**
  - Launch failure (zero audience): High-visibility red alert.
  - Delivery failure: Failure count highlighted in red with expandable audit rows.
- **Responsive Behavior:** Telemetry metrics stack into 2x2 grid on mobile; delivery log collapses timestamp.

---

### SCR-11: Bulk Data Ingestion Hub
- **Route:** `/uploads`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Ingest customer spreadsheets in CSV or XLSX format with file format validation, row-by-row error reporting, and historical audit tracking.
- **Backend Endpoint(s):**
  - `POST /api/v1/uploads/bulk` (MultipartFile `file`)
  - `GET /api/v1/uploads/history` (`page`, `size`)
- **Data Model:**
  - `UploadResultResponse`: `uploadId`, `fileName`, `status`, `totalRecords`, `successfulRecords`, `failedRecords`, `errors: List<UploadErrorDetail>`
  - `List<UploadHistoryDto>` with `PageMetadata`
- **Actions:**
  - Drag-and-drop file upload zone (Supports `.csv`, `.xlsx`, max 10MB)
  - Browse file picker button
  - Inspect row-level error grid for recent upload (`row`, `email`, `reason`)
  - Browse past upload history table
- **Validation:**
  - Client-side file extension check before upload
  - File size must not exceed 10MB
- **Loading State:** Client-side network upload progress bar and indeterminate row processing spinner.
- **Empty State:** History table shows "No previous uploads recorded."
- **Error State:**
  - Total failure: Red alert "Upload failed: Invalid file structure or corrupted data."
  - Partial success: Amber badge "PARTIAL SUCCESS — 85 imported, 15 failed."
- **Responsive Behavior:** Upload drop zone contracts on mobile; error table scrolls horizontally.

---

### SCR-12: Executive Analytics & Reporting Center
- **Route:** `/reports`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Business intelligence dashboard displaying customer demographic aggregations, spend statistics, and campaign delivery performance trends.
- **Backend Endpoint(s):**
  - `GET /api/v1/reports/customers/overview`
  - `GET /api/v1/reports/campaigns/history` (`page`, `size`)
- **Data Model:**
  - `CustomerOverviewReportResponse`: `totalActiveCustomers`, `grossCustomerSpend`, `averageSpendPerCustomer`, `totalVisits`, `averageVisitsPerCustomer`, `topLocations` (`city`, `customerCount`)
  - `List<CampaignHistoryItemResponse>`: Historical campaigns with delivery rates
- **Actions:**
  - Inspect Top Customer Locations (Bar chart & tabular metrics)
  - Browse Campaign Performance History
  - Click on any campaign row to navigate to Campaign Deep-Dive (`/reports/campaigns/:id`)
- **Validation:** None (Read-only analytics).
- **Loading State:** Skeleton chart containers and stat loaders.
- **Empty State:** "No historical campaign data available yet."
- **Error State:** Alert card with retry button.
- **Responsive Behavior:** Charts stack vertically on tablet and mobile viewports.

---

### SCR-13: Campaign Analytics Deep-Dive & AI Narrative
- **Route:** `/reports/campaigns/:id`
- **Authorized Roles:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Purpose:** Reporting page for an individual campaign. Shows delivery metrics, execution timeline, and optional AI-generated executive narrative.
- **Backend Endpoint(s):**
  - `GET /api/v1/reports/campaigns/{id}` -> `CampaignReportResponse`
  - `GET /api/v1/reports/campaigns/{id}/ai-summary` -> `AiCampaignSummaryResponse`
- **Data Model:**
  - `CampaignReportResponse`: `campaignId`, `campaignName`, `status`, `targetAudienceSize`, `metrics` (`sent`, `failed`, `deliveryRatePercentage`), `timeline` (`launchedAt`, `completedAt`, `durationSeconds`)
  - `AiCampaignSummaryResponse`: `campaignId`, `aiSummary`
- **Actions:**
  - Click "Request AI Executive Summary" button
  - Return to Reports overview
- **Validation:** AI summary available once campaign is in a terminal status (`COMPLETED` or `FAILED`).
- **Loading State:** Pulsing card with "Generating summary..."; gauge spinner.
- **Empty State:** Card shows "AI Executive Summary available on demand. [Generate Summary]".
- **Error State:** Displays statistical report cleanly; indicates "AI narrative generation unavailable."
- **Responsive Behavior:** Metric ring centers above details on mobile; side-by-side on desktop.

---

### SCR-14: Admin User Governance & Credential Console
- **Route:** `/admin/users`
- **Authorized Roles:** **`ROLE_ADMIN` only** (Hidden and blocked for Marketer)
- **Purpose:** Complete user account lifecycle management: provisioning new operators, role assignment (`ROLE_MARKETER` vs `ROLE_ADMIN`), account deactivation, and password resets.
- **Backend Endpoint(s):**
  - `GET /api/v1/users` (`page`, `size`, `sort`)
  - `POST /api/v1/users`
  - `GET /api/v1/users/{id}`
  - `PATCH /api/v1/users/{id}/role`
  - `PATCH /api/v1/users/{id}/deactivate`
  - `PATCH /api/v1/users/{id}/password`
- **Data Model:**
  - `UserResponse`: `id`, `username`, `email`, `role`, `isActive`, `createdAt`, `updatedAt`
  - `UserCreateRequest`, `UserRoleUpdateRequest`, `UserPasswordUpdateRequest`
- **Actions:**
  - Click "Create New User" -> Opens user provisioning modal
  - Change Role dropdown (`ROLE_MARKETER` <-> `ROLE_ADMIN`) with confirmation
  - Click "Deactivate Account" (Cannot deactivate self)
  - Click "Reset Password" -> Opens credential modal with password validation
- **Validation:**
  - Username: 1 to 50 characters, unique
  - Email: Valid email format, max 255 characters
  - Password: Strong password required (min 8 chars, uppercase, lowercase, digit, special char)
  - Self-deactivation prevention: Admin cannot deactivate their own account
- **Loading State:** User grid shows loading bar.
- **Empty State:** "No additional users registered."
- **Error State:** Red toast if username/email conflicts or validation fails.
- **Forbidden State:** Marketer accessing `/admin/users` is redirected to `/error/403`.
- **Responsive Behavior:** Table scrolls horizontally on mobile; action buttons collapse into a dropdown.

---

### SCR-15: AI Model Audit Log
- **Route:** `/admin/ai-audits`
- **Authorized Roles:** **`ROLE_ADMIN` only** (Hidden and blocked for Marketer)
- **Purpose:** Audit log tracking all AI segment prompt interactions, generated rules, and user actions.
- **Backend Endpoint(s):**
  - `GET /api/v1/ai/segments/audits` (`page`, `size`, `sort`)
- **Data Model:**
  - `List<AiSegmentAuditDto>` with `PageMetadata`:
    - `id` (`Long`)
    - `username` (`String`)
    - `promptText` (`String`)
    - `generatedRules` (`JsonNode`)
    - `actionTaken` (`String`: "DISCARDED" | "CREATED")
    - `segmentId` (`Long`, nullable)
    - `createdAt` (`Instant`)
- **Actions:**
  - Browse paginated audit records
  - Click row to inspect prompt text and generated JSON rule tree in a modal
- **Validation:** Read-only audit data.
- **Loading State:** Grid loading animation.
- **Empty State:** "No AI generation events recorded in audit log."
- **Error State:** Inline error card.
- **Forbidden State:** 403 Access Denied view if non-admin attempts access.
- **Responsive Behavior:** JSON preview expands into a full-screen drawer on mobile.

---

### SCR-16: System Error & Access Denied Views
- **Route:** `/error/:code` (Mapped internally to 401, 403, 404, 500)
- **Authorized Roles:** Public / All
- **Purpose:** Graceful error handling for missing views, expired sessions, forbidden access attempts, and server errors.
- **Backend Endpoint(s):** N/A (Client-side routing error boundary)
- **Data Model:** Error code, title, descriptive message, actionable navigation target.
- **Actions:**
  - "Return to Dashboard" button
  - "Log In Again" button (for 401)
- **Empty / Error States:**
  - 401 Unauthorized: "Your session has expired. [Log In]"
  - 403 Forbidden: "Access Denied: Administrative privileges required. [Go to Dashboard]"
  - 404 Not Found: "The requested view does not exist. [Go to Dashboard]"
  - 500 Internal Error: "An unexpected system error occurred. Please contact IT support."
- **Responsive Behavior:** Centered message container with clean typography.
