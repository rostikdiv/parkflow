package com.parkflow.inventory.infra;

import com.parkflow.TestcontainersConfiguration;
import com.parkflow.inventory.domain.ParkingLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ParkingLotRepositoryIT {

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private SpotRepository spotRepository;

    // Do not call deleteAll() in shared Testcontainers to avoid breaking foreign key constraints on seeded data

    @Test
    void shouldFindActiveParkingLotsInBoundingBox() {
        // Insert a test parking lot inside a distinct bounding box
        ParkingLot lot = new ParkingLot(
                "Test Isolated Lot", "Test St", 52.4475, 31.5225,
                com.parkflow.inventory.domain.enums.ParkingLotType.OPEN_AIR, 
                java.math.BigDecimal.TEN, 
                java.time.LocalTime.of(8, 0), 
                java.time.LocalTime.of(20, 0),
                java.time.ZoneId.of("Europe/Kyiv")
        );
        parkingLotRepository.save(lot);

        // Query a bounding box around it
        List<ParkingLot> lots = parkingLotRepository.findActiveInBbox(
                52.4000, 31.5000, 52.5000, 31.5500
        );

        assertThat(lots).isNotEmpty();
        assertThat(lots).hasSize(1);
        assertThat(lots.getFirst().getName()).isEqualTo("Test Isolated Lot");
    }
}
