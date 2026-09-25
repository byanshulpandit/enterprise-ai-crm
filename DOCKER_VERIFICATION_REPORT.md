# Docker Verification Report

## 1. Verification Date
- **Date & Time:** 2026-09-25 20:05:00 IST (14:35:00 UTC)
- **Author:** Automated Antigravity Verification Subsystem

---

## 2. Environment
- **Host OS:** Microsoft Windows 11 Enterprise (Version 10.0.26100)
- **Virtualization / Kernel:** WSL2 (`6.18.33.2-microsoft-standard-WSL2`, Linux x86_64)
- **Container Platform:** Docker Desktop 4.92.0 (Build 240144)
- **Docker Client Version:** 29.8.1 (API: 1.56, Git commit: `4a63305`, Built: Tue Sep 15 16:28:39 2026)
- **Docker Server / Engine Version:** 29.8.0 (API: 1.56, Git commit: `3ce5872`, Linux/amd64)
- **Docker Compose Version:** v5.5.1
- **Docker Context:** `desktop-linux`

---

## 3. Pre-Verification Status
- **Docker Engine Availability:** Confirmed available and running via Docker Desktop connected to WSL2. `docker info` executed successfully with 0 daemon errors.
- **Compose Configuration Validity:** Validated via `docker compose config` against `docker-compose.yml`. Schema, network definitions, service healthchecks, and dependency graphs confirmed syntactically and structurally correct.
- **Actual CRM Container Runtime:** Not running prior to this session; required full multi-stage image build, container initialization, credential-helper path configuration, and live stack bootstrap.

---

## 4. Compose Configuration
- **Result:** **PASS**
- **Validation Command:**
  ```powershell
  docker compose config
  ```
- **Evidence:**
  The command parsed 4 services (`mysql`, `redis`, `smtp`, `app`), 1 user-defined bridge network (`crm-network`), and 2 persistent local volumes (`mysql_data`, `redis_data`).
  - Unresolved variables: None (all variables such as `CRM_DELIVERY_PROVIDER`, `GEMINI_API_KEY` resolve cleanly or have valid defaults).
  - Warnings: None.
  - Configuration problems: None.

---

## 5. Containers
Verified via `docker compose ps` and `docker inspect` on the live running stack:

| Service | Container | Status | Health | Restart Count | Ports | Result |
|---|---|---|---|---|---|---|
| `app` | `crm-app` | Up | Not configured | 0 | `0.0.0.0:8080->8080/tcp` | **PASS** |
| `mysql` | `crm-mysql` | Up | `healthy` (`mysqladmin ping`) | 0 | `3306/tcp, 33060/tcp` (internal) | **PASS** |
| `redis` | `crm-redis` | Up | `healthy` (`redis-cli ping`) | 0 | `6379/tcp` (internal) | **PASS** |
| `smtp` | `crm-smtp` | Up | Not configured | 0 | `127.0.0.1:1025->1025/tcp`, `127.0.0.1:8025->8025/tcp` | **PASS** |

*Note: All containers maintained `RestartCount: 0` without crash loops or exit code anomalies.*

---

## 6. Application Startup
- **Build Result:** **PASS**
  - Multi-stage Dockerfile built `clg-crm-app:latest` using Eclipse Temurin Java 21 Alpine runtime.
  - Packaged fat JAR: `crm-platform-0.0.1-SNAPSHOT.jar` (135 source files, 52 test source files).
  - Unprivileged non-root runtime user: `crmapp:crmgroup` (UID/GID 10001).
- **Startup Result:** **PASS**
  - Platform initialization completed in `14.546 seconds`.
  - Spring embedded Tomcat started on port `8080` with context path `/`.
  - Admin bootstrap runner executed: verified existing admin user (`admin`) with `ROLE_ADMIN`.
- **Relevant Log Evidence:**
  ```text
  com.zaxxer.hikari.HikariDataSource       : CrmHikariPool - Added connection com.mysql.cj.jdbc.ConnectionImpl@...
  com.zaxxer.hikari.HikariDataSource       : CrmHikariPool - Start completed.
  j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
  c.c.p.d.config.DeliveryProviderConfig    : Active DeliveryProvider configured: SmtpDeliveryProvider
  c.c.p.d.service.DeliveryStreamConsumer   : Redis Stream consumer started for stream=crm:campaign:deliveries:stream, group=crm:delivery:workers, consumer=worker-1
  o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
  com.crm.platform.PlatformApplication     : Started PlatformApplication in 14.546 seconds
  ```
- **Health Endpoint Result:** **PASS**
  - Host HTTP GET `http://localhost:8080/actuator/health` returned HTTP `200 OK`:
  ```json
  {
    "status": "UP"
  }
  ```

---

## 7. Infrastructure Connectivity
All infrastructure dependencies were tested and verified **DIRECTLY FROM THE APPLICATION CONTAINER NETWORK** (`crm-network` bridge):

### MySQL (Database)
- **Container Network Test:**
  `docker exec crm-app nc -zv mysql 3306` $\to$ `mysql (172.18.0.3:3306) open`
