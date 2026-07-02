# URL Shortener Service

A production-grade URL Shortener microservice built with **Java 17**, **Spring Boot 3.x**, **Cassandra**, and **Redis** — similar in design to Bitly.

---

## Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Language       | Java 17                             |
| Framework      | Spring Boot 3.2.x                   |
| Build Tool     | Gradle                              |
| Primary DB     | Apache Cassandra 4.1                |
| Cache          | Redis 7.2                           |
| Logging        | SLF4J + Logback with MDC tracing    |
| Testing        | JUnit 5 + Mockito                   |
| Containerization | Docker + Docker Compose           |

---

## Project Structure

```
url-shortener-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/urlshortener/
│   │   │   ├── UrlShortenerServiceApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── UrlShortenerController.java   # POST /shorten, GET /stats/{code}
│   │   │   │   └── RedirectController.java       # GET /{code}
│   │   │   ├── service/
│   │   │   │   ├── UrlShortenerService.java      # Interface
│   │   │   │   ├── UrlCacheService.java          # Interface
│   │   │   │   └── impl/
│   │   │   │       ├── UrlShortenerServiceImpl.java
│   │   │   │       └── UrlCacheServiceImpl.java
│   │   │   ├── repository/
│   │   │   │   └── UrlMappingRepository.java
│   │   │   ├── entity/
│   │   │   │   └── UrlMapping.java
│   │   │   ├── dto/
│   │   │   │   ├── ShortenUrlRequest.java
│   │   │   │   ├── ShortenUrlResponse.java
│   │   │   │   ├── UrlStatsResponse.java
│   │   │   │   └── ErrorResponse.java
│   │   │   ├── config/
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── CassandraConfig.java
│   │   │   │   ├── JacksonConfig.java
│   │   │   │   └── CorrelationIdFilter.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── AliasAlreadyExistsException.java
│   │   │   │   └── InvalidUrlException.java
│   │   │   ├── mapper/
│   │   │   │   └── UrlMappingMapper.java
│   │   │   └── util/
│   │   │       ├── ShortCodeGenerator.java
│   │   │       ├── UrlValidator.java
│   │   │       └── CorrelationIdGenerator.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── schema.cql
│   └── test/
│       └── java/com/example/urlshortener/
│           ├── controller/
│           │   ├── UrlShortenerControllerTest.java
│           │   └── RedirectControllerTest.java
│           ├── service/
│           │   ├── UrlShortenerServiceTest.java
│           │   └── UrlCacheServiceTest.java
│           └── util/
│               └── ShortCodeGeneratorTest.java
├── build.gradle
├── settings.gradle
├── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## API Endpoints

### 1. Shorten URL

```
POST /api/v1/url/shorten
Content-Type: application/json
```

**Request Body:**
```json
{
  "originalUrl": "https://www.google.com/very/long/path",
  "customAlias": "google123"
}
```

**Success Response — 201 CREATED:**
```json
{
  "shortCode": "google123",
  "shortUrl": "http://localhost:8080/google123",
  "originalUrl": "https://www.google.com/very/long/path"
}
```

**Error Responses:**
- `400 BAD REQUEST` — Invalid URL format or blank input
- `409 CONFLICT` — Custom alias already exists

---

### 2. Redirect

```
GET /{code}
```

- Checks Redis cache first (cache-aside pattern)
- Falls back to Cassandra on cache miss
- Increments click count on each visit
- Returns `301 FOUND` with `Location` header to original URL
- Returns `404 NOT FOUND` if code doesn't exist

---

### 3. Analytics / Stats

```
GET /api/v1/url/stats/{code}
```

**Success Response — 200 OK:**
```json
{
  "shortCode": "google123",
  "originalUrl": "https://www.google.com/very/long/path",
  "clickCount": 42,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

## Error Response Format

All errors follow a consistent structure:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "UrlMapping not found with shortCode: 'abc123'",
  "path": "/api/v1/url/stats/abc123",
  "correlationId": "a1b2c3d4-e5f6-..."
}
```

---

## Cache-Aside Pattern

```
GET /{code}
     │
     ▼
Check Redis (url:{code})
     │
     ├─── HIT ──► Return original URL immediately
     │
     └─── MISS ──► Query Cassandra
                        │
                        ├─── Found ──► Cache in Redis (TTL: 24h)
                        │              Increment click count
                        │              Return original URL
                        │
                        └─── Not Found ──► 404 NOT FOUND
```

---

## Correlation ID Tracing

Every request is assigned a unique `correlationId` injected into SLF4J MDC
- Otherwise, a UUID is auto-generated.
- The correlation ID appears in all log lines and is returned in the `X-Correlation-Id` response header and in error response bodies.

---

## Database Schema (Cassandra)

```cql
CREATE KEYSPACE IF NOT EXISTS url_shortener
    WITH replication = {
        'class': 'SimpleStrategy',
        'replication_factor': 1
        }
     AND durable_writes = true;

USE url_shortener;

CREATE TABLE IF NOT EXISTS url_mapping (
    short_code   text PRIMARY KEY,
    original_url text,
    custom_alias boolean,
    created_at   timestamp
);

CREATE TABLE IF NOT EXISTS url_clicks (
    short_code text PRIMARY KEY,
    click_count counter
);

```

---

## Running Locally

### Prerequisites
- Docker & Docker Compose
- Java 17 (for local development without Docker)

### Start with Docker Compose

```bash
docker-compose up --build
```

This will start:
- **Cassandra** on port `9042`
- **Redis** on port `6379`
- **Spring Boot App** on port `8080`

### Run Tests

```bash
./gradlew test
```

### Build JAR

```bash
./gradlew bootJar
```

---

## Sample curl Commands

```bash
# Shorten a URL
curl -X POST http://localhost:8080/api/v1/url/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.google.com/search?q=spring+boot"}'

# Shorten with custom alias
curl -X POST http://localhost:8080/api/v1/url/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.google.com", "customAlias": "google123"}'

# Redirect (follow redirect)
curl -L http://localhost:8080/google123

# Get stats
curl http://localhost:8080/api/v1/url/stats/google123

# Health check
curl http://localhost:8080/actuator/health
```

---

## Configuration

All configuration is in `src/main/resources/application.yml`.

| Property                    | Default              | Description                   |
|-----------------------------|----------------------|-------------------------------|
| `app.base-url`              | `http://localhost:8080` | Base URL for shortened links |
| `app.short-code.length`     | `7`                  | Length of generated short codes |
| `app.redis.ttl-hours`       | `24`                 | Redis cache TTL in hours       |
| `spring.cassandra.keyspace-name` | `url_shortener` | Cassandra keyspace            |

---

## Short Code Generation

Short codes are generated using **SHA-256 hashing** of the original URL combined with a fixed salt.
The resulting hash bytes are mapped to a 62-character alphanumeric alphabet to produce a deterministic 7-character code.
**Collision Resolution:** If the generated code already exists,
up to 5 suffix-based retries are attempted before falling back to a cryptographically random code.

---

## Design Principles

- **SOLID** principles throughout
- **Constructor injection** only — no field injection
- **Cache-aside pattern** for Redis
- **Clean architecture** with clear layer separation
- **Resilient caching** — Redis failures never break core functionality
- **Structured error responses** with correlation ID tracking
