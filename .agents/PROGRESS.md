# Хід виконання: ParkFlow

## Реалізовано (M1 - M9b)
- ✅ **M1 (Project Skeleton & Database)**: Згенеровано Spring Boot додаток. Налаштовано Flyway, Testcontainers (PostgreSQL, Redis, RabbitMQ), CI/CD GitHub Actions. Реалізовано базові сутності.
- ✅ **M2 (Auth & Security)**: Налаштовано JWT-аутентифікацію, ролі (`ROLE_USER`, `ROLE_ADMIN`). Реалізовано `AuthController` та `SecurityConfig`.
- ✅ **M3 (Inventory & GraphQL)**: Реалізовано домен паркомісць (`parking_lot`, `spot`). Додано GraphQL-схему для отримання доступності. Налаштовано кешування у Redis (анотації `@Cacheable`).
- ✅ **M4 (Reservations & Payments)**: Створено логіку бронювання (CQRS). Додано PostgreSQL exclusion constraints (`no_overlapping_reservations`). Інтегровано фіктивний платіжний шлюз `PaymentWorker` та налаштовано оновлення статусу бронювання.
- ✅ **M5 (Sensors & Real-time Anomalies)**: Реалізовано обробку подій сенсорів через RabbitMQ. Додано `ReconciliationService` для виявлення аномалій (зайнято без броні, не приїхав, відвал сенсора) та запис у `spot_anomaly`.
- ✅ **M6 (Resilience & Webhooks)**: Інтегровано `Resilience4j` (Circuit Breaker) для викликів зовнішніх API платежів.
- ✅ **M7 (Frontend Map & Integration)**: Створено веб-застосунок на React+Vite. Реалізовано інтерактивну мапу парковки (відображення вільних/зайнятих місць та аномалій). Додано WebSocket підписки (`urql`) для реального часу.
- ✅ **M8 (Polish & E2E Testing)**: Допрацьовано адмін-панель, валідацію номерних знаків (Regex) та симулятор сенсорів.
- ✅ **M9b (Observability & Monitoring)**: Налаштовано k6 для навантажувального тестування (10 VUs) і перевірки Circuit Breaker. Інтегровано Prometheus та Grafana з автоматичним provisioning дашбордів для JVM та Spring Boot. Інтегровано Zipkin (Micrometer Tracing) для відстеження розподілених запитів.

## В процесі
- ⏳ **M9a (Cloud & Infrastructure)**: Написання Terraform-скриптів для GCP (VPC, Cloud SQL, Memorystore, Cloud Run/GKE).

## Наступні кроки
- 🚀 Деплой системи в Google Cloud та фінальне тестування в хмарі.
