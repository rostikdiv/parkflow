# 📝 ParkFlow Commit Writing Guide

## 1. Why it’s important

Commits aren’t about “saving changes”, they’re about **decision history**. Recruiters and tech leads read `git log` in pet projects just like they read code. Chaotic messages like `fix`, `update`, `final final v2` give the impression of a low development culture.

---

## 2. Specification: Conventional Commits

ParkFlow uses **Conventional Commits 1.0** — the de facto standard for Spring/Java ecosystem and open-source projects.

### Format

```
<type>(<scope>): <short description in present simple>

[optional body — explanation of WHY, not WHAT]

[optional footer — BREAKING CHANGE, closes #123, Co-authored-by]

```

### Header rules (first line)
| Rule | Explanation |
|---|---|
| **≤ 50 characters** | GitHub/IDE truncates long lines |
| **English only** | All commits (both title and body) MUST be in English. |
| **Not capitalized** (except for proper names) | `feat:` instead of `Feat:` |
| **No period at the end** | `add idempotency key` instead of `add idempotency key.` |
| **Present simple, imperative mood** | `add`, `fix`, `refactor` — as if giving a command to the system |
| **Clearly answers the question** "what will this commit do if applied?" |

---

## 3. Commit types (`<type>`)

| Type | When to use | Example for ParkFlow |
|---|---|---|
| `feat` | New feature | `feat(reservation): add exclusion constraint for time-range booking` |
| `fix` | Bug fix | `fix(sensor): handle null payload in batch ingestion` |
| `refactor` | Change code without changing behavior | `refactor(payment): extract retry logic into separate service` |
| `test` | Add/update tests | `test(reservation): add 50-thread race condition test` |
| `docs` | Change documentation/README | `docs: add architecture diagram to README` |
| `chore` | Routine tasks (dependencies, CI, configs) | `chore(ci): add GitHub Actions workflow` |
| `build` | Build system changes (Maven, Docker) | `build(docker): add sensor-emulator Dockerfile` |
| `perf` | Performance improvements | `perf(redis): cache availability query with 30s TTL` |
| `style` | Formatting, missing spaces, `;` | `style: apply google-java-format to domain package` |

> **Do not use** `update`, `fix bug`, `wip`, `temp`, `asdf`, `!!!` — these are labels for local work, not for history.

---

## 4. Scope (`<scope>`)

Scope is a **module** or **layer** of the system. For ParkFlow use:

- `reservation` — reservations, statuses, audit
- `inventory` — parking lots, places, geodata
- `payment` — payments, gateway, Resilience4j
- `notification` — notifications, email, push
- `sensor` — ingestion, emulator, events
- `graphql` — schema, resolvers, subscriptions
- `security` — JWT, roles, validation
- `infra` — docker-compose, Terraform, CI/CD
- `shared` — configurations, utilities, exception handlers

Without scope — only if the change is global (`docs`, `chore(ci)`).

---

## 5. Body and Footer

### Body (write if the commit is not obvious)

- Explain **why** the change was made, not **what** (diff will show "what" anyway).
- Add context: alternatives you considered and why you rejected them.
- For technical solutions, link to the plan section.

**Good body:**
```
feat(reservation): add PostgreSQL exclusion constraint for booking

Use EXCLUDE USING gist (spot_id WITH =, tstzrange(start_time, end_time) WITH &&)
instead of @Version optimistic locking.

Reason: @Version serializes ALL bookings for a spot, even those that
do not overlap in time. Exclusion constraint rejects only intersecting
intervals atomically at the DB level.

Level 3 of the decision ladder from parkflow_final_plan.md §4.3.
Race-test: 50 threads → 1 success, 49 ConflictException.
```

### Footer (optional)

```
BREAKING CHANGE: changed API response format /api/v1/reservations
Closes #42
Co-authored-by: Name <email@example.com>
```

---

## 6. Examples: good vs bad

| ❌ Bad | ✅ Good |
|---|---|
| `fix bug` | `fix(reservation): reject overlapping bookings via exclusion constraint` |
| `update` | `refactor(sensor): replace synchronized with ReentrantLock for virtual threads` |
| `test` | `test(payment): verify circuit breaker opens after 5 consecutive failures` |
| `add files` | `feat(infra): add docker-compose with Postgres, Redis, RabbitMQ` |
| `final version` | `docs: mark M2 booking milestone as completed in README` |
| `wip` | *(don't commit WIP to main; use `git stash` or feature branches)* |

---

## 7. Milestone commit workflow

For ParkFlow, the recommended strategy is:

```
main (protected)
│
├── M0-foundation
│ └── feat/... foundation commits
│
├── M1-domain-crud
│ └── feat(inventory): ...
│ └── feat(reservation): ...
│
└── ...
```

### Rules:
1. **One logical step = one commit.** Don't mix `feat` and `fix` in the same commit.
2. **Commit more often than once a day.** 5 small, understandable commits are better than 1 giant "done everything".
3. **Before PR — `git rebase -i main`:** remove WIP commits, merge "fix review comments" with main commit.
4. **Every month