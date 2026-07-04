## What did you ask the AI to do, and what did you write or decide for yourself?

I first defined the functional and non-functional requirements for a URL shortener service and used AI to generate an initial system design and implementation approach.

From there, I iteratively refined the design myself by:
- Improving the architecture using system design principles.
- Designing the service layer and overall project structure.
- Implementing Redis-based caching and rate limiting.
- Choosing Cassandra as the persistent datastore.
- Refining the collision handling strategy.
- Improving maintainability through cleaner separation of concerns, configuration management, and reusable constants.
- Writing and refining the production code, tests, and documentation.

AI was primarily used as a brainstorming and review tool, while the implementation decisions, architecture refinements, and final code were made through iterative development.

---

## Where did you override, correct, or throw away the AI’s output — and why?

I have overriden several of the AI's suggestions to better align the implementation with production-ready design principles.

Some of the key changes included:
- Introducing centralized API route constants instead of hardcoded endpoints.
- Improving separation of concerns between controllers, services, repositories, and configuration classes.
- Replacing a basic random alphanumeric short-code generation strategy with a Redis `INCR` + Feistel permutation + Base62 encoding approach to eliminate collisions by design.
- Refining exception handling to provide cleaner API responses and improve maintainability.
- Reworking portions of the README and test structure to better document system behavior, rate limiting, and design decisions.

These changes resulted in a cleaner, more scalable, and easier-to-maintain implementation than the initial AI-generated output.

---

## The two or three biggest trade-offs you made, and the alternatives you considered

While designing the URL shortener system, I made several trade-offs to balance scalability, simplicity, and operational reliability.

### Cassandra over SQL

I chose Cassandra because the service needs horizontal scalability, high availability, and extremely fast key-based lookups. Although the workload is read-heavy, Cassandra scales more naturally across multiple nodes than a traditional relational database, which can become a bottleneck as traffic grows.

### Redis over an in-memory cache

Instead of using an in-memory cache, I chose Redis to optimize redirect performance. Redis provides a distributed cache that works across multiple application instances, supports persistence, and survives application restarts, making it much more suitable for a scalable deployment.

### Collision handling strategy

Rather than generating random hashes (for example using SHA-256) and checking for collisions, I used a Redis `INCR` counter combined with a Feistel permutation and Base62 encoding. This produces unique short codes deterministically, eliminating collision detection and retry logic. A random-code fallback is retained only for the unlikely scenario where Redis is unavailable.

---

## What’s missing, or what would you do with another day?

While the current implementation is production-oriented, several improvements remain:

- Add dedicated unit and integration tests for the rate limiting functionality to cover throttling and edge cases.
- Containerize the application using Docker and Docker Compose for easier local development and deployment.
- Add integration testing with Testcontainers to validate interactions with Redis and Cassandra instead of relying primarily on unit tests.
- Move click analytics to an asynchronous event-driven pipeline (for example using Kafka) so redirect requests remain lightweight while analytics processing scales independently.
- Support configurable URL expiration instead of the current fixed 24-hour TTL.
- Add authentication and authorization as optional product-level features.

With another day, I would primarily focus on expanding test coverage, improving deployment readiness, and introducing additional scalability enhancements for production use.