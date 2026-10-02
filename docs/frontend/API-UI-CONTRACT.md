# Enterprise AI-CRM Platform (`CS-CRM-2026`)
# Exhaustive API-to-UI Contract Specification
# Document Version: 2.0.0 — Status: Approved Verified Baseline

---

## 1. Architectural Invariants & Boundary Rules

### 1.1 Strict Decoupled Boundary
The frontend communicates with the platform strictly as an HTTP client over the `/api/v1/*` REST interface:

```
[ Vaadin View (Java) ]
        ↓
[ Frontend API Service (Java) ]
        ↓
[ Spring 6 RestClient (Bearer JWT Injection) ]
        ↓ (HTTP / JSON / Multipart)
[ Existing Backend REST Controllers (@RestController) ]
        ↓
[ Backend Application Services (@Service) ]
        ↓
[ MySQL 8.4 / Redis 7.x Streams / Google Gemini ]
```

**MANDATORY ARCHITECTURAL RULES:**
- The frontend **MUST NOT** inject Spring Data repositories directly.
- The frontend **MUST NOT** inject backend `@Service` beans directly.
- The frontend **MUST NOT** connect directly to MySQL, Redis, or Google Gemini.
- The frontend **MUST NOT** duplicate domain business logic (e.g. AST compilation, outbox event generation).
- All requests are authenticated via `Authorization: Bearer <token>` HTTP header using the 1-hour stateless JWT acquired at login.

### 1.2 Global Envelopes
Every backend response conforms to one of two immutable Jackson-serialized envelopes:

#### 1.2.1 Standard Success Envelope (`com.crm.platform.common.dto.ApiResponse<T>`)
```json
{
  "success": true,
  "data": { ... },
  "metadata": {
    "timestamp": "2026-10-02T19:30:00Z",
    "requestId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 1420,
      "totalPages": 71,
      "isFirst": true,
      "isLast": false
    }
  }
}
```
*Note: `pagination` is present only when the underlying service returns a Spring Data `Page<T>` wrapped via `PageMetadata.fromPage(page)`.*

#### 1.2.2 Standard Error Envelope (`com.crm.platform.common.dto.ErrorResponse`)
```json
{
  "success": false,
  "error": {
    "code": "ERR_VALIDATION",
    "message": "Validation failed for request payload",
    "timestamp": "2026-10-02T19:30:00Z",
    "requestId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "details": [
      {
        "field": "email",
        "rejectedValue": "invalid-email-format",
        "message": "Email must be a valid email address"
      }
    ]
  }
}
```

#### 1.2.3 Error Codes Inventory
| Error Code | HTTP Status | Description |
| :--- | :--- | :--- |
| `ERR_UNAUTHORIZED` | 401 Unauthorized | Invalid credentials or deactivated account |
| `ERR_ACCESS_DENIED` | 403 Forbidden | User lacks required role authority (`ROLE_ADMIN`) |
| `ERR_NOT_FOUND` | 404 Not Found | Resource with specified ID does not exist |
| `ERR_RESOURCE_CONFLICT`| 409 Conflict | Unique constraint violation (duplicate username or email) |
| `ERR_VALIDATION` | 400 Bad Request | Payload failed JSR-380 / Bean Validation constraints |
| `ERR_INVALID_REQUEST` | 400 Bad Request | Malformed JSON AST, invalid operator/field mismatch |
| `ERR_ZERO_AUDIENCE` | 400 Bad Request | Attempted to launch campaign against segment with 0 audience |
| `ERR_INVALID_STATE` | 400 Bad Request | Attempted to launch or update non-`DRAFT` campaign |
| `ERR_INTERNAL` | 500 Internal Error | Unhandled server exception |

---

## 2. Exhaustive API Endpoint Inventory (36 Operations Across 9 Controllers)

---

### 2.1 Authentication Controller (`com.crm.platform.security.controller.AuthController`)
**Base Path:** `/api/v1/auth`

#### Endpoint 1: Login & Token Acquisition
- **Method & Path:** `POST /api/v1/auth/login`
- **Authentication Required:** No (Public / `.permitAll()`)
- **Role Required:** None
- **Path Parameters:** None
- **Query Parameters:** None
- **Request Headers:** `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.security.dto.LoginRequest`
  ```json
  {
    "username": "admin",
    "password": "Password123!"
  }
  ```
  - Validation: `@NotBlank` username, `@NotBlank` password
- **Response DTO:** `com.crm.platform.security.dto.LoginResponse` inside `ApiResponse<LoginResponse>`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "username": "admin",
      "role": "ROLE_ADMIN"
    }
  }
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `401 Unauthorized`: Bad credentials or account deactivated (`ERR_UNAUTHORIZED`)
  - `400 Bad Request`: Validation failure on blank fields (`ERR_VALIDATION`)
  - `500 Internal Server Error`: Server failure (`ERR_INTERNAL`)

---

### 2.2 User Management Controller (`com.crm.platform.user.controller.UserController`)
**Base Path:** `/api/v1/users`  
**Security Scope:** Governed by `requestMatchers("/api/v1/users/**").hasRole("ADMIN")`. All operations require `ROLE_ADMIN`.

#### Endpoint 2: Create User
- **Method & Path:** `POST /api/v1/users`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`
- **Path Parameters:** None
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.user.dto.UserCreateRequest`
  ```json
  {
    "username": "marketer_jane",
    "email": "jane@enterprise.com",
    "password": "Password123!",
    "role": "ROLE_MARKETER"
  }
  ```
  - Validation: `@NotBlank`, `@Size(min=1, max=50)` username; `@NotBlank`, `@Email`, `@Size(max=255)` email; `@NotBlank` password; `@NotNull` role (`RoleEnum`: `ROLE_ADMIN`, `ROLE_MARKETER`)
- **Response DTO:** `com.crm.platform.user.dto.UserResponse` inside `ApiResponse<UserResponse>`
  ```json
  {
    "id": 2,
    "username": "marketer_jane",
    "email": "jane@enterprise.com",
    "role": "ROLE_MARKETER",
    "isActive": true,
    "createdAt": "2026-10-02T19:00:00Z",
    "updatedAt": "2026-10-02T19:00:00Z"
  }
  ```
- **Success Status:** `201 Created` with `Location: /api/v1/users/{id}`
- **Expected Error Responses:**
  - `400 Bad Request`: JSR-380 validation error (`ERR_VALIDATION`)
  - `401 Unauthorized`: Token missing, expired, or invalid (`ERR_UNAUTHORIZED`)
  - `403 Forbidden`: Caller is not `ROLE_ADMIN` (`ERR_ACCESS_DENIED`)
  - `409 Conflict`: Username or email already registered (`ERR_RESOURCE_CONFLICT`)

#### Endpoint 3: List Users
- **Method & Path:** `GET /api/v1/users`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`
- **Path Parameters:** None
- **Query Parameters:** `page` (int, default 0), `size` (int, default 10), `sort` (String, default "id")
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `List<com.crm.platform.user.dto.UserResponse>` inside `ApiResponse<List<UserResponse>>` with `PageMetadata`
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`, `403 Forbidden`

#### Endpoint 4: Get User by ID
- **Method & Path:** `GET /api/v1/users/{id}`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.user.dto.UserResponse` inside `ApiResponse<UserResponse>`
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `401 Unauthorized`, `403 Forbidden`
  - `404 Not Found`: User ID does not exist (`ERR_NOT_FOUND`)

#### Endpoint 5: Update User Role
- **Method & Path:** `PATCH /api/v1/users/{id}/role`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.user.dto.UserRoleUpdateRequest`
  ```json
  {
    "role": "ROLE_ADMIN"
  }
  ```
  - Validation: `@NotNull` role (`RoleEnum`)
- **Response DTO:** `com.crm.platform.user.dto.UserResponse` inside `ApiResponse<UserResponse>`
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `400 Bad Request`: Validation failure or self-demotion constraint (`ERR_VALIDATION` / `ERR_INVALID_REQUEST`)
  - `401 Unauthorized`, `403 Forbidden`
  - `404 Not Found`: User not found (`ERR_NOT_FOUND`)

#### Endpoint 6: Deactivate User
- **Method & Path:** `PATCH /api/v1/users/{id}/deactivate`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.user.dto.UserResponse` inside `ApiResponse<UserResponse>` (with `isActive: false`)
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `400 Bad Request`: Admin attempting to deactivate their own account (`ERR_INVALID_REQUEST`)
  - `401 Unauthorized`, `403 Forbidden`
  - `404 Not Found`: User not found (`ERR_NOT_FOUND`)

#### Endpoint 7: Update User Password
- **Method & Path:** `PATCH /api/v1/users/{id}/password`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.user.dto.UserPasswordUpdateRequest`
  ```json
  {
    "password": "NewCompliantPassword123!"
  }
  ```
  - Validation: `@NotBlank` password
- **Response DTO:** `Map<String, String>` inside `ApiResponse<Map<String, String>>` -> `{"message": "Password updated successfully"}`
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `400 Bad Request`: Password blank or fails password policy (`ERR_VALIDATION`)
  - `401 Unauthorized`, `403 Forbidden`
  - `404 Not Found`: User not found (`ERR_NOT_FOUND`)

---

### 2.3 Customer Controller (`com.crm.platform.customer.controller.CustomerController`)
**Base Path:** `/api/v1/customers`

#### Endpoint 8: Create Customer
- **Method & Path:** `POST /api/v1/customers`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** None
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.customer.dto.CustomerRequestDto`
  ```json
  {
    "firstName": "Aarav",
    "lastName": "Sharma",
    "email": "aarav.sharma@example.com",
    "phone": "+919876543210",
    "city": "Delhi",
    "country": "India",
    "totalSpend": 12500.00,
    "visitCount": 8,
    "lastActiveDate": "2026-09-15",
    "tags": ["VIP", "Retail"]
  }
  ```
  - Validation: `@NotBlank`, `@Size(max=100)` firstName; `@NotBlank`, `@Size(max=100)` lastName; `@NotBlank`, `@Email`, `@Size(max=255)` email; `@Size(max=30)` phone; `@Size(max=100)` city; `@Size(max=100)` country; `@DecimalMin("0.00")` totalSpend; `@Min(0)` visitCount; `lastActiveDate` (LocalDate); `tags` (Set<String>).
- **Response DTO:** `com.crm.platform.customer.dto.CustomerResponseDto` inside `ApiResponse<CustomerResponseDto>`
  ```json
  {
    "id": 101,
    "customerId": 101,
    "firstName": "Aarav",
    "lastName": "Sharma",
    "email": "aarav.sharma@example.com",
    "phone": "+919876543210",
    "city": "Delhi",
    "country": "India",
    "totalSpend": 12500.00,
    "visitCount": 8,
    "lastActiveDate": "2026-09-15",
    "tags": ["VIP", "Retail"],
    "createdAt": "2026-10-02T19:00:00Z",
    "updatedAt": "2026-10-02T19:00:00Z"
  }
  ```
- **Success Status:** `201 Created` with `Location: /api/v1/customers/{id}`
- **Expected Error Responses:**
  - `400 Bad Request`: Validation failure (`ERR_VALIDATION`)
  - `401 Unauthorized` (`ERR_UNAUTHORIZED`)
  - `409 Conflict`: Email already exists (`ERR_RESOURCE_CONFLICT`)

#### Endpoint 9: Get Customer by ID
- **Method & Path:** `GET /api/v1/customers/{id}`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.customer.dto.CustomerResponseDto` inside `ApiResponse<CustomerResponseDto>`
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `401 Unauthorized`
  - `404 Not Found`: Customer does not exist or has been soft-deleted (`ERR_NOT_FOUND`)

