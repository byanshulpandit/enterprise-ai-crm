# AI-CRM

A modern, production-grade Customer Relationship Management (CRM) platform featuring a high-performance **React 19 + TypeScript + Vite + Tailwind CSS** frontend backed by a robust **Java 21 LTS + Spring Boot 3.3.3** REST API, **MySQL 8.4**, **Redis Streams**, and **Google Gemini AI**.

---

## 1. System Architecture

AI-CRM is engineered as a high-reliability enterprise application with clear separation between a static Single Page Application (SPA), an Nginx same-origin reverse proxy, and a stateless REST backend:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Client Browser                                   │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ http://localhost:8080/
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 Nginx 1.27 Reverse Proxy & Static Server                    │
│ - Serves pre-built React 19 SPA with gzip compression & immutable caching   │
│ - SPA routing fallback (try_files $uri $uri/ /index.html)                   │
│ - Proxies same-origin API requests (/api/*) to Spring Boot backend          │
│ - Restricts internal actuator and diagnostic endpoints                      │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ http://app:8080/api/
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 Spring Boot 3.3.3 Stateless REST Backend                    │
│ - Stateless JWT Bearer Authentication & Spring Security 6.x                 │
│ - Strict REST Endpoints (/api/v1/) with Uniform Success/Error Envelopes     │
│ - In-memory access token lifecycle (zero bearer tokens in local storage)    │
│ - Dynamic Boolean AST Segmentation Engine (depth <= 10, criteria compiler)  │
│ - Keyset-paginated Campaign Dispatch with Pessimistic Row Locking           │
│ - Streaming CSV/XLSX Bulk Ingestion with Row-Level Validation & Fallback    │
│ - Google Gemini AI natural language rule synthesis with fallback telemetry  │
└──────────────────┬───────────────────────────────────────┬──────────────────┘
                   │                                       │
                   ▼                                       ▼
┌──────────────────────────────────────┐ ┌────────────────────────────────────┐
│      MySQL 8.4 Relational Store      │ │   Redis 7 Streams Transport        │
│ - Sole persistent source of truth    │ │ - crm:campaign:deliveries:stream   │
│ - Transactional Outbox Pattern       │ │ - Consumer Group: workers          │
│ - Keyset pagination & row locking    │ │ - At-least-once with manual XACK   │
└──────────────────────────────────────┘ └────────────────────────────────────┘
```

---

## 2. Technology Stack

### Frontend (`crm-ui/`)
- **Core:** React 19, TypeScript (strict mode, `noEmit`, `verbatimModuleSyntax`)
- **Build & Dev:** Vite 6, `@vitejs/plugin-react`
- **Styling & Tokens:** Tailwind CSS v4, CSS Variables for light and dark themes
- **Component Primitives:** Radix UI (`@radix-ui/react-dialog`, `@radix-ui/react-select`, `@radix-ui/react-toast`, etc.)
- **Routing:** React Router v7
- **Data Fetching & Cache:** TanStack Query v5 (`@tanstack/react-query`)
- **Forms & Validation:** React Hook Form + Zod
- **Icons:** Lucide React
- **Data Visualization:** Recharts
- **Testing:** Vitest, React Testing Library, `@testing-library/jest-dom`

### Backend (`src/`)
- **Runtime & Language:** Java 21 LTS, OpenJDK
- **Framework:** Spring Boot 3.3.3, Spring Security 6.x, Spring Data JPA / Hibernate 6.5
- **Primary Database:** MySQL 8.4 (Sole persistent source of truth)
- **Async Messaging:** Redis 7 Streams (Lettuce client with Transactional Outbox)
- **File Parsing:** OpenCSV 5.9 (streaming), Apache POI 5.3.0 SAX (streaming)
- **Generative AI:** Google Gemini 1.5 Flash (with transparent deterministic fallback)
- **API Documentation:** SpringDoc OpenAPI 2.6.0 / Swagger UI
- **Observability:** Spring Boot Actuator, SLF4J + Logback with MDC correlation IDs
- **Testing:** JUnit 5, Mockito, Spring Security Test, Awaitility, GreenMail

---

## 3. Environment Variables & Configuration

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/crm_db?...` | JDBC URL for MySQL database |
| `SPRING_DATASOURCE_USERNAME` | `root` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `rootpassword` | Database password |
| `REDIS_HOST` | `redis` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |
| `JWT_SECRET` | *(required in prod)* | HMAC-SHA256 secret key (minimum 32 UTF-8 bytes) |
| `CRM_DELIVERY_PROVIDER` | `smtp` | Delivery provider (`smtp` or `simulated`) |
| `SMTP_HOST` | `smtp` | SMTP server host (`smtp` in Docker, localhost for local) |
| `SMTP_PORT` | `1025` | SMTP port (1025 for MailHog, 587 for TLS) |
| `SMTP_FROM` | `noreply@crm.internal` | RFC 5322 From address for campaign broadcasts |
| `GEMINI_API_KEY` | *(optional)* | Google Gemini API key (defaults to fallback if empty) |

---

## 4. Running the Application

### 4.1 Production Docker Startup (Recommended)
To build and launch the complete production stack (Nginx, React Frontend, Spring Boot API, MySQL 8.4, Redis 7, and MailHog SMTP):

```bash
docker compose up -d --build
```

Verify service status:
```bash
docker compose ps
```

The application is available at:
- **Web Application:** `http://localhost:8080/`
- **Login Screen:** `http://localhost:8080/login`
- **MailHog SMTP Web UI:** `http://localhost:8025/`

### 4.2 Frontend Development Workflow (Vite)
For hot-module replacement during frontend development:
1. Ensure the backend services are running (`docker compose up -d mysql redis smtp app`).
2. Navigate to the frontend directory:
   ```bash
   cd crm-ui
   npm install
   npm run dev
   ```
3. Open `http://localhost:5173/`. Vite automatically proxies `/api` requests to `http://localhost:8081`.

---

## 5. Security & Authentication Architecture

- **Stateless Bearer JWT:** All protected API endpoints require an `Authorization: Bearer <token>` header.
- **In-Memory Token Handling:** The React application manages access tokens strictly in memory within the `AuthContext` React state. Tokens are never stored in `localStorage` or `sessionStorage` to eliminate XSS token theft vectors.
- **Role-Based Access Control (RBAC):**
  - `ROLE_ADMIN`: Full access to user provisioning, role assignments, password resets, customer hard/soft deletion, campaign deletion, and AI compliance audit logs.
  - `ROLE_MARKETER`: Access to customer records, segment AST builder, campaign drafting, launching, and delivery monitoring. Privileged administrative actions return HTTP 403 Forbidden.
- **Theme Preference Persistence:** Only the visual theme preference (`light` or `dark`) is stored in `localStorage` (`ai_crm_theme`).

---

## 6. Local Development Credentials & Password Rotation

### Default Local Admin Account
- **Username:** `ai-crm-admin`
- **Role:** `ROLE_ADMIN`

### Secure Password Rotation Procedure
To rotate the local administrator password:
1. Generate a new secure password (minimum 8 characters, maximum 72 UTF-8 bytes).
2. Generate a cost-12 BCrypt hash using the standard BCrypt utility.
3. Update the password hash in the local MySQL database:
   ```bash
   docker exec -i crm-mysql mysql -uroot -prootpassword -e "UPDATE crm_db.users SET password_hash = '<new_bcrypt_hash>', updated_at = NOW(6) WHERE username = 'ai-crm-admin';"
   ```
4. Authenticate via `POST /api/v1/auth/login` with the updated password to verify.

---

## 7. Automated Test Suites

### Backend Unit & Integration Tests (JUnit 5 + Spring Security Test)
Run the 405 automated backend tests:
```bash
mvn clean test
```
*Results: 405 tests run, 0 failures, 0 errors, 0 skipped.*

### Frontend Tests (Vitest + React Testing Library)
Run the React unit and component tests:
```bash
cd crm-ui
npm test
```
*Results: 20 tests run, 0 failures, 0 errors.*

---

## 8. License & Enterprise Notice

AI-CRM Platform © 2026. All rights reserved. Enterprise edition.
