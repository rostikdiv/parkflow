package com.parkflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: verifies the full application context loads with real infrastructure.
 *
 * Locally on Windows, Docker Desktop 4.60+ uses a new CLI proxy architecture that
 * returns HTTP 400 with stub data on /info — incompatible with docker-java (Testcontainers'
 * underlying library). The test is automatically SKIPPED when Docker is not accessible
 * instead of failing, so the local build still passes.
 * In CI (GitHub Actions / Linux), Docker's Unix socket works correctly and the test runs.
 *
 * See plan §14, Phase A, smoke test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ParkflowApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Test
    void contextLoads() {
        // If this method is reached, the full Spring context loaded successfully
        // with Flyway migrations applied and all beans initialized.
    }
}