#### Endpoint 10: Patch Customer
- **Method & Path:** `PATCH /api/v1/customers/{id}`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.customer.dto.CustomerPatchRequestDto`
  - Validation: All fields optional; `@Size(max=100)` firstName/lastName/city/country; `@Email` email; `@DecimalMin("0.00")` totalSpend; `@Min(0)` visitCount.
- **Response DTO:** `com.crm.platform.customer.dto.CustomerResponseDto` inside `ApiResponse<CustomerResponseDto>`
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `400 Bad Request`: Validation failure
  - `401 Unauthorized`
  - `404 Not Found`: Customer not found
  - `409 Conflict`: Target email conflicts with another customer

#### Endpoint 11: Soft Delete Customer (Admin Only)
- **Method & Path:** `DELETE /api/v1/customers/{id}`
- **Authentication Required:** Yes
- **Role Required:** **`ROLE_ADMIN` only** (`requestMatchers(HttpMethod.DELETE, "/api/v1/customers/**").hasRole("ADMIN")`)
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response:** `Void` (Empty HTTP body)
- **Success Status:** `204 No Content`
- **Expected Error Responses:**
  - `401 Unauthorized`
  - `403 Forbidden`: User has `ROLE_MARKETER` (`ERR_ACCESS_DENIED`)
  - `404 Not Found`: Customer not found (`ERR_NOT_FOUND`)

#### Endpoint 12: Search & Filter Customers
- **Method & Path:** `GET /api/v1/customers`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** None
- **Query Parameters:**
  - `firstName` (String, optional)
  - `lastName` (String, optional)
  - `email` (String, optional)
  - `city` (String, optional)
  - `country` (String, optional)
  - `tag` (String, optional)
  - `page` (int, default 0)
  - `size` (int, default 20)
  - `sort` (String, e.g., `totalSpend,desc`)
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `List<com.crm.platform.customer.dto.CustomerResponseDto>` inside `ApiResponse<List<CustomerResponseDto>>` with `PageMetadata`
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`

#### Endpoint 13: Count Active Customers
- **Method & Path:** `GET /api/v1/customers/count`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** None
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `Map<String, Long>` inside `ApiResponse<Map<String, Long>>` -> `{"totalActiveCustomers": 4520}`
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`

---

### 2.4 Segment Controller (`com.crm.platform.segment.controller.SegmentController`)
**Base Path:** `/api/v1/segments`

#### Endpoint 14: Create Segment
- **Method & Path:** `POST /api/v1/segments`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** None
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.segment.dto.SegmentCreateRequest`
  ```json
  {
    "name": "Delhi High Spenders",
    "description": "Customers in Delhi with total spend >= 10000",
    "rules": {
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
          "value": 10000
        }
      ]
    }
  }
  ```
  - Validation: `@NotBlank`, `@Size(max=100)` name; `@Size(max=500)` description; `@NotNull` rules (JsonNode).
- **Response DTO:** `com.crm.platform.segment.dto.SegmentResponse` inside `ApiResponse<SegmentResponse>`
  ```json
  {
    "id": 12,
    "name": "Delhi High Spenders",
    "description": "Customers in Delhi with total spend >= 10000",
    "rules": { ... },
    "createdBy": 1,
    "createdByName": "admin",
    "createdAt": "2026-10-02T19:15:00Z",
    "updatedAt": "2026-10-02T19:15:00Z"
  }
  ```
- **Success Status:** `201 Created` with `Location: /api/v1/segments/{id}`
- **Expected Error Responses:**
  - `400 Bad Request`: Malformed rule tree, unknown field, unsupported operator, or depth > 10 (`ERR_VALIDATION` / `ERR_INVALID_REQUEST`)
  - `401 Unauthorized`

#### Endpoint 15: Get Segment by ID
- **Method & Path:** `GET /api/v1/segments/{id}`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.segment.dto.SegmentResponse` inside `ApiResponse<SegmentResponse>`
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`, `404 Not Found`

