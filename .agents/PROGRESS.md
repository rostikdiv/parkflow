# Progress: ParkFlow

## Implemented (M1 - M9b)
- ✅ **M1 (Project Skeleton & Database)**: Generated Spring Boot application. Configured Flyway, Testcontainers (PostgreSQL, Redis, RabbitMQ), CI/CD GitHub Actions. Implemented basic entities.
- ✅ **M2 (Auth & Security)**: Configured JWT authentication, roles (`ROLE_USER`, `ROLE_ADMIN`). Implemented `AuthController` and `SecurityConfig`.
- ✅ **M3 (Inventory & GraphQL)**: Implemented parking domain (`parking_lot`, `spot`). Added GraphQL schema to get availability. Configured caching in Redis (annotations `@Cacheable`).
- ✅ **M4 (Reservations & Payments)**: Created reservation logic (CQRS). Added PostgreSQL exclusion constraints (`no_overlapping_reservations`). Integrated dummy payment gateway `PaymentWorker` and configured reservation status updates.
- ✅ **M5 (Sensors & Real-time Anomalies)**: Implemented sensor event processing via RabbitMQ. Added `ReconciliationService` to detect anomalies (occupied without reservation, no-show, sensor dump) and write to `spot_anomaly`.
- ✅ **M6 (Resilience & Webhooks)**: Integrated `Resilience4j` (Circuit Breaker) for calling external payment APIs.
- ✅ **M7 (Frontend Map & Integration)**: Created a web application on React+Vite. Implemented an interactive parking map (displaying free/occupied spaces and anomalies). Added WebSocket subscriptions (`urql`) for real-time.
- ✅ **M8 (Polish & E2E Testing)**: Admin panel, license plate validation (Regex) and sensor simulator have been improved.
- ✅ **M9b (Observability & Monitoring)**: Configured k6 for load testing (10 VUs) and Circuit Breaker verification. Integrated Prometheus and Grafana with automatic provisioning of dashboards for JVM and Spring Boot. Integrated Zipkin (Micrometer Tracing) for tracking distributed queries.