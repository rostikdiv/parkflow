# 📝 Гайд із написання комітів для ParkFlow

## 1. Чому це важливо

Коміти — це не «зберегти зміни», а **історія прийняття рішень**. Рекрутери та техліди читають `git log` у pet-проєктах так само, як код. Хаотичні повідомлення типу `fix`, `update`, `final final v2` створюють враження низької культури розробки.

---

## 2. Специфікація: Conventional Commits

ParkFlow використовує **Conventional Commits 1.0** — де-факто стандарт для Spring/Java-екосистеми та open-source проєктів.

### Формат

```
<type>(<scope>): <короткий опис у present simple>

[optional body — пояснення ЧОМУ, а не ЩО]

[optional footer — BREAKING CHANGE, closes #123, Co-authored-by]
```

### Правила заголовка (перший рядок)
| Правило | Пояснення |
|---|---|
| **≤ 50 символів** | GitHub/IDE обрізає довгі рядки |
| **Не з великої літери** (крім власних назв) | `feat:` а не `Feat:` |
| **Без крапки в кінці** | `add idempotency key` а не `add idempotency key.` |
| **Present simple, imperative mood** | `add`, `fix`, `refactor` — наче даєш команду системі |
| **Чітко відповідає на питання** «що зробить цей коміт, якщо застосувати?» |

---

## 3. Типи комітів (`<type>`)

| Тип | Коли використовувати | Приклад для ParkFlow |
|---|---|---|
| `feat` | Нова функціональність | `feat(reservation): add exclusion constraint for time-range booking` |
| `fix` | Виправлення багу | `fix(sensor): handle null payload in batch ingestion` |
| `refactor` | Зміна коду без зміни поведінки | `refactor(payment): extract retry logic into separate service` |
| `test` | Додавання/оновлення тестів | `test(reservation): add 50-thread race condition test` |
| `docs` | Зміна документації/README | `docs: add architecture diagram to README` |
| `chore` | Рутинні задачі (залежності, CI, конфіги) | `chore(ci): add GitHub Actions workflow` |
| `build` | Зміни в системі збірки (Maven, Docker) | `build(docker): add sensor-emulator Dockerfile` |
| `perf` | Покращення продуктивності | `perf(redis): cache availability query with 30s TTL` |
| `style` | Форматування, пропущені пробіли, `;` | `style: apply google-java-format to domain package` |

> **Не використовуй** `update`, `fix bug`, `wip`, `temp`, `asdf`, `!!!` — це мітки для локальної роботи, не для історії.

---

## 4. Scope (`<scope>`)

Scope — це **модуль** або **шар** системи. Для ParkFlow використовуй:

- `reservation` — бронювання, статуси, аудит
- `inventory` — паркінги, місця, геодані
- `payment` — платежі, шлюз, Resilience4j
- `notification` — сповіщення, email, push
- `sensor` — ingestion, емулятор, події
- `graphql` — схема, резолвери, підписки
- `security` — JWT, ролі, валідація
- `infra` — docker-compose, Terraform, CI/CD
- `shared` — конфігурації, утиліти, exception handler'и

Без scope — тільки якщо зміна глобальна (`docs`, `chore(ci)`).

---

## 5. Body та Footer

### Body (пиши, якщо коміт неочевидний)

- Поясни **чому** зроблено зміну, а не **що** (diff і так покаже «що»).
- Додай контекст: альтернативи, які розглядав, і чому відкинув.
- Для технічних рішень — посилання на розділ плану.

**Хороший body:**
```
feat(reservation): add PostgreSQL exclusion constraint for booking

Використовуємо EXCLUDE USING gist (spot_id WITH =, 
tstzrange(start_time, end_time) WITH &&) замість @Version.

Причина: @Version серіалізує ВСІ бронювання місця, навіть 
неперетинні за часом. Exclusion constraint відсікає тільки 
конфліктні інтервали атомарно на рівні БД.

Рівень 3 драбини рішень з parkflow_final_plan.md §4.3.
Race-тест: 50 потоків → 1 success, 49 ConflictException.
```

### Footer (опціонально)

```
BREAKING CHANGE: змінено формат відповіді API /api/v1/reservations
Closes #42
Co-authored-by: Name <email@example.com>
```

---

## 6. Приклади: хороші vs погані

| ❌ Погано | ✅ Добре |
|---|---|
| `fix bug` | `fix(reservation): reject overlapping bookings via exclusion constraint` |
| `update` | `refactor(sensor): replace synchronized with ReentrantLock for virtual threads` |
| `test` | `test(payment): verify circuit breaker opens after 5 consecutive failures` |
| `add files` | `feat(infra): add docker-compose with Postgres, Redis, RabbitMQ` |
| `final version` | `docs: mark M2 booking milestone as completed in README` |
| `wip` | *(не коміть WIP у main; використовуй `git stash` або feature-гілки)* |

---

## 7. Workflow комітів по мілстоунах

Для ParkFlow рекомендована стратегія:

```
main (protected)
  │
  ├── M0-foundation
  │     └── feat/... коміти фундаменту
  │
  ├── M1-domain-crud
  │     └── feat(inventory): ...
  │     └── feat(reservation): ...
  │
  └── ...
```

### Правила:
1. **Один логічний крок = один коміт.** Не змішуй `feat` і `fix` в одному коміті.
2. **Коміть частіше, ніж раз на день.** Краще 5 маленьких зрозумілих комітів, ніж 1 гігантський «все зробив».
3. **Перед PR — `git rebase -i main`:** прибери WIP-коміти, об'єднай «fix review comments» з основним комітом.
4. **Кожен мілстоун = тег.** Після мержу PR: `git tag -a v0.1 -m "M0: Foundation"` (як у плані §15).

---

## 8. Чекліст перед `git commit`

- [ ] Заголовок ≤ 50 символів?
- [ ] Тип із списку вище?
- [ ] Scope відповідає модулю ParkFlow?
- [ ] Без крапки в кінці?
- [ ] Present simple, imperative mood?
- [ ] Body пояснює ЧОМУ, якщо зміна нетривіальна?
- [ ] `git diff --cached` показує тільки те, що заявлено в заголовку?

---

## 9. Приклад повної історії для M2

```
a1b2c3d feat(reservation): add exclusion constraint for time-range booking
e4f5g6h test(reservation): add 50-thread race condition test
i7j8k9l feat(reservation): implement idempotency key with Redis + DB
m0n1o2p test(reservation): verify idempotency returns 409 on duplicate
q3r4s5t feat(reservation): add ReservationAudit append-only log
u6v7w8x docs: update README with M2 booking milestone status
```

Ця історія читається як **історія прийняття рішень**, а не як «щось зробив і забув».

---

> **Пам'ятай:** `git log` у відкритому репозиторії — це частина твого резюме. Пиши коміти так, наче їх читатиме техлід на співбесіді.