#### Endpoint 16: Update Segment
- **Method & Path:** `PATCH /api/v1/segments/{id}`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.segment.dto.SegmentUpdateRequest`
  - Fields: `name` (String, max 100), `description` (String, max 500), `rules` (JsonNode)
- **Response DTO:** `com.crm.platform.segment.dto.SegmentResponse` inside `ApiResponse<SegmentResponse>`
- **Success Status:** `200 OK`
- **Expected Error Responses:** `400 Bad Request`, `401 Unauthorized`, `404 Not Found`

#### Endpoint 17: Delete Segment
- **Method & Path:** `DELETE /api/v1/segments/{id}`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response:** `Void`
- **Success Status:** `204 No Content`
- **Expected Error Responses:** `401 Unauthorized`, `404 Not Found`

#### Endpoint 18: List Segments
- **Method & Path:** `GET /api/v1/segments`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** None
- **Query Parameters:** `page` (default 0), `size` (default 10), `sort` (default "id")
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `List<com.crm.platform.segment.dto.SegmentResponse>` inside `ApiResponse<List<SegmentResponse>>` with `PageMetadata`
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`

#### Endpoint 19: Preview Segment Audience Count
- **Method & Path:** `POST /api/v1/segments/{id}/preview`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.segment.dto.SegmentPreviewResponse` inside `ApiResponse<SegmentPreviewResponse>`
  ```json
  {
    "segmentId": 12,
    "segmentName": "Delhi High Spenders",
    "matchedAudienceCount": 340,
    "evaluatedAt": "2026-10-02T19:20:00Z"
  }
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`, `404 Not Found`

#### Endpoint 20: Get Segment Evaluated Members
- **Method & Path:** `GET /api/v1/segments/{id}/members`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** `page` (default 0), `size` (default 20), `sort` (default "id")
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `List<com.crm.platform.customer.dto.CustomerResponseDto>` inside `ApiResponse<List<CustomerResponseDto>>` with `PageMetadata`
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`, `404 Not Found`

---

### 2.5 Campaign Controller (`com.crm.platform.campaign.controller.CampaignController`)
**Base Path:** `/api/v1/campaigns`

#### Endpoint 21: Create Campaign
- **Method & Path:** `POST /api/v1/campaigns`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** None
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.campaign.dto.CampaignCreateRequest`
  ```json
  {
    "name": "Diwali Festival Flash Sale",
    "description": "Exclusive offer for top spenders",
    "segmentId": 12,
    "messageTemplate": "Dear {firstName}, enjoy 20% off your next purchase in {city}!",
    "personalizationEnabled": true
  }
  ```
  - Validation: `@NotBlank`, `@Size(max=100)` name; `@Size(max=500)` description; `@NotNull`, `@Positive` segmentId; `@NotBlank`, `@Size(max=1000)` messageTemplate; `personalizationEnabled` (Boolean).
- **Response DTO:** `com.crm.platform.campaign.dto.CampaignResponse` inside `ApiResponse<CampaignResponse>`
  - Status is initialized to `DRAFT`.
- **Success Status:** `201 Created` with `Location: /api/v1/campaigns/{id}`
- **Expected Error Responses:**
  - `400 Bad Request`: Validation failure (`ERR_VALIDATION`)
  - `401 Unauthorized`
  - `404 Not Found`: Target segmentId does not exist (`ERR_NOT_FOUND`)

#### Endpoint 22: Get Campaign by ID
- **Method & Path:** `GET /api/v1/campaigns/{id}`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.campaign.dto.CampaignResponse` inside `ApiResponse<CampaignResponse>`
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`, `404 Not Found`

#### Endpoint 23: Patch Campaign
- **Method & Path:** `PATCH /api/v1/campaigns/{id}`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.campaign.dto.CampaignUpdateRequest`
  - Validation: All fields optional; applies only to campaigns with status `DRAFT`.
- **Response DTO:** `com.crm.platform.campaign.dto.CampaignResponse` inside `ApiResponse<CampaignResponse>`
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `400 Bad Request`: Validation failure or campaign is not in `DRAFT` status (`ERR_INVALID_STATE`)
  - `401 Unauthorized`, `404 Not Found`

