# ParkFlow Project Progress

## Completed Milestones
- [x] **M1: Project Initialization & Infrastructure**
- [x] **M2: Domain Model & Database Schema**
- [x] **M3: Core Logic (Reservations, Auth)**
- [x] **M4: Asynchronous Processing (RabbitMQ)**
- [x] **M5: Caching & Performance (Redis)**
- [x] **M6: Admin Panel Frontend**
- [x] **M7: Resilience & Fault Tolerance**
- [x] **M8: Sensor Emulator**
- [x] **M9a: Cloud Infrastructure Setup (GCP)**
  - Terraform modules for VPC, Secret Manager, Cloud SQL, Compute Engine (Redis/RabbitMQ), and Cloud Run.
  - Successfully deployed the ParkFlow backend to Cloud Run.

## Pending Milestones
- [x] **M9b: Frontend & Load Testing**
  - [x] Implement Cold Start UX on Frontend.
  - [x] Interactive Sensor Emulator via Admin Panel.
  - [x] Deploy frontend to Vercel/Firebase.
  - [x] Run k6 load tests against the cloud environment (Script Ready).
- [x] **M10: Final Polish & Documentation**
  - [x] M10.1: Security Audit & Hardening (Backend API Keys, Exception Hiding).
  - [x] M10.2: Frontend Updates, Tests & Localization.
  - [x] M10.3: CI/CD Pipeline Configuration (GitHub Actions, WIF, Vercel).

## Completed Audit: Database, Performance & Testing
- [x] **Database Optimization (N+1, Batching, btree_gist)**
  - Configured JDBC batching (`batch_size: 25`, `order_inserts: true`, `order_updates: true`) and Hibernate SELECT batch fetching (`default_batch_fetch_size: 30`).
  - Added Flyway migration `V9__add_performance_indexes.sql` with B-tree indexes for foreign keys and query access patterns.
  - Eliminated JPA N+1 queries using `@EntityGraph` on `ReservationRepository` and `SpotAnomalyRepository`.
  - Added `@BatchSize(size = 50)` on `ParkingLot.spots` to prevent collection N+1 and avoid `HHH000104` in-memory pagination warnings.
  - Implemented GraphQL `@BatchMapping(typeName = "ParkingLot", field = "spots")` for single-query resolution.
- [x] **Local Testcontainers & CI/CD Verification**
  - Configured `maven-failsafe-plugin` and `maven-surefire-plugin` in `windows-docker` profile (TCP 2375, API 1.44) isolated from Linux CI.
  - Enforced `./mvnw clean verify -B` in GitHub Actions CI workflow before Docker packaging.
- [x] **JaCoCo Coverage & Test Separation**
  - Integrated `jacoco-maven-plugin:0.8.12` with report generation bound to fast unit testing (`test` phase).
  - Separated Unit tests (`*Test.java`) and Testcontainers Integration tests (`*IT.java`).
  - Line coverage increased from 33.43% to 62.77% (+29.34%), branch coverage from 35.53% to 42.80% (+7.27%).

