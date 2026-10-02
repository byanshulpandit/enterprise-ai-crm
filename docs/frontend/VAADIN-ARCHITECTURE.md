# Enterprise AI-CRM Platform (`CS-CRM-2026`)
# Vaadin Flow Architecture & Technical Design Specification
# Document Version: 2.0.0 — Status: Approved Verified Baseline

---

## 1. Technology Selection & Architectural Boundary

### 1.1 Selected Framework & Version Matrix
| Component | Selected Version | Compatibility & Rationale |
| :--- | :--- | :--- |
| **Java Runtime** | **Java 21 (LTS)** | Matches existing backend baseline. |
| **Backend Framework** | **Spring Boot 3.3.3** | **FROZEN BASELINE.** No upgrade to Spring Boot 3.4.x or 4.x. |
| **Vaadin Flow Framework** | **Vaadin 24.4.15 (LTS)** | **Explicitly verified for Spring Boot 3.3.x and Java 21.** Fully compatible with Spring Security 6.3.x, Jakarta EE 10, and Tomcat 10.1.x without dependency conflicts. |
| **Build System** | **Apache Maven 3.9+** | Managed via `vaadin-bom:24.4.15` in `<dependencyManagement>`. |
| **UI Paradigm** | **Java-based server-side web frontend using Vaadin Flow** | Type-safe Java components, server-side DOM state, and type-safe routing. The project does not use a separate React, Next.js, or Vite SPA. |

### 1.2 Mandatory Architectural Boundary (Vaadin → REST → Backend)
The frontend architecture enforces a strict decoupling boundary between the UI layer and the backend persistence/business engine:

```
[ Vaadin View (Java Component) ]
            ↓
[ Frontend API Service (Java Service) ]
            ↓
[ Spring 6 RestClient (HTTP / Bearer Token Interceptor) ]
            ↓
    ==== REST API BOUNDARY (/api/v1/*) ====
            ↓
[ Spring Boot RestControllers (@RestController) ]
            ↓
[ Backend Application Services (@Service) ]
            ↓
[ Persistence & Infra: MySQL 8.4 / Redis 7.x Streams / Google Gemini ]
```

**EXPLICIT RESTRICTIONS & INVARIANTS:**
- The frontend code **MUST NOT** inject Spring Data repositories (`CustomerRepository`, `CampaignRepository`, etc.) directly.
- The frontend code **MUST NOT** call backend business services (`CustomerService`, `CampaignService`, `DeliveryService`, etc.) directly.
- The frontend code **MUST NOT** bypass REST controllers.
- The frontend code **MUST NOT** duplicate domain business logic (e.g. outbox event generation, criteria tree compilation).
- The frontend code **MUST NOT** access MySQL directly.
- The frontend code **MUST NOT** access Redis directly.
- The frontend code **MUST NOT** access Google Gemini directly.
- **ALL** data read, write, update, delete, and execution triggers MUST traverse the existing `/api/v1/*` REST interface via HTTP using `RestClient`.

---

## 2. Target Package Structure

The frontend is organized cleanly under `com.crm.platform.frontend` in `src/main/java`:

