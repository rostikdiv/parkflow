package com.parkflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke integration test: verifies the full application context loads with real infrastructure.
 * Uses Testcontainers Singleton / reuse pattern.
 *
 * See plan §14, Phase A, smoke test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ParkflowApplicationIT {

    @Test
    void contextLoads() {
        // If this method is reached, the full Spring context loaded successfully
        // with Flyway migrations applied and all beans initialized.
    }
}