#### Endpoint 24: Delete Campaign (Admin Only)
- **Method & Path:** `DELETE /api/v1/campaigns/{id}`
- **Authentication Required:** Yes
- **Role Required:** **`ROLE_ADMIN` only** (`requestMatchers(HttpMethod.DELETE, "/api/v1/campaigns/**").hasRole("ADMIN")`)
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response:** `Void`
- **Success Status:** `204 No Content`
- **Expected Error Responses:**
  - `401 Unauthorized`
  - `403 Forbidden`: Caller has `ROLE_MARKETER`
  - `404 Not Found`: Campaign not found

#### Endpoint 25: List Campaigns
- **Method & Path:** `GET /api/v1/campaigns`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** None
- **Query Parameters:**
  - `status` (`CampaignStatus` enum: `DRAFT`, `RUNNING`, `COMPLETED`, `FAILED`, optional)
  - `page` (int, default 0)
  - `size` (int, default 10)
  - `sort` (String, default "id")
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `List<com.crm.platform.campaign.dto.CampaignResponse>` inside `ApiResponse<List<CampaignResponse>>` with `PageMetadata`
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`

#### Endpoint 26: Launch Campaign
- **Method & Path:** `POST /api/v1/campaigns/{id}/launch`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.campaign.dto.CampaignLaunchResponse` inside `ApiResponse<CampaignLaunchResponse>`
  ```json
  {
    "campaignId": 105,
    "status": "RUNNING",
    "targetAudienceSize": 340,
    "message": "Campaign launched successfully. Delivery simulated asynchronously.",
    "launchedAt": "2026-10-02T19:25:00Z"
  }
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `400 Bad Request`: If target audience is 0 (`ERR_ZERO_AUDIENCE`) or campaign is not in `DRAFT` status (`ERR_INVALID_STATE`)
  - `401 Unauthorized`, `404 Not Found`

---

### 2.6 Delivery Controller (`com.crm.platform.delivery.controller.DeliveryController`)
**Base Path:** `/api/v1/campaigns`

#### Endpoint 27: Get Delivery Summary
- **Method & Path:** `GET /api/v1/campaigns/{id}/delivery-summary`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.delivery.dto.DeliverySummaryResponse` inside `ApiResponse<DeliverySummaryResponse>`
  ```json
  {
    "campaignId": 105,
    "campaignStatus": "RUNNING",
    "targetAudienceSize": 340,
    "pendingCount": 40,
    "sentCount": 290,
    "failedCount": 10,
    "completionPercentage": 88.24,
    "isTerminal": false
  }
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`, `404 Not Found`

#### Endpoint 28: Get Deliveries (Recipient Audit Log)
- **Method & Path:** `GET /api/v1/campaigns/{id}/deliveries`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:**
  - `status` (`DeliveryStatus` enum: `PENDING`, `SENT`, `FAILED`, optional)
  - `page` (int, default 0)
  - `size` (int, default 20)
  - `sort` (String, default "id")
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `List<com.crm.platform.delivery.dto.DeliveryRecordResponse>` inside `ApiResponse<List<DeliveryRecordResponse>>` with `PageMetadata`
  ```json
  [
    {
      "id": 8920,
      "campaignId": 105,
      "customerId": 101,
      "customerEmail": "aarav.sharma@example.com",
      "customerName": "Aarav Sharma",
      "message": "Dear Aarav, enjoy 20% off your next purchase in Delhi!",
      "status": "SENT",
      "failureReason": null,
      "processedAt": "2026-10-02T19:25:05Z",
      "createdAt": "2026-10-02T19:25:00Z"
    }
  ]
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`, `404 Not Found`

---

### 2.7 Upload Controller (`com.crm.platform.upload.controller.UploadController`)
**Base Path:** `/api/v1/uploads`

