# Enterprise AI-CRM Platform (`CS-CRM-2026`)
# Frontend Implementation Plan & Milestone Roadmap
# Document Version: 2.0.0 — Status: Approved Verified Baseline

---

## 1. Governance & Implementation Gates

### 1.1 Implementation Invariant
**DO NOT WRITE PRODUCTION FRONTEND CODE DURING THIS ARCHITECTURE PASS.**
This document establishes the verified milestone execution roadmap for subsequent implementation phases under the configured Google Antigravity 7-Phase AI Workflow.

### 1.2 Tool Responsibility Matrix for Upcoming Phases
| Phase | Tool / Harness | Mandatory Behavior |
| :--- | :--- | :--- |
| **GSD (Milestones & Tasks)** | `gsd` / `gsd-executor` | Decomposes each milestone into atomic tasks with verified acceptance criteria. |
| **Roo Code (Code Mode)** | `roo-code` (Code Mode) | Implements approved Vaadin views, components, and service clients via the REST interface. |
| **Verification & Ralph Loop**| `ralph-loop` | Runs targeted verification (Maven compilation, view rendering, API client tests); bounded to max 5 iterations. |
| **Code Review** | `coderabbit-review` | Inspects git diffs for correctness, security, regression risks, and quality gates before commit. |

---

## 2. Discovered Backend Nuances & Architectural Boundary Notes

During the thorough architectural audit of the frozen backend, the following verified API characteristics and operational invariants were identified:

1. **Segment Audience Preview Invariant:**
   - **Backend Behavior:** The preview endpoint `POST /api/v1/segments/{id}/preview` and member list `GET /api/v1/segments/{id}/members` require an existing persistent segment ID (`Long id`). There is no endpoint for transient, unsaved AST evaluation.
   - **Frontend Design Alignment:** In the Segment Builder (SCR-06), clicking "Save & Preview" saves/updates the segment via `POST /api/v1/segments`, immediately transitioning to SCR-07 where live evaluation count and member rosters are rendered.
2. **User Password Modification Access Boundary:**
   - **Backend Behavior:** `PATCH /api/v1/users/{id}/password` is governed by `requestMatchers("/api/v1/users/**").hasRole("ADMIN")` in `SecurityConfig.java`. There is no self-service password update endpoint for `ROLE_MARKETER`.
   - **Frontend Design Alignment:** The credential reset console is strictly rendered within SCR-14 for `ROLE_ADMIN`. Marketers are not presented with self-service password options.
3. **Role-Restricted Deletion Capabilities:**
   - **Backend Behavior:** `DELETE /api/v1/customers/{id}` and `DELETE /api/v1/campaigns/{id}` require `ROLE_ADMIN`.
   - **Frontend Design Alignment:** Delete buttons in SCR-03, SCR-04, and SCR-08 are omitted from the DOM when the active user has `ROLE_MARKETER`.
4. **Zero-Audience Campaign Launch Defense:**
   - **Backend Behavior:** If a target segment matches 0 active customers, `POST /api/v1/campaigns/{id}/launch` rejects the request with HTTP 400 (`ERR_ZERO_AUDIENCE`) and leaves the campaign in `DRAFT`.
   - **Frontend Design Alignment:** The Campaign Monitor (SCR-10) queries `delivery-summary` or `preview` before rendering the launch action; if the audience is 0, the button is disabled and displays a warning banner.
5. **AI Fallback Transparency:**
   - **Backend Behavior:** When Google Gemini is unavailable, `AiService` executes a deterministic fallback and sets `isFallback = true` in `AiRuleGenerationResponse`.
   - **Frontend Design Alignment:** The UI detects `isFallback = true` and renders an amber status badge informing the marketer that fallback rules were applied.
6. **No Customer Transaction / History Endpoint:**
   - **Backend Behavior:** The `Customer` entity provides aggregate metrics (`totalSpend`, `visitCount`, `lastActiveDate`), not a transaction timeline.
   - **Frontend Design Alignment:** The Customer Profile (SCR-04) displays these aggregate metrics without attempting to render unsupported transaction history charts.
7. **Stateless JWT 1-Hour Expiration:**
   - **Backend Behavior:** JWT access tokens expire after 1 hour (3,600,000 ms). No refresh tokens exist.
   - **Frontend Design Alignment:** If any backend call returns 401, the frontend immediately clears the stored token, terminates authentication state, redirects to `/login`, and displays "Session expired. Please log in again."

---

## 3. Sequential Implementation Milestones

---

### Milestone F1: Foundation, Vaadin 24 BOM & Authentication Harness
- **Goal:** Establish the Maven build foundation with Vaadin 24.4.15 BOM, create the application shell layout, implement the stateless JWT client harness, and deliver the login view.
- **Scope:**
  - Add `vaadin-bom` (24.4.15) and `vaadin-spring-boot-starter` to `pom.xml`.
  - Implement `UserSession` (scoped to `VaadinSession`), `FrontendSecurityContext`, and `BearerTokenInterceptor`.
  - Implement `BackendApiClient` utilizing Spring 6 `RestClient`.
  - Implement `MainLayout`, `TopNavbar`, and `SidebarNav` with role-aware drawer navigation.
  - Implement `LoginView` (SCR-01) with authentication error handling.
  - Implement `RouteAccessChecker` enforcing RBAC redirects (`ROLE_ADMIN` vs `ROLE_MARKETER`).
  - Implement automatic 401 logout and session clearing handler.
- **Acceptance Criteria:**
  - `mvn clean compile` succeeds without dependency conflicts.
  - Navigating to `/` redirects to `/login`.
  - Valid admin login routes to `/dashboard` with `ROLE_ADMIN` badge in top navbar.
  - Valid marketer login routes to `/dashboard` with `ROLE_MARKETER` badge; admin navigation links are completely omitted.
  - Expired token triggers 401 interceptor, clears session, and redirects to `/login`.

---

### Milestone F2: Design System, Theming & Executive Dashboard
- **Goal:** Implement the bespoke Enterprise CRM theme (charcoal/graphite base, warm amber accent) and the operational dashboard.
- **Scope:**
  - Configure Vaadin Lumo theme overrides with custom CSS properties defined in `DESIGN-SYSTEM.md`.
  - Implement reusable components: `KpiStatCard`, `StatusBadge`, `StateViewContainer`.
  - Implement `DashboardView` (SCR-02) consuming `GET /api/v1/reports/customers/overview` and `GET /api/v1/campaigns`.
- **Acceptance Criteria:**
  - UI displays dark charcoal surfaces (`#161b22`) and warm amber accents (`#d97706`).
  - Active customer count, spend totals, and visit metrics display accurate backend data.
  - Recent campaigns table displays accurate statuses with semantic badges.
  - Empty and loading skeleton states render smoothly.

---

### Milestone F3: Customer Management Domain
- **Goal:** Build the paginated customer master directory, search filters, creation modal, and profile inspector.
- **Scope:**
  - Implement `CustomerApiService` consuming `/api/v1/customers/**`.
  - Implement `CustomerListView` (SCR-03) with server-side `DataProvider` pagination.
  - Implement multi-attribute filter bar (City, Tag, Email, Name) and sorting controls.
  - Implement `CustomerFormDialog` for customer creation.
  - Implement `CustomerDetailView` (SCR-04) with field editing and soft-delete confirmation (ADMIN only).
- **Acceptance Criteria:**
  - Customer table pages smoothly over large datasets without loading full records into browser memory.
  - Creating a customer validates required fields and surfaces 409 Conflict if email is duplicate.
  - Soft-delete button is visible only to ADMIN; executes 204 No Content and refreshes grid.

---

### Milestone F4: Dynamic Audience Segmentation & Visual Rule Builder
- **Goal:** Build the visual AST rule builder, rule parser/compiler integration, and member roster inspector.
- **Scope:**
  - Implement `SegmentApiService` consuming `/api/v1/segments/**`.
  - Implement `SegmentListView` (SCR-05).
  - Implement `RuleTreeContainer`, `RuleGroupComponent` (AND/OR combinator), `ConditionRowComponent`, and `ValueFieldFactory`.
  - Implement `SegmentBuilderView` (SCR-06) supporting addition/removal of rules, nested groups, and depth limiting (max 10).
  - Implement `SegmentMembersView` (SCR-07) showing live preview audience count and member table.
- **Acceptance Criteria:**
  - Visual builder constructs valid JSON AST trees matching `SegmentRuleParser` specifications.
  - Nested groups render with clear visual indentation.
  - Audience preview correctly queries `POST /api/v1/segments/{id}/preview` and renders matching customer count.

---