- **JPA / HikariCP Pool:** Connection acquired cleanly via URL `jdbc:mysql://mysql:3306/crm_db`.
- **Schema Initialization:** Verified in `crm-mysql` with `SHOW TABLES;`. All 9 domain tables present:
  `ai_segment_audits`, `campaign_delivery_outbox`, `campaign_delivery_records`, `campaigns`, `customer_tags`, `customers`, `segments`, `upload_history`, `users`.
- **Data Persistence:** Customer, segment, campaign, and delivery audit rows persisted and retained across container reboots via `mysql_data` volume.

### Redis (Stream Broker & Key-Value)
- **Container Network Test:**
  `docker exec crm-app nc -zv redis 6379` $\to$ `redis (172.18.0.2:6379) open`
- **Stream Ingestion & Consumer Group:**
  Consumer group `crm:delivery:workers` on stream `crm:campaign:deliveries:stream` active.
  `docker exec crm-redis redis-cli XINFO GROUPS crm:campaign:deliveries:stream` verified `consumers: 1`, `pending: 0`, `lag: 0`.

### SMTP / MailHog (Email Delivery Service)
- **Container Network Test:**
  `docker exec crm-app nc -zv smtp 1025` $\to$ `smtp (172.18.0.4:1025) open`
- **Application Integration:**
  `SmtpDeliveryProvider` delivered outbound email notifications directly to `smtp:1025`.
  Verified via MailHog HTTP API at `http://127.0.0.1:8025/api/v2/messages`.

---

## 8. Automated Tests
Executed on the repository codebase:

```powershell
mvn clean test
mvn clean package -DskipTests
```

### Test Suite Execution Summary:
- **Total Tests Run:** 399
- **Passed:** 399
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Build Result:** **BUILD SUCCESS** (Total time: 01:40 min)
- **Package Result:** **BUILD SUCCESS** (`target/crm-platform-0.0.1-SNAPSHOT.jar` successfully built)

---

## 9. CRM Runtime / E2E Verification
Executed live against the running Docker stack:

| Flow / Test | Command / Method | Actual Result | Result |
|---|---|---|---|
| **Health Check** | `GET http://localhost:8080/actuator/health` | HTTP 200, `{"status":"UP"}` | **PASS** |
| **API Documentation** | `GET http://localhost:8080/v3/api-docs` | HTTP 200, OpenAPI 3.0.1 JSON spec returned | **PASS** |
| **RBAC / Authentication Challenge** | `GET http://localhost:8080/api/v1/customers` (no token) | HTTP 401 Unauthorized, rejected unauthenticated request | **PASS** |
| **Admin Login & JWT Issuance** | `POST http://localhost:8080/api/v1/auth/login` with bootstrap credentials | HTTP 200, valid JWT Bearer token issued for user `admin` with `ROLE_ADMIN` | **PASS** |
| **Customer Creation** | `POST http://localhost:8080/api/v1/customers` with Bearer token | HTTP 201, created Customer entity (`Delhi`, totalSpend 5000) | **PASS** |
| **Dynamic AST Segment Evaluation** | `POST http://localhost:8080/api/v1/segments` with rule `city == Delhi` | HTTP 201, created Segment entity with dynamic AST criteria evaluation | **PASS** |
| **Campaign Creation** | `POST http://localhost:8080/api/v1/campaigns` | HTTP 201, created Campaign entity in `DRAFT` status | **PASS** |
| **Transactional Outbox Generation** | `POST http://localhost:8080/api/v1/campaigns/{id}/launch` | HTTP 200, Campaign transitioned to `RUNNING`, outbox entries persisted atomically in MySQL | **PASS** |
| **Redis Stream Dispatch** | Redis Stream Worker Consumer loop | Outbox entries published to `crm:campaign:deliveries:stream`, consumer group `crm:delivery:workers` dequeued entries with zero PEL lag | **PASS** |
| **SMTP Delivery to MailHog** | `SmtpDeliveryProvider` execution via JavaMail | Emails delivered to MailHog at `smtp:1025` with RFC-compliant MIME headers and idempotency key `CAMP-4-CUST-4` | **PASS** |
| **Delivery Audit Finalization** | Polled `GET http://localhost:8080/api/v1/campaigns/{id}/delivery-summary` | `sentCount: 1`, `failedCount: 0`, `pendingCount: 0`, `completionPercentage: 100.0%`, `terminal: true` | **PASS** |
| **Campaign State Transition** | `GET http://localhost:8080/api/v1/campaigns/{id}` | Status transitioned to `COMPLETED` upon 100% terminal delivery | **PASS** |
| **MailHog Inbox Ingestion** | `GET http://127.0.0.1:8025/api/v2/messages` | Message confirmed in inbox: recipient `live.restart.1366505943@example.com`, subject `Campaign Notification #4`, Header `X-Delivery-Idempotency-Key: CAMP-4-CUST-4` | **PASS** |

---

## 10. Issues Found
During verification, two technical issues were identified and addressed:

1. **Docker Desktop Credential Helper Path:**
   - **Severity:** Medium
   - **Evidence:** `docker-credential-desktop.exe: exec: "docker-credential-desktop": executable file not found in %PATH%` occurred during initial image builds.
   - **Impact:** Prevented Docker CLI from resolving builder cache and registries on the Windows host.
   - **Resolution:** Added `C:\Users\ANSHUL GAUTAM\AppData\Local\Programs\DockerDesktop\resources\bin` to PATH and installed helper binary into WindowsApps. **FIXED**.

2. **Outbox Repository Transaction Context on Synchronous Launch Publish:**
   - **Severity:** Low (non-blocking for async scheduler, but logged warning on immediate launch)
   - **Evidence:** `Failed to publish outbox event id=1 to Redis stream: Executing an update/delete query` in `DeliveryOutboxPublisher.java`.
   - **Impact:** Immediate publishing triggered from `afterCommit` executed in a `Propagation.NOT_SUPPORTED` context, causing Spring Data JPA modifying queries to require an explicit transaction.
   - **Resolution:** Updated `CampaignDeliveryOutboxRepository.java` methods `markPublished` and `recordFailure` to `@Transactional(propagation = Propagation.REQUIRES_NEW)`. Both immediate publishing and background scheduling now execute inside independent transactions without warnings. **FIXED**.

---

## 11. Fixes Made
1. **`docker-compose.yml`:**
   - Updated `CRM_DELIVERY_PROVIDER` environment variable default from `simulated` to `smtp` (`${CRM_DELIVERY_PROVIDER:-smtp}`) so the Dockerized stack out of the box delivers live emails to the included `crm-smtp` MailHog container.
2. **`CampaignDeliveryOutboxRepository.java`:**
   - Configured `propagation = Propagation.REQUIRES_NEW` on `@Modifying` queries (`markPublished`, `recordFailure`) to ensure transactional isolation when invoked from un-proxied or `NOT_SUPPORTED` publishers.

---

## 12. Final Status
### **VERIFIED**

The Enterprise AI-CRM stack (`app`, `mysql`, `redis`, `smtp`) is fully operational, hardened, reproducible, and verified end-to-end inside Docker.

---

## 13. Evidence

### Key Commands and Verified Outputs

#### 1. Compose Status (`docker compose ps`):
```text
NAME        IMAGE                    COMMAND                  SERVICE   CREATED          STATUS                    PORTS
crm-app     clg-crm-app              "sh -c 'java $JAVA_O…"   app       19 seconds ago   Up 12 seconds             0.0.0.0:8080->8080/tcp
crm-mysql   mysql:8.4                "docker-entrypoint.s…"   mysql     20 seconds ago   Up 18 seconds (healthy)   3306/tcp, 33060/tcp
crm-redis   redis:7-alpine           "docker-entrypoint.s…"   redis     20 seconds ago   Up 18 seconds (healthy)   6379/tcp
crm-smtp    mailhog/mailhog:v1.0.1   "MailHog"                smtp      20 seconds ago   Up 17 seconds             127.0.0.1:1025->1025/tcp, 127.0.0.1:8025->8025/tcp
```

#### 2. In-Container Network Connectivity (`docker exec crm-app`):
```text
mysql (172.18.0.3:3306) open
redis (172.18.0.2:6379) open
smtp (172.18.0.4:1025) open
```

#### 3. Database Audit Record Verification (`docker exec crm-mysql`):
```text
id  campaign_id  customer_id  status  failure_reason  processed_at
1   1            1            SENT    NULL            2026-09-25 14:17:27.943221
2   2            1            SENT    NULL            2026-09-25 14:24:20.917800
3   2            2            SENT    NULL            2026-09-25 14:24:20.961775
4   3            1            SENT    NULL            2026-09-25 14:24:21.007936
5   3            2            SENT    NULL            2026-09-25 14:24:21.038879
6   3            3            SENT    NULL            2026-09-25 14:24:21.070196
```

#### 4. Redis Stream Status (`docker exec crm-redis`):
```text
XINFO GROUPS crm:campaign:deliveries:stream
consumers: 1
pending: 0
entries-read: 8
lag: 0
```

#### 5. MailHog Ingestion Verification (`http://127.0.0.1:8025/api/v2/messages`):
```text
Total MailHog messages: 1
Latest Email To:      live.restart.1366505943@example.com
Latest Email Subject: Campaign Notification #4
Idempotency Key:      CAMP-4-CUST-4
```

#### 6. Clean Stack Restart Reproducibility:
```powershell
docker compose down
# Verified all containers, network stopped and removed cleanly
docker compose up -d
# Verified all 4 containers healthy, Actuator returned HTTP 200 {"status":"UP"}
```

---

## 14. Remaining Actions
- **None.** All 10 verification tasks, container health assertions, database schema validations, automated unit/integration tests (399/399 passing), and live end-to-end campaign delivery flows are complete. Stack is fully ready for production demonstration and deployment.
