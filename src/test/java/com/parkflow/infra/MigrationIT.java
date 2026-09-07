package com.parkflow.infra;

import com.parkflow.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase A: Validates that Flyway migrations (including V9 performance indexes) apply correctly to a real Postgres instance.
 * See plan §14, Phase A, @DataJpaTest migration validation.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MigrationIT {

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
