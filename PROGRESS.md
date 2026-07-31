# Хід виконання: ParkFlow

## Реалізовано
- ✅ **Скелет (M1)**: Проект згенеровано, Flyway, Testcontainers (PostgreSQL, Redis, RabbitMQ), CI/CD GitHub Actions налаштовано.
- ✅ **Infrastructure (M2)**: База даних `app_user`, `parking_lot`, `spot`, `reservation`, `sensor_event`. GraphQL-схема (сутності + запити). WebSocket-конфігурація, Publisher/Consumer `SpotAvailabilityEvent`. Кешування у Redis (запити `getAvailability`).
- ✅ **Виправлення та доопрацювання (M2-M3)**: 
  - Налаштовано `SecurityConfig`, щоб `PaymentWorker` міг викликати `MockGatewayController` і оновлювати статус на `CONFIRMED`.
  - Додано обчислення `bookedUntil` та відображення зайнятості для майбутніх періодів, дозволяючи сенсорам НЕ блокувати майбутні бронювання.
  - Виправлено баг з меню профілю через прозорий бекдроп.
  - Додано кнопку "Now" у DatePicker.
  - Оновлено емулятор сенсорів для проходження аутентифікації перед симуляцією бронювань.
  - Додано ідентифікацію `Anomaly` (місце фізично зайняте без бронювання).
  - **Реалізовано Admin Dashboard**: 
    - Новий DTO `AdminReservationResponse` та контролер `AdminReservationController` для перегляду всіх бронювань з пагінацією.
    - Оновлено `SpotAnomalyResponse` (додано `lotId`, `lotName`, `spotCode`).
    - Оновлено `ReservationController` (додано пагінацію `Pageable` для користувацьких бронювань).
    - Фронтенд-компонент `admin-panel.tsx` (вкладки "Бронювання" та "Аномалії", групування за парковкою, можливість закривати аномалії).
    - Захист адмін-панелі (доступна лише для `ROLE_ADMIN` з меню профілю).

## В процесі
- ⏳ **Core Business Logic (M3)**: CQRS для `Reservation`, інтеграція з `PaymentWorker` та `SensorSimulator`.

## Наступні кроки
- Перехід до детального доопрацювання бізнес-логіки платежів та повідомлень.