### Milestone F5: Artificial Intelligence Integration
- **Goal:** Integrate AI-assisted natural-language segment generation and campaign summary analytics with transparent fallback badges.
- **Scope:**
  - Implement `AiApiService` consuming `/api/v1/ai/**` and `/api/v1/reports/campaigns/{id}/ai-summary`.
  - Implement `AiSegmentModal` within the Segment Builder.
  - Implement rule population from AI-generated `ruleTree`.
  - Implement fallback banner logic (`isFallback=true`).
  - Implement `AiAuditsView` (SCR-15) restricted strictly to `ROLE_ADMIN`, displaying actual DTO fields (`promptText`, `generatedRules`, `actionTaken`, `createdAt`).
- **Acceptance Criteria:**
  - Submitting a natural-language prompt generates rules in the builder.
  - If backend AI fallback is triggered, an amber "Deterministic Fallback Engine Used" alert is shown.
  - Marketers cannot access `/admin/ai-audits`. Admin can inspect audit records.

---

### Milestone F6: Campaign Lifecycle & Near-Real-Time Delivery Monitoring
- **Goal:** Deliver the campaign authoring studio, pre-flight launch safety protocol, and near-real-time delivery monitoring console.
- **Scope:**
  - Implement `CampaignApiService` and `DeliveryApiService` consuming `/api/v1/campaigns/**`.
  - Implement `CampaignListView` (SCR-08) with status filters.
  - Implement `CampaignEditorView` (SCR-09) with segment picker, token insertion helper, and live preview.
  - Implement `CampaignMonitorView` (SCR-10) with near-real-time periodic polling (`UI.getCurrent().setPollInterval(2500)`).
  - Implement `CampaignLaunchDialog` with zero-audience validation and confirmation checkbox.
  - Implement recipient-level delivery log with status filtering (`PENDING`, `SENT`, `FAILED`).
- **Acceptance Criteria:**
  - Campaigns in `DRAFT` status can be edited; running campaigns poll telemetry every 2.5 seconds.
  - Launch button is strictly disabled if segment matches 0 customers.
  - Polling stops immediately when `isTerminal == true` (`COMPLETED` or `FAILED`).
  - Recipient failure reasons are inspectable in a detail drawer.

---

### Milestone F7: Bulk Ingestion Hub & Row Error Inspector
- **Goal:** Deliver the drag-and-drop CSV/XLSX customer ingestion interface with row-level error reporting and history tracking.
- **Scope:**
  - Implement `UploadApiService` consuming `/api/v1/uploads/**`.
  - Implement `BulkUploadView` (SCR-11) utilizing Vaadin `Upload` component with MIME type validation.
  - Implement row-level error grid displaying exact row numbers, emails, and rejection reasons.
  - Implement upload history grid with status badges (`SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`).
- **Acceptance Criteria:**
  - Uploads CSV or XLSX file to `/api/v1/uploads/bulk`.
  - Partial success displays committed count and an error grid detailing rejected rows.
  - Unsupported file types are rejected client-side before submission.

---

### Milestone F8: Executive Reports & Admin User Console
- **Goal:** Finalize reporting deep-dives and implement admin user governance.
- **Scope:**
  - Implement `ReportApiService` consuming `/api/v1/reports/**`.
  - Implement `ReportsOverviewView` (SCR-12) and `CampaignReportView` (SCR-13).
  - Implement `UserManagementView` (SCR-14), `UserFormDialog`, and `PasswordResetDialog` (ADMIN only).
- **Acceptance Criteria:**
  - Reports display delivery rate gauges, duration metrics, and top customer locations.
  - Admin can provision new users, change roles, deactivate accounts, and update passwords.
  - All admin endpoints are protected; non-admin users receive 403 Forbidden.

---

### Milestone F9: Comprehensive Verification, Accessibility & Responsive Polish
- **Goal:** Full end-to-end verification, keyboard accessibility (WCAG 2.1 AA), responsive testing across desktop/tablet/mobile, and static analysis.
- **Scope:**
  - Verify all 16 screens against backend REST contracts.
  - Verify keyboard focus management and screen-reader dialog labels.
  - Verify responsive behavior on mobile (<= 768px), tablet (768px-1199px), and desktop (>= 1200px).
  - Execute automated Maven tests and CodeRabbit review.
- **Acceptance Criteria:**
  - Zero console errors, zero accessibility regressions.
  - Seamless navigation between all screens.
  - Zero modifications to the frozen backend.