#### Endpoint 29: Bulk Customer Upload
- **Method & Path:** `POST /api/v1/uploads/bulk`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER` (`@PreAuthorize("hasAnyRole('ADMIN', 'MARKETER')")`)
- **Path Parameters:** None
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: multipart/form-data`
- **Multipart Form Part:** `file` (`MultipartFile` — CSV or XLSX, max 10MB)
- **Response DTO:** `com.crm.platform.upload.dto.UploadResultResponse` inside `ApiResponse<UploadResultResponse>`
  ```json
  {
    "uploadId": 45,
    "fileName": "q3_customers.csv",
    "status": "PARTIAL_SUCCESS",
    "totalRecords": 100,
    "successfulRecords": 85,
    "failedRecords": 15,
    "errors": [
      {
        "row": 12,
        "email": "invalid-email-row",
        "reason": "Email must be a valid email address"
      }
    ]
  }
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `400 Bad Request`: Empty file or parsing failure (`ERR_VALIDATION` / `ERR_INVALID_REQUEST`)
  - `401 Unauthorized`
  - `415 Unsupported Media Type`: Non-CSV/XLSX file format

#### Endpoint 30: Get Upload History
- **Method & Path:** `GET /api/v1/uploads/history`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER` (`@PreAuthorize("hasAnyRole('ADMIN', 'MARKETER')")`)
- **Path Parameters:** None
- **Query Parameters:** `page` (int, default 0), `size` (int, default 20), `sort`
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `List<com.crm.platform.upload.dto.UploadHistoryDto>` inside `ApiResponse<List<UploadHistoryDto>>` with `PageMetadata`
  ```json
  [
    {
      "id": 45,
      "uploadedBy": 1,
      "fileName": "q3_customers.csv",
      "fileType": "CSV",
      "totalRows": 100,
      "successCount": 85,
      "failureCount": 15,
      "status": "PARTIAL_SUCCESS",
      "createdAt": "2026-10-02T19:10:00Z"
    }
  ]
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`

---

### 2.8 Reporting Controller (`com.crm.platform.reporting.controller.ReportingController`)
**Base Path:** `/api/v1/reports`

#### Endpoint 31: Get Campaign Report
- **Method & Path:** `GET /api/v1/reports/campaigns/{id}`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.reporting.dto.CampaignReportResponse` inside `ApiResponse<CampaignReportResponse>`
  ```json
  {
    "campaignId": 105,
    "campaignName": "Diwali Festival Flash Sale",
    "status": "COMPLETED",
    "targetAudienceSize": 340,
    "metrics": {
      "sent": 328,
      "failed": 12,
      "deliveryRatePercentage": 96.47
    },
    "timeline": {
      "launchedAt": "2026-10-02T19:25:00Z",
      "completedAt": "2026-10-02T19:26:15Z",
      "durationSeconds": 75
    }
  }
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`, `404 Not Found`

#### Endpoint 32: Get Customer Demographic & Spend Overview
- **Method & Path:** `GET /api/v1/reports/customers/overview`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** None
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.reporting.dto.CustomerOverviewReportResponse` inside `ApiResponse<CustomerOverviewReportResponse>`
  ```json
  {
    "totalActiveCustomers": 4520,
    "grossCustomerSpend": 1284500.00,
    "averageSpendPerCustomer": 284.18,
    "totalVisits": 18450,
    "averageVisitsPerCustomer": 4.08,
    "topLocations": [
      { "city": "Delhi", "customerCount": 1420 },
      { "city": "Mumbai", "customerCount": 1180 },
      { "city": "Bangalore", "customerCount": 850 }
    ]
  }
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`

#### Endpoint 33: Get Campaign AI Summary
- **Method & Path:** `GET /api/v1/reports/campaigns/{id}/ai-summary`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** `id` (`Long`)
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `com.crm.platform.ai.dto.AiCampaignSummaryResponse` inside `ApiResponse<AiCampaignSummaryResponse>`
  ```json
  {
    "campaignId": 105,
    "aiSummary": "The Diwali Festival Flash Sale campaign completed with an exceptional 96.47% delivery success rate across 340 recipients."
  }
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`, `404 Not Found`, `500 Internal Error`

