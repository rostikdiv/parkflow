package com.parkflow.infra;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase A: Validates that Flyway migrations (V1 and V2) apply correctly to a real Postgres instance.
 *
 * Locally on Windows, Docker Desktop 4.60+ CLI proxy is incompatible with docker-java,
 * so this test is SKIPPED (not failed) when Docker is unavailable. In CI (Linux), it runs.
 * See plan §14, Phase A, @DataJpaTest migration validation.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationsShouldApplySuccessfully() throws Exception {
        assertNotNull(dataSource);
        try (Connection conn = dataSource.getConnection()) {
            assertTrue(conn.isValid(1), "Database connection must be valid after Flyway migration");
        }
    }

    @Test
    void baseTablesShouldExistAfterV2Migration() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Verify V2 migration created all expected tables per plan §4.1
            String[] expectedTables = {
                "parking_lot", "spot", "app_user", "reservation",
                "payment", "sensor_event", "spot_anomaly", "reservation_audit"
            };
            for (String table : expectedTables) {
                ResultSet rs = conn.getMetaData().getTables(null, "public", table, new String[]{"TABLE"});
                assertTrue(rs.next(), "Table '" + table + "' must exist after V2 migration");
            }
        }
    }
}
