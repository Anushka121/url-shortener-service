# URL Shortener Service

A production-grade URL Shortener microservice built with **Java 17**, **Spring Boot 3.x**, **Apache Cassandra**, and **Redis**, inspired by systems such as Bitly.

---

## Tech Stack

| Layer            | Technology                                 |
| ---------------- | ------------------------------------------ |
| Language         | Java 17                                    |
| Framework        | Spring Boot 3.x                            |
| Build Tool       | Gradle                                     |
| Primary Database | Apache Cassandra 4.x                       |
| Cache            | Redis                                      |
| Logging          | SLF4J + Logback + MDC tracing              |
| Testing          | JUnit 5 + Mockito                          |
| Architecture     | Layered Architecture + Cache Aside Pattern |

---

## Features

* Generate shortened URLs from long URLs
* Support custom aliases for shortened URLs
* Redirect using short code
* Redis cache-aside strategy for fast redirects
* Analytics endpoint for click statistics
* Click count tracking
* Global exception handling with structured error responses
* Correlation ID tracing across requests
* SHA-256 based deterministic short code generation
* Collision handling with retry fallback strategy
* Resilient caching (cache failures do not break application flow)

---

## Installation & Setup

### Prerequisites

Make sure the following are installed:

* Java 17+
* Gradle 8+
* Local Apache Cassandra instance
* Local Redis instance

Verify installation:

```bash
java -version
gradle -v
```

---

## Clone Repository

```bash
git clone <your-github-repository-url>
cd url-shortener-service
```

---

## Running the Project

Run locally:

```bash
./gradlew bootRun
```

Build JAR:

```bash
./gradlew bootJar
```

Run generated JAR:

```bash
java -jar build/libs/url-shortener-service.jar
```

---

## Running Tests

Execute all tests:

```bash
./gradlew test
```

Current test coverage includes:

* Controller tests using MockMvc
* Service layer tests using Mockito
* Utility layer tests
* Exception handling validation

---

## Verify Application

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP",
  "components": {
    "cassandra": {
      "status": "UP",
      "details": {
        ... other database details
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        ... system resource details
      }
    },
    "ping": {
      "status": "UP"
    },
    "redis": {
      "status": "UP",
      "details": {
        ... cache server details
      }
    }
  }
}
```

---

## Project Structure

```text
url-shortener-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/urlshortener/
│   │   │   ├── controller/
│   │   │   │   ├── UrlShortenerController.java
│   │   │   │   └── RedirectController.java
│   │   │   ├── service/
│   │   │   │   ├── UrlShortenerService.java
│   │   │   │   ├── UrlCacheService.java
│   │   │   │   └── impl/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   ├── config/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   └── util/
│   └── test/
│       ├── controller/
│       ├── service/
│       └── util/
├── build.gradle
├── settings.gradle
└── README.md
```

---

## API Endpoints

## 1. Shorten URL

```http
POST /api/v1/url/shorten
```

Request:

```json
{
  "originalUrl": "https://www.google.com/very/long/path",
  "customAlias": "google123"
}
```

Success Response:

```json
{
  "shortCode": "google123",
  "shortUrl": "http://localhost:8080/google123",
  "originalUrl": "https://www.google.com/very/long/path"
}
```

Possible Errors:

* 400 BAD REQUEST → Invalid URL format
* 409 CONFLICT → Alias already exists

---

## 2. Redirect

```http
GET /{code}
```

Flow:

* Check Redis cache first
* Fall back to Cassandra on cache miss
* Increment click count
* Return redirect response

Responses:

* 301 MOVED PERMANENTLY
* 404 NOT FOUND

---

## 3. Analytics

```http
GET /api/v1/url/stats/{code}
```

Response:

```json
{
  "shortCode": "google123",
  "originalUrl": "https://www.google.com/very/long/path",
  "clickCount": 42,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

## Cache Aside Architecture

```text
GET /{code}
      │
      ▼
Check Redis Cache
      │
      ├── HIT → Return original URL
      │
      └── MISS → Query Cassandra
                      │
                      ├── Found → Cache result
                      │           Increment clicks
                      │           Return URL
                      │
                      └── Not Found → Return 404
```

---

## Correlation ID Tracing

Each request is assigned a unique correlation ID.

The correlation ID:

* Is stored in MDC
* Appears in all log entries
* Returned in response header
* Included in error responses

Used for request tracing and debugging.

---

## Error Response Format

All errors follow a consistent structure.

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "UrlMapping not found with shortCode: abc123",
  "path": "/api/v1/url/stats/abc123",
  "correlationId": "generated-request-id"
}
```

---

## Database Schema

```sql
CREATE KEYSPACE IF NOT EXISTS url_shortener
WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': 1
};

USE url_shortener;

CREATE TABLE IF NOT EXISTS url_mapping (
    short_code text PRIMARY KEY,
    original_url text,
    custom_alias boolean,
    created_at timestamp
);

CREATE TABLE IF NOT EXISTS url_clicks (
    short_code text PRIMARY KEY,
    click_count counter
);
```

---

## Sample cURL Commands

Create short URL:

```bash
curl -X POST http://localhost:8080/api/v1/url/shorten \
-H "Content-Type: application/json" \
-d '{"originalUrl":"https://google.com"}'
```

Redirect:

```bash
curl -L http://localhost:8080/google123
```

Analytics:

```bash
curl http://localhost:8080/api/v1/url/stats/google123
```

---

## Short Code Generation Strategy

Short code generation uses deterministic hashing.

Process:

1. SHA-256 hash original URL
2. Convert bytes into Base62 character set
3. Generate fixed-length 7 character short code

Collision Handling:

* Retry with suffix variations
* Maximum 5 retries
* Fall back to secure random generation if collision persists

---

## Testing Strategy

Covered test layers:

### Controller Tests

* Request validation
* HTTP status code validation
* Exception handling
* Redirect behavior

### Service Tests

* URL shortening logic
* Alias conflict handling
* Cache lookup behavior
* Cache fallback logic

### Utility Tests

* Short code generation
* URL validation logic

Repository tests were intentionally skipped for standard CRUD repositories because they rely on Spring Data generated implementations.

Testing priority was given to business logic layers.

---

## Design Principles

Architecture decisions followed:

* SOLID principles
* Constructor injection only
* Layered architecture
* Cache aside pattern
* Separation of concerns
* Resilient caching strategy
* Structured exception handling
* Clean service abstraction

---

## Future Improvements

Planned production-grade enhancements:

* Containerization using Docker and Docker Compose
* Integration testing using Testcontainers
* API rate limiting
* Authentication and authorization
* Custom expiration time for short URLs
* Kafka based click event processing
* Distributed cache clustering
* Analytics dashboard
* Multi-region deployment

---

## Learnings From This Project

Concepts implemented and practiced:

* Microservice design principles
* Distributed caching using Redis
* Working with Apache Cassandra counters
* Layered backend architecture
* Exception handling patterns
* Request tracing using MDC
* Unit testing with Mockito and JUnit
* Designing scalable URL shortening systems
* Cache aside design pattern
* Building production-oriented backend services

```
```