#### Endpoint 34: Get Campaign History
- **Method & Path:** `GET /api/v1/reports/campaigns/history`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER`
- **Path Parameters:** None
- **Query Parameters:** `page` (int, default 0), `size` (int, default 10), `sort` (String, default "id")
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `List<com.crm.platform.reporting.dto.CampaignHistoryItemResponse>` inside `ApiResponse<List<CampaignHistoryItemResponse>>` with `PageMetadata`
  ```json
  [
    {
      "campaignId": 105,
      "campaignName": "Diwali Festival Flash Sale",
      "status": "COMPLETED",
      "targetAudienceSize": 340,
      "sentCount": 328,
      "failedCount": 12,
      "deliveryRatePercentage": 96.47,
      "launchedAt": "2026-10-02T19:25:00Z",
      "completedAt": "2026-10-02T19:26:15Z"
    }
  ]
  ```
- **Success Status:** `200 OK`
- **Expected Error Responses:** `401 Unauthorized`

---

### 2.9 AI Controller (`com.crm.platform.ai.controller.AiController`)
**Base Path:** `/api/v1/ai`

#### Endpoint 35: Generate Segment Rules from Natural Language
- **Method & Path:** `POST /api/v1/ai/segments/generate-rules`
- **Authentication Required:** Yes
- **Role Required:** `ROLE_ADMIN`, `ROLE_MARKETER` (`@PreAuthorize("hasAnyRole('ADMIN', 'MARKETER')")`)
- **Path Parameters:** None
- **Query Parameters:** None
- **Request Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body DTO:** `com.crm.platform.ai.dto.AiRuleGenerationRequest`
  ```json
  {
    "prompt": "Customers living in Mumbai who visited at least 4 times and spent over 5000"
  }
  ```
  - Validation: `@NotBlank`, `@Size(min=10, max=500)` prompt.
- **Response DTO:** `com.crm.platform.ai.dto.AiRuleGenerationResponse` inside `ApiResponse<AiRuleGenerationResponse>`
  ```json
  {
    "prompt": "Customers living in Mumbai who visited at least 4 times and spent over 5000",
    "ruleTree": {
      "operator": "AND",
      "conditions": [
        { "field": "city", "op": "EQUALS", "value": "Mumbai" },
        { "field": "visitCount", "op": "GREATER_THAN_OR_EQUAL", "value": 4 },
        { "field": "totalSpend", "op": "GREATER_THAN", "value": 5000 }
      ]
    },
    "isValidated": true,
    "fallback": false
  }
  ```
  - *Note: `isFallback` (or JSON alias `fallback`) indicates whether Google Gemini executed or the local deterministic fallback engine was engaged.*
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `400 Bad Request`: Prompt < 10 characters or blank (`ERR_VALIDATION`)
  - `401 Unauthorized`
  - `500 Internal Error`: External AI and fallback failure (`ERR_INTERNAL`)

#### Endpoint 36: Get AI Segment Audits Log (Admin Only)
- **Method & Path:** `GET /api/v1/ai/segments/audits`
- **Authentication Required:** Yes
- **Role Required:** **`ROLE_ADMIN` only** (`@PreAuthorize("hasRole('ADMIN')")` and `requestMatchers(HttpMethod.GET, "/api/v1/ai/segments/audits").hasRole("ADMIN")`)
- **Path Parameters:** None
- **Query Parameters:** `page` (int, default 0), `size` (int, default 10), `sort` (String, default "id")
- **Request Headers:** `Authorization: Bearer <token>`
- **Request Body:** None
- **Response DTO:** `List<com.crm.platform.ai.dto.AiSegmentAuditDto>` inside `ApiResponse<List<AiSegmentAuditDto>>` with `PageMetadata`
  ```json
  [
    {
      "id": 1,
      "username": "admin",
      "promptText": "Customers in Delhi",
      "generatedRules": { "operator": "AND", "conditions": [ ... ] },
      "actionTaken": "DISCARDED",
      "segmentId": null,
      "createdAt": "2026-10-02T19:12:00Z"
    }
  ]
  ```
  - *Authoritative Fields: `id`, `username`, `promptText`, `generatedRules`, `actionTaken`, `segmentId`, `createdAt`. NO latencyMs or modelName fields exist in DTO.*
- **Success Status:** `200 OK`
- **Expected Error Responses:**
  - `401 Unauthorized`
  - `403 Forbidden`: Caller has `ROLE_MARKETER` (`ERR_ACCESS_DENIED`)