```
src/main/java/com/crm/platform/frontend/
├── config/
│   ├── VaadinConfiguration.java           # Vaadin servlet & resource mapping
│   ├── RestClientConfiguration.java       # Spring 6 RestClient with Bearer Auth interceptor
│   └── JacksonFrontendConfig.java         # ObjectMapper tuning for AST trees
├── security/
│   ├── FrontendSecurityContext.java       # Thread-local / VaadinSession accessor
│   ├── VaadinAuthService.java             # Login/Logout & token lifecycle manager
│   ├── RouteAccessChecker.java            # BeforeEnterObserver enforcing role boundaries
│   └── UserSession.java                   # User session bean holding JWT & UserSummary
├── layout/
│   ├── MainLayout.java                    # AppLayout shell with topbar & sidebar
│   ├── TopNavbar.java                     # Utility bar, user chip, breadcrumbs, signout
│   ├── SidebarNav.java                    # Collapsible grouped navigation drawer
│   └── NavSection.java                    # Grouped navigation item container
├── views/
│   ├── login/
│   │   └── LoginView.java                 # SCR-01: Public authentication screen
│   ├── dashboard/
│   │   └── DashboardView.java             # SCR-02: Executive & operational dashboard
│   ├── customers/
│   │   ├── CustomerListView.java          # SCR-03: Paginated customer directory & filters
│   │   ├── CustomerDetailView.java        # SCR-04: Customer profile & inspector
│   │   └── CustomerFormDialog.java        # Customer creation & edit modal dialog
│   ├── segments/
│   │   ├── SegmentListView.java           # SCR-05: Segment catalog
│   │   ├── SegmentBuilderView.java        # SCR-06: Visual AST builder & editor
│   │   ├── SegmentMembersView.java        # SCR-07: Evaluated member inspector
│   │   └── AiSegmentModal.java            # AI natural-language prompt dialog
│   ├── campaigns/
│   │   ├── CampaignListView.java          # SCR-08: Campaign portfolio
│   │   ├── CampaignEditorView.java        # SCR-09: Campaign authoring studio
│   │   ├── CampaignMonitorView.java       # SCR-10: Near-real-time delivery monitoring console
│   │   └── CampaignLaunchDialog.java      # Pre-flight safety confirmation modal
│   ├── uploads/
│   │   └── BulkUploadView.java            # SCR-11: Drag-and-drop ingestion & history
│   ├── reports/
│   │   ├── ReportsOverviewView.java       # SCR-12: Business intelligence analytics center
│   │   └── CampaignReportView.java        # SCR-13: Deep-dive campaign report & AI summary
│   ├── admin/
│   │   ├── UserManagementView.java        # SCR-14: Admin user lifecycle console
│   │   ├── UserFormDialog.java            # User creation & edit modal
│   │   ├── PasswordResetDialog.java       # Admin password update modal
│   │   └── AiAuditsView.java              # SCR-15: AI prompt & audit log
│   └── errors/
│       ├── AccessDeniedView.java          # 403 Forbidden view
│       ├── NotFoundView.java              # 404 Route not found view
│       └── SystemErrorView.java           # 500 Uncaught exception view
├── components/
│   ├── builder/                           # Visual Rule Tree Components
│   │   ├── RuleTreeContainer.java         # Root AST canvas
│   │   ├── RuleGroupComponent.java        # Logical group (AND/OR, child nodes)
│   │   ├── ConditionRowComponent.java     # Leaf condition (Field, Op, Value)
│   │   └── ValueFieldFactory.java         # Type-specific value input controls
│   ├── common/                            # Reusable Enterprise Widgets
│   │   ├── KpiStatCard.java               # Metric KPI card with trend & icon
│   │   ├── StatusBadge.java               # Semantic status pill (Emerald, Amber, Red)
│   │   ├── ConfirmDialog.java             # Accessible confirmation modal
│   │   ├── StateViewContainer.java        # Loading, Empty, Error state switcher
│   │   └── TokenInsertField.java          # Message template with personalization chips
│   └── charts/                            # Pure Java / CSS Gauge & Metric Bars
│       ├── DeliveryProgressGauge.java     # Linear delivery completion bar
│       └── TopLocationsBarChart.java      # Demographic spend & volume bars
├── client/
│   ├── BackendApiClient.java              # Typed HTTP client wrapper around RestClient
│   ├── ApiEndpointConstants.java          # Authoritative backend URI constants
│   └── ApiException.java                  # Structured exception wrapping ErrorResponse
├── service/                               # Thin Frontend Services consuming RestClient
│   ├── CustomerApiService.java
│   ├── SegmentApiService.java
│   ├── CampaignApiService.java
│   ├── DeliveryApiService.java
│   ├── UploadApiService.java
│   ├── ReportApiService.java
│   ├── UserApiService.java
│   └── AiApiService.java
└── util/
    ├── FormattingUtils.java               # Currency, numbers, ISO timestamps formatting
    └── NotificationUtils.java             # Standardized enterprise toast notifications
```

---

## 3. Authentication & JWT Expiration Semantics (Phase 5)

### 3.1 Token Lifetime vs. Vaadin Session Lifetime
The backend implements stateless JWT authentication with a **1-hour access token TTL** (`JwtProperties.DEFAULT_EXPIRATION_MS = 3600000L`). The backend **does not implement refresh tokens**.

- **Vaadin Session:** Retains UI component state in server memory for the duration of user interaction.
- **Backend JWT Token:** Expires exactly 1 hour after issuance.
- **Expiry Behavior:** The Vaadin session may still exist when the JWT token expires. Therefore, the frontend cannot assume a valid Vaadin session implies a valid backend token.

### 3.2 Error Interception & Automatic Logout
When any backend REST request returns HTTP `401 Unauthorized`:
1. The `RestClient` interceptor or `BackendApiClient` catches the 401 error.
2. It executes `userSession.clear()`, invalidating the stored JWT token and clearing the user credentials from `VaadinSession`.
3. It displays an explicit toast notification via `NotificationUtils.showError("Session expired. Please log in again.")`.
4. It programmatically redirects the browser to `/login`.
5. The frontend **DOES NOT** invent refresh-token mechanics.
6. The frontend **DOES NOT** silently retry expired authentication indefinitely.
7. The frontend **DOES NOT** continue displaying authenticated controls after backend authentication has expired.

