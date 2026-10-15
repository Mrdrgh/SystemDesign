# 📋 Distributed Scalable PasteBin System

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-6DB33F.svg?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-4479A1.svg?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-Alpine-DC382D.svg?logo=redis&logoColor=white)](https://redis.io/)
[![MinIO S3](https://img.shields.io/badge/MinIO-S3%20Compatible-C72C48.svg?logo=minio&logoColor=white)](https://min.io/)
[![Bucket4j](https://img.shields.io/badge/Bucket4j-Rate%20Limiter-blue.svg)](https://bucket4j.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker&logoColor=white)](https://www.docker.com/)

A high-performance, enterprise-grade, distributed **PasteBin system design** implementation built with **Spring Boot 4**, **MySQL**, **Redis**, **MinIO (S3-compatible Object Storage)**, and an isolated **Event-Driven Python Worker**.

Engineered around distributed systems best practices, this architecture completely decouples payload transport from the application server using **direct-to-S3 presigned URLs**, eliminates memory bloat, ensures non-blocking upload verifications via an **asynchronous Redis event queue**, and protects all public endpoints with **Bucket4j token-bucket IP rate limiting** and stateless **JWT authentication**.

---

## 📑 Table of Contents

- [System Architecture Overview](#-system-architecture-overview)
- [Spring Security Architecture & Class Diagram](#-spring-security-architecture--class-diagram)
- [Key Architectural Highlights & Design Patterns](#-key-architectural-highlights--design-patterns)
  - [1. Direct-to-S3 Upload (Zero App-Server Ingestion Overhead)](#1-direct-to-s3-upload-zero-app-server-ingestion-overhead)
  - [2. Event-Driven Asynchronous Status Reconciliation](#2-event-driven-asynchronous-status-reconciliation)
  - [3. Granular Access Control via Strategy Pattern](#3-granular-access-control-via-strategy-pattern)
  - [4. Multi-Layer Caching & Read Counter Decoupling](#4-multi-layer-caching--read-counter-decoupling)
  - [5. Token Bucket IP Rate Limiting](#5-token-bucket-ip-rate-limiting)
  - [6. Automated Cron Lifecycle Management](#6-automated-cron-lifecycle-management)
- [Direct-to-S3 Ingestion & Verification Sequence Flow](#-direct-to-s3-ingestion--verification-sequence-flow)
- [Repository Structure](#-repository-structure)
- [Technology Stack](#-technology-stack)
- [Environment Configuration](#-environment-configuration)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Running the Infrastructure](#1-start-backing-services-via-docker-compose)
  - [Running the Spring Boot Application](#2-run-the-spring-boot-application)
- [API Reference & Usage Examples](#-api-reference--usage-examples)
  - [Authentication API](#1-authentication-endpoints)
  - [Pastes API](#2-pastes-endpoints)
- [Error Handling & HTTP Status Codes](#-error-handling--http-status-codes)
- [Architecture Diagrams & Excalidraw Assets](#-architecture-diagrams--excalidraw-assets)

---

## 🏛 System Architecture Overview

The system architecture prevents application server starvation by offloading heavy raw paste uploads and downloads directly to MinIO Object Storage, backed by asynchronous queue reconciliation and low-latency cache reads:

```mermaid
flowchart TB
    subgraph Clients["Clients & Consumers"]
        User["User / HTTP Client"]
    end

    subgraph Edge["Security & Ingress Layer"]
        RateLimit["IpRateLimiterFilter\n(Bucket4j Token Bucket)"]
        JwtFilter["JwtAuthenticationFilter\n(Stateless Bearer JWT)"]
        SecFilter["SecurityFilterChain & AuthorizationFilter"]
    end

    subgraph AppServer["Spring Boot Core Service (:8081)"]
        AuthController["AuthController"]
        PasteController["PasteController"]
        PasteService["PasteService"]
        KeyGen["KeyGenService\n(Cryptographic MD5 / UUID)"]
        AccessSvc["PasteAccessService\n(Strategy Pattern)"]
        S3Svc["S3Service\n(AWS SDK S3Presigner)"]
        Scheduler["@Scheduled Tasks\n(Midnight Flusher & TTL)"]
    end

    subgraph StorageLayer["Data & Object Storage Tier"]
        MySQL[("MySQL 8.0\nMetadata & User Accounts")]
        RedisCache[("Redis Cache\npaste:{url} TTL Cache\nreads:{url} Atomic Counters")]
        MinIO[("MinIO Object Storage\n'pastes/{url}.txt'")]
    end

    subgraph EventTier["Async Event & Worker Layer"]
        BucketNotification["MinIO Event Notification\narn:minio:sqs::PRIMARY:redis\n(event: put -p)"]
        RedisQueue[("Redis Queue List\n'pastebin_events'")]
        Worker["Python Event Worker\n(BLPOP Consumer & Parser)"]
    end

    %% Flows
    User -->|1. HTTP Request| RateLimit
    RateLimit -->|Pass Token Probe| JwtFilter
    JwtFilter -->|Populate SecurityContext| SecFilter
    SecFilter --> AuthController
    SecFilter --> PasteController

    PasteController --> PasteService
    PasteService --> KeyGen
    PasteService --> AccessSvc
    PasteService --> S3Svc

    PasteService -->|Save Metadata: UPLOAD_PENDING| MySQL
    PasteService -->|Read / Write Cached Metadata| RedisCache
    S3Svc -->|Generate Presigned PUT / GET URLs| User

    User -->|2. Direct PUT payload| MinIO
    MinIO -->|3. Trigger Event| BucketNotification
    BucketNotification -->|4. Push Event| RedisQueue
    RedisQueue -->|5. BLPOP Event| Worker
    Worker -->|6. UPDATE paste_status = AVAILABLE| MySQL
    Worker -->|7. Invalidate Stale Cache| RedisCache

    Scheduler -.->|Delete Expired Objects| MinIO
    Scheduler -.->|Purge Expired DB Rows| MySQL
    Scheduler -.->|Evict Expired Keys| RedisCache
```

---

## 🔐 Spring Security Architecture & Class Diagram

The application implements a stateless **Spring Security 6+** filter pipeline. Below is the complete class diagram matching the design in [docs/diagrams/spring-security-architecture.svg](docs/diagrams/spring-security-architecture.svg) and [docs/diagrams/spring-security-architecture.excalidraw](docs/diagrams/spring-security-architecture.excalidraw):

<p align="center">
  <img src="docs/diagrams/spring-security-architecture.svg" alt="Spring Security Architecture Diagram" width="100%" />
</p>

### Interactive Security Class Diagram (Mermaid)

```mermaid
classDiagram
    direction TB

    SecurityConfig ..> SecurityFilterChain : builds
    SecurityFilterChain o-- IpRateLimiterFilter : 1. IP Rate Limiter
    SecurityFilterChain o-- JwtAuthenticationFilter : 2. JWT filter
    SecurityFilterChain o-- UsernamePasswordAuthenticationFilter : 3. Form filter
    SecurityFilterChain o-- AuthorizationFilter : 4. Access gatekeeper

    JwtAuthenticationFilter ..> SecurityContextHolder : populates upon valid JWT
    AuthorizationFilter ..> SecurityContextHolder : reads auth to grant/deny access
    SecurityContextHolder o-- Authentication : stores current

    UsernamePasswordAuthenticationToken ..|> Authentication : implements
    AuthenticationManager <|.. ProviderManager : implements
    ProviderManager o-- AuthenticationProvider : delegates to
    AuthenticationProvider <|.. DaoAuthenticationProvider : implements
    DaoAuthenticationProvider ..> UserDetailsService : loads user via
    DaoAuthenticationProvider ..> PasswordEncoder : verifies hash via
    UserDetails <|.. User : implements
    UserDetailsService ..> UserDetails : returns

    class SecurityConfig {
        +securityFilterChain(HttpSecurity) SecurityFilterChain
    }
    class SecurityFilterChain {
        +List~Filter~ filters
        +matches(HttpServletRequest) boolean
    }
    class IpRateLimiterFilter {
        +doFilterInternal(request, response, chain)
        -extractClientIp(HttpServletRequest) String
    }
    class JwtAuthenticationFilter {
        +doFilterInternal(request, response, chain)
    }
    class UsernamePasswordAuthenticationFilter {
        +doFilter(request, response, chain)
    }
    class AuthorizationFilter {
        +doFilter(request, response, chain)
    }
    class SecurityContextHolder {
        <<static>>
        +getContext() SecurityContext
        +setAuthentication(Authentication)
    }
    class Authentication {
        <<interface>>
        +getPrincipal() Object
        +getCredentials() Object
        +getAuthorities() Collection~GrantedAuthority~
        +isAuthenticated() boolean
    }
    class UsernamePasswordAuthenticationToken {
        +UsernamePasswordAuthenticationToken(principal, credentials, authorities)
    }
    class AuthenticationManager {
        <<interface>>
        +authenticate(Authentication) Authentication
    }
    class ProviderManager {
        +List~AuthenticationProvider~ providers
        +authenticate(Authentication) Authentication
    }
    class AuthenticationProvider {
        <<interface>>
        +authenticate(Authentication) Authentication
        +supports(Class) boolean
    }
    class DaoAuthenticationProvider {
        +authenticate(Authentication) Authentication
    }
    class UserDetailsService {
        <<interface>>
        +loadUserByUsername(String) UserDetails
    }
    class PasswordEncoder {
        <<interface>>
        +encode(CharSequence) String
        +matches(CharSequence, String) boolean
    }
    class UserDetails {
        <<interface>>
        +getAuthorities() Collection~GrantedAuthority~
        +getPassword() String
        +getUsername() String
        +isAccountNonExpired() boolean
        +isAccountNonLocked() boolean
        +isCredentialsNonExpired() boolean
        +isEnabled() boolean
    }
    class User {
        -String id
        -String username
        -String email
        -String password
        -UserRole role
        -List~Paste~ pastes
        +getAuthorities() Collection
    }
```

### Security Pipeline Workflow:
1. **`IpRateLimiterFilter`**: Extracts IP from `X-Forwarded-For` or `RemoteAddr`. Probes Bucket4j bucket. Returns `429 Too Many Requests` with `Retry-After` if exhausted.
2. **`JwtAuthenticationFilter`**: Inspects `Authorization: Bearer <token>`. Decodes claims using `JwtService`. Validates expiration and user identity against `UserDetailsService`.
3. **`SecurityContextHolder`**: If token is valid, creates a `UsernamePasswordAuthenticationToken` and sets it in the thread-local context.
4. **`AuthorizationFilter`**: Validates whether the matched request path permits anonymous access (`/api/auth/**`, `GET /api/pastes/**`) or requires an authenticated principal.
5. **`DaoAuthenticationProvider` & `PasswordEncoder`**: Used during login (`POST /api/auth/login`) to verify credentials against BCrypt salted password hashes.

---

## 🚀 Key Architectural Highlights & Design Patterns

### 1. Direct-to-S3 Upload (Zero App-Server Ingestion Overhead)
Traditional paste systems stream megabytes of payload through the application server heap. This project uses AWS S3 Presigned URLs (`S3Presigner`):
- When a paste is registered, the server reserves a unique short URL and returns a **Presigned PUT S3 URL** valid for a strictly locked duration (`minio.presign-duration=75` minutes).
- **Exact Size Enforcement**: The presigned URL locks `contentLength(lockedSize)` and `contentType("text/plain")`. Any client attempting to upload larger payloads is immediately rejected by S3.
- Application servers stay completely lightweight with negligible CPU and memory consumption.

### 2. Event-Driven Asynchronous Status Reconciliation
- Pastes are initialized in MySQL with `pasteStatus = UPLOAD_PENDING`.
- Once the client uploads the raw bytes to MinIO, MinIO automatically emits a notification to the Redis list `pastebin_events`.
- A lightweight, containerized Python worker consumes events via `BLPOP` without public ingress:
  ```sql
  UPDATE paste 
  SET paste_status = 'AVAILABLE' 
  WHERE url = %s AND paste_status = 'UPLOAD_PENDING';
  ```
- The worker evicts any cached entry (`paste:{url}`) to guarantee strong read consistency across all application instances.

### 3. Granular Access Control via Strategy Pattern
Paste security is decoupled using the **Strategy Design Pattern**:
```mermaid
classDiagram
    PasteAccessService --> PasteAccessStrategy : delegates to
    PasteAccessStrategy <|.. PublicAccessStrategyImpl : PUBLIC
    PasteAccessStrategy <|.. PasswordAccessStrategy : PASSWORD_PROTECTED
    PasteAccessStrategy <|.. RestrictedUsersAcessStrategy : ACCESS_LIST
    PasteAccessStrategy <|.. PrivateAccessStrategy : PRIVATE

    class PasteAccessService {
        -Map~AccessType, PasteAccessStrategy~ strategies
        +checkAccess(Paste, AccessContext)
    }
    class PasteAccessStrategy {
        <<interface>>
        +getSupportedAcessType() AccessType
        +validateAccess(Paste, AccessContext)
    }
    class AccessContext {
        -User currentUser
        -String password
    }
```
- **`PUBLIC`**: Unrestricted worldwide read access.
- **`PASSWORD_PROTECTED`**: Validates supplied plaintext password against BCrypt hash; throws `PasswordRequiredException` (HTTP 401) or `UnauthorizedException` (HTTP 403).
- **`ACCESS_LIST`**: Authorizes only usernames explicitly declared in the paste's `authorizedUsers` set (or the original creator).
- **`PRIVATE`**: Strict creator-only access via `@AuthenticationPrincipal User`.

### 4. Multi-Layer Caching & Read Counter Decoupling
- **Read-Through Cache**: When a paste is requested, `PasteService` checks `paste:{url}` in Redis. On cache hit, metadata deserializes instantly from `PasteCacheDto` without touching MySQL.
- **Dynamic TTL**: Cached entries have their expiration dynamically aligned with the paste's remaining lifespan.
- **Decoupled Analytics**: Read views are incremented via atomic Redis commands (`INCR reads:{url}`), avoiding row-level write contention in the relational database.

### 5. Token Bucket IP Rate Limiting
- Built on **Bucket4j**: Configured with a default capacity of **5 requests per 30-second window** per unique IP.
- Correctly parses the real client IP through reverse proxies by inspecting the leftmost entry in `X-Forwarded-For`.
- Responds with standard `Retry-After: <seconds>` headers on limit breaches.

### 6. Automated Cron Lifecycle Management
Integrated Spring `@Scheduled` tasks run unattended maintenance:
- **Midnight Object Flush (`flushExpiredPastes`)**: Queries expired pastes, issues S3 `DeleteObjectRequest` to MinIO, and purges Redis keys.
- **Pending Paste Reaper (`expireObsoletePastes`)**: Reclaims abandoned paste reservations left in `UPLOAD_PENDING` for more than 5 minutes.

---

## 🔄 Direct-to-S3 Ingestion & Verification Sequence Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant API as Spring Boot API Server
    participant DB as MySQL Database
    participant MinIO as MinIO Object Storage
    participant Redis as Redis (Queue & Cache)
    participant Worker as Python Background Worker

    Client->>API: POST /api/pastes/create (size, title, accessType, expiration)
    Note over API: Generate unique MD5 / Base62 key
    API->>DB: INSERT paste (status: UPLOAD_PENDING)
    API->>API: S3Presigner.presignPutObject (locks size & bucket)
    API-->>Client: 200 OK { url: "a1b2c3d4", s3url: "http://minio/pastes/a1b2c3d4.txt?..." }

    Note over Client: Client directly streams raw payload to Object Store
    Client->>MinIO: PUT s3url (payload text/plain)
    MinIO-->>Client: 200 OK (Upload Successful)

    Note over MinIO: Bucket Event notification emitted
    MinIO->>Redis: LPUSH 'pastebin_events' (S3 notification JSON)
    
    Worker->>Redis: BLPOP 'pastebin_events'
    Redis-->>Worker: event payload
    Note over Worker: Extracts paste URL 'a1b2c3d4'
    Worker->>DB: UPDATE paste SET paste_status = 'AVAILABLE' WHERE url = 'a1b2c3d4'
    Worker->>Redis: DEL paste:a1b2c3d4 (Cache Invalidation)

    Client->>API: GET /api/pastes/a1b2c3d4
    API->>Redis: GET paste:a1b2c3d4
    alt Cache Miss
        API->>DB: SELECT * FROM paste WHERE url = 'a1b2c3d4'
        API->>Redis: SET paste:a1b2c3d4 (TTL: expiration)
    end
    API->>API: PasteAccessService.checkAccess()
    API->>Redis: INCR reads:a1b2c3d4
    API->>API: S3Presigner.presignGetObject (download URL)
    API-->>Client: 200 OK { title, s3url: "http://minio/pastes/a1b2c3d4.txt?..." }
    Client->>MinIO: GET s3url
    MinIO-->>Client: 200 OK (Paste Content)
```

---

## 📂 Repository Structure

```text
PasteBin/
├── .env.example                               # Infrastructure environment template
├── docker-compose.yml                         # Containerized MySQL, Redis, MinIO, mc, & Worker
├── pom.xml                                    # Spring Boot 4.1.1 + AWS SDK + Bucket4j + JJWT
├── mvnw / mvnw.cmd                            # Maven Wrapper
├── docs/
│   └── diagrams/
│       ├── spring-security-architecture.svg   # High-resolution SVG Security Architecture
│       └── spring-security-architecture.excalidraw # Native editable Excalidraw JSON scene
├── worker/
│   ├── Dockerfile                             # Minimal Python 3.10 slim container
│   ├── requirements.txt                       # redis, pymysql dependencies
│   └── worker.py                              # Redis event listener & DB status updater
└── src/
    ├── main/
    │   ├── java/com/example/pastebin/
    │   │   ├── Controller/
    │   │   │   ├── AuthController.java        # Authentication endpoints (register, login, me)
    │   │   │   └── PasteController.java       # Paste CRUD & retrieval endpoints
    │   │   ├── Dto/                           # Request & Response payload definitions
    │   │   ├── Entity/
    │   │   │   ├── Paste.java                 # JPA Entity (title, url, status, accessType)
    │   │   │   └── User.java                  # JPA Entity + Spring Security UserDetails
    │   │   ├── Enum/                          # AccessType, PasteStatus, UserRole
    │   │   ├── Exception/                     # Exception definitions & @RestControllerAdvice
    │   │   ├── repository/                    # Spring Data JPA repositories
    │   │   ├── security/
    │   │   │   ├── ApplicationConfig.java     # DaoAuthProvider, UserDetailsService, BCrypt
    │   │   │   ├── IpRateLimiterFilter.java   # Bucket4j IP Rate Limiting filter
    │   │   │   ├── JwtAuthenticationFilter.java # Stateless Bearer JWT extraction filter
    │   │   │   └── SecurityConfig.java        # SecurityFilterChain pipeline definition
    │   │   ├── Service/
    │   │   │   ├── impl/                      # Paste Access Strategy implementations
    │   │   │   ├── AuthenticationService.java # User registration & JWT badge issuance
    │   │   │   ├── JwtService.java            # HMAC-SHA token generator & claim verifier
    │   │   │   ├── KeyGenService.java         # Secure Random MD5 short-hash generator
    │   │   │   ├── PasteAccessService.java    # Strategy dispatcher
    │   │   │   ├── PasteService.java          # Core paste workflow, cache, and scheduler
    │   │   │   ├── RateLimitingService.java   # Token bucket manager
    │   │   │   └── S3Service.java             # MinIO / AWS S3 presigner & cron flusher
    │   │   └── PasteBinApplication.java       # Spring Boot main entrypoint (@EnableScheduling)
    │   └── resources/
    │       └── application.properties         # Server port, datasource, MinIO & JWT configs
    └── test/
```

---

## 🛠 Technology Stack

| Layer | Component | Description |
|---|---|---|
| **Language & Framework** | Java 17, Spring Boot 4.1.1 | Modern Spring Boot enterprise foundation |
| **Security & Auth** | Spring Security 6, JJWT 0.12.6, BCrypt | Stateless JWT Bearer tokens + salted password hashing |
| **Rate Limiting** | Bucket4j JDK 17 (8.21.0) | Token-bucket rate limiter with `X-Forwarded-For` support |
| **Relational Database** | MySQL 8.0, Spring Data JPA / Hibernate | Persistent storage for users, metadata, and access lists |
| **In-Memory Cache & Queue**| Redis (Alpine), Spring Data Redis | Sub-millisecond metadata caching & BLPOP event bus |
| **Object Storage** | MinIO (AWS S3 SDK 2.55.12) | High-throughput object storage for raw paste files |
| **Event Worker** | Python 3.10, PyMySQL, redis-py | Zero-ingress async consumer updating paste statuses |
| **Infrastructure** | Docker & Docker Compose | Containerized orchestration for all backing services |

---

## ⚙ Environment Configuration

Copy `.env.example` to `.env` before running Docker Compose:

```bash
cp .env.example .env
```

| Variable | Default Value | Description |
|---|---|---|
| `MYSQL_HOST` | `mysql` (Docker) / `localhost` | MySQL hostname |
| `MYSQL_PORT` | `3306` | MySQL port |
| `MYSQL_ROOT_PASSWORD` | `root` | Root password for MySQL |
| `MYSQL_DATABASE` | `pastebin` | Application database schema name |
| `REDIS_HOST` | `redis` (Docker) / `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_EVENTS_QUEUE` | `pastebin_events` | Redis list key for MinIO S3 notifications |
| `MINIO_HOST` | `minio` (Docker) / `localhost` | MinIO API host |
| `MINIO_PORT` | `9000` | MinIO S3 API port |
| `MINIO_CONSOLE_PORT` | `9001` | MinIO Web UI Console port |
| `MINIO_ROOT_USER` | `minioadmin` | MinIO root access key |
| `MINIO_ROOT_PASSWORD` | `minioadmin` | MinIO root secret key |
| `MINIO_BUCKET_NAME` | `pastebin` | Target S3 bucket for paste storage |

---

## 🚦 Getting Started

### Prerequisites
- [Docker Engine & Docker Compose](https://docs.docker.com/get-docker/)
- [Java Development Kit (JDK) 17+](https://adoptium.net/)

### 1. Start Backing Services via Docker Compose

Launch MySQL, Redis, MinIO, the auto-bucket initializer, and the background worker:

```bash
docker compose up -d --build
```

Verify all containers are healthy:
```bash
docker compose ps
```

*Expected output:*
- `pastebin-mysql`: Up on port `3306`
- `pastebin-redis`: Up on port `6379`
- `pastebin-minio`: Up on ports `9000` (API) and `9001` (Console)
- `pastebin-worker`: Up (Listening for Redis events)

### 2. Run the Spring Boot Application

Using the included Maven wrapper:

**On Linux / macOS:**
```bash
./mvnw clean spring-boot:run
```

**On Windows:**
```powershell
.\mvnw.cmd clean spring-boot:run
```

The application starts on `http://localhost:8081`.

---

## 📡 API Reference & Usage Examples

### 1. Authentication Endpoints

#### Register a New User
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alex",
    "email": "alex@example.com",
    "password": "Password123!"
  }'
```
*Response (200 OK):*
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alex",
    "password": "Password123!"
  }'
```

#### Check Current Identity
```bash
curl -X POST http://localhost:8081/api/auth/me \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

---

### 2. Pastes Endpoints

#### Create a Paste (`POST /api/pastes/create`)
Supported `accessType` values: `PUBLIC`, `PASSWORD_PROTECTED`, `ACCESS_LIST`, `PRIVATE`.

**Public Paste Example:**
```bash
curl -X POST http://localhost:8081/api/pastes/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "title": "System Architecture Notes",
    "expirationInMinutes": 60,
    "accessType": "PUBLIC",
    "sizeInBytes": 27
  }'
```

*Response (200 OK):*
```json
{
  "url": "a1b2c3d4",
  "s3url": "http://localhost:9000/pastebin/pastes/a1b2c3d4.txt?X-Amz-Algorithm=..."
}
```

#### Upload the Paste Content Directly to S3 (MinIO)
Use the `s3url` received above:
```bash
curl -X PUT "http://localhost:9000/pastebin/pastes/a1b2c3d4.txt?X-Amz-Algorithm=..." \
  -H "Content-Type: text/plain" \
  -d "Hello World from PasteBin!"
```
> 💡 *Once uploaded, MinIO fires an event notification to Redis. The Python worker automatically reconciles the status to `AVAILABLE` in MySQL.*

#### Read a Paste (`GET /api/pastes/{url}`)
```bash
curl -X GET http://localhost:8081/api/pastes/a1b2c3d4
```

*Response (200 OK):*
```json
{
  "id": 1,
  "title": "System Architecture Notes",
  "url": "a1b2c3d4",
  "creator": "alex",
  "createdAt": "2026-10-15T11:20:00",
  "expiration": "PT1H",
  "s3url": "http://localhost:9000/pastebin/pastes/a1b2c3d4.txt?X-Amz-Algorithm=..."
}
```

#### Fetch Raw Content from Presigned S3 Download URL:
```bash
curl "http://localhost:9000/pastebin/pastes/a1b2c3d4.txt?X-Amz-Algorithm=..."
```
*Output:*
```text
Hello World from PasteBin!
```

#### Password-Protected Paste Example
1. **Create:**
   ```bash
   curl -X POST http://localhost:8081/api/pastes/create \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
     -d '{
       "title": "Secret Config",
       "expirationInMinutes": 120,
       "accessType": "PASSWORD_PROTECTED",
       "password": "SuperSecretPassword!",
       "sizeInBytes": 15
     }'
   ```
2. **Access with password query parameter:**
   ```bash
   curl -X GET "http://localhost:8081/api/pastes/{url}?password=SuperSecretPassword!"
   ```

---

## 🛡 Error Handling & HTTP Status Codes

The service features centralized error handling via `@RestControllerAdvice` (`ControllerExceptionsHandler`):

| HTTP Status | Exception Class | Reason |
|---|---|---|
| **401 Unauthorized** | `PasswordRequiredException` | A password-protected paste was requested without a password |
| **403 Forbidden** | `UnauthorizedException` / `AuthenticationException` | Invalid password, restricted access list denial, or duplicate registration |
| **404 Not Found** | `PasteNotAvailableException` | Paste URL not found, expired, or still in `UPLOAD_PENDING` |
| **413 Payload Too Large** | `MaximumFileSizeException` | Requested paste size exceeds maximum configured threshold (`1 MB`) |
| **429 Too Many Requests** | `RateLimitExceededException` | Client exceeded the Bucket4j token limit (includes `Retry-After` header) |
| **500 Internal Error** | `Exception` | Unhandled runtime error |

---

## 🎨 Architecture Diagrams & Excalidraw Assets

<p align="center">
  <img src="docs/diagrams/spring-security-architecture.svg" alt="Spring Security Architecture Preview" width="100%" />
</p>

The architectural diagrams are available in both vector format and native Excalidraw format:

- **SVG Architecture Diagram:** [docs/diagrams/spring-security-architecture.svg](docs/diagrams/spring-security-architecture.svg)
- **Editable Excalidraw Scene File:** [docs/diagrams/spring-security-architecture.excalidraw](docs/diagrams/spring-security-architecture.excalidraw)

To edit or customize the Excalidraw diagram:
1. Navigate to [excalidraw.com](https://excalidraw.com).
2. Drag and drop `docs/diagrams/spring-security-architecture.excalidraw` directly into your browser canvas.

---

## 📄 License

This project is licensed under the MIT License - feel free to use and modify for learning, interviews, or production deployment.