```java
public class BearerTokenInterceptor implements ClientHttpRequestInterceptor {
    private final UserSession userSession;

    public BearerTokenInterceptor(UserSession userSession) {
        this.userSession = userSession;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                        ClientHttpRequestExecution execution) throws IOException {
        String token = userSession.getToken();
        if (token != null && !token.isBlank()) {
            request.getHeaders().setBearerAuth(token);
        }
        ClientHttpResponse response = execution.execute(request, body);
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            userSession.clear();
            UI currentUi = UI.getCurrent();
            if (currentUi != null) {
                currentUi.access(() -> {
                    NotificationUtils.showError("Session expired. Please log in again.");
                    currentUi.navigate(LoginView.class);
                });
            }
        }
        return response;
    }
}
```

---

## 4. Visual Segment Rule Builder Architecture (Phase 9)

### 4.1 Strict AST Grammar
The builder generates the exact JSON structure parsed by `SegmentRuleParser`:
- **Logical Node (Group):**
  ```json
  {
    "operator": "AND",
    "conditions": [ ... ]
  }
  ```
  *(Supported operators: `AND`, `OR`)*
- **Condition Node (Leaf):**
  ```json
  {
    "field": "city",
    "op": "EQUALS",
    "value": "Delhi"
  }
  ```

### 4.2 Canonical AST Example
Below is the canonical JSON structure produced by the visual builder and verified against backend parser integration tests:

```json
{
  "operator": "OR",
  "conditions": [
    {
      "operator": "AND",
      "conditions": [
        {
          "field": "city",
          "op": "EQUALS",
          "value": "Delhi"
        },
        {
          "field": "totalSpend",
          "op": "GREATER_THAN_OR_EQUAL",
          "value": 10000.00
        }
      ]
    },
    {
      "operator": "AND",
      "conditions": [
        {
          "field": "city",
          "op": "EQUALS",
          "value": "Mumbai"
        },
        {
          "field": "visitCount",
          "op": "LESS_THAN_OR_EQUAL",
          "value": 5
        }
      ]
    }
  ]
}
```

### 4.3 Field-to-Operator Mapping
| Field Name in AST | Target Class | Supported Operators in AST | Input Component |
| :--- | :--- | :--- | :--- |
| `city` | `String` | `EQUALS`, `NOT_EQUALS`, `CONTAINS`, `IN`, `NOT_IN` | TextField / TokenField for IN |
| `totalSpend` | `BigDecimal` | `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN_OR_EQUAL`, `IN`, `NOT_IN` | BigDecimalField (`0.00`) |
| `visitCount` | `Integer` | `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN_OR_EQUAL`, `IN`, `NOT_IN` | IntegerField |
| `lastActiveDate` | `LocalDate` | `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN_OR_EQUAL`, `IN`, `NOT_IN` | DatePicker (`YYYY-MM-DD`) |
| `tags` | `String` | `EQUALS`, `NOT_EQUALS`, `CONTAINS`, `IN`, `NOT_IN` | TextField / TokenField |

---

## 5. Near-Real-Time Delivery Monitoring Architecture (Phase 10)

The delivery monitoring console (SCR-10) uses **periodic polling** against the existing backend REST API. It does not introduce WebSockets or server push:

### 5.1 Polling Mechanics
1. **Trigger:** When `CampaignMonitorView` loads a campaign with `status == RUNNING`, it activates periodic polling:
   ```java
   UI.getCurrent().setPollInterval(2500); // Poll every 2.5 seconds
   ```
2. **Execution:** On each poll event, `DeliveryApiService` queries:
   ```
   GET /api/v1/campaigns/{id}/delivery-summary
   ```
3. **Telemetry Update:** The view updates:
   - `completionPercentage` on the linear progress bar
   - `sentCount`, `failedCount`, `pendingCount`, `targetAudienceSize` counters
4. **Stop Condition (Terminal State):**
   - When the response indicates `isTerminal == true` (or `campaignStatus` is `COMPLETED` or `FAILED`):
     ```java
     UI.getCurrent().setPollInterval(-1); // Polling permanently disabled
     ```
   - The status badge transitions to its terminal color (Completed Emerald / Failed Red).
   - The "Generate AI Performance Summary" button becomes active.
5. **Clean Exit:** If the user navigates away from `CampaignMonitorView`, polling is immediately disabled in `onDetach()`.

---

## 6. Table & Grid Performance Architecture (Phase 18)

- **Lazy Server-Side Pagination:** Data is fetched lazily using `DataProvider.fromCallbacks()`:
  - `query.getOffset()` and `query.getLimit()` map to Spring `page` and `size`.
  - Backend `PageMetadata.getTotalElements()` populates the grid's total count.
  - Zero full-table loading into browser memory; maximum 100 rows fetched per request.
