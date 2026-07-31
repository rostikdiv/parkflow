package com.parkflow.inventory.infra;

import com.parkflow.inventory.domain.ParkingLot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.context.annotation.Import(com.parkflow.TestcontainersConfiguration.class)
class ParkingLotRepositoryTest {

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private SpotRepository spotRepository;

    @org.junit.jupiter.api.BeforeEach
    void cleanDb() {
        spotRepository.deleteAll();
        parkingLotRepository.deleteAll();
    }

    @Test
    void shouldFindActiveParkingLotsInBoundingBox() {
        // Insert a test parking lot inside the bounding box
        ParkingLot lot = new ParkingLot(
                "Test Khreshchatyk", "Test St", 50.4475, 30.5225,
                com.parkflow.inventory.domain.enums.ParkingLotType.OPEN_AIR, 
                java.math.BigDecimal.TEN, 
                java.time.LocalTime.of(8, 0), 
                java.time.LocalTime.of(20, 0),
                java.time.ZoneId.of("Europe/Kyiv")
        );
        parkingLotRepository.save(lot);

        // Query a bounding box around it
        List<ParkingLot> lots = parkingLotRepository.findActiveInBbox(
                50.4000, 30.5000, 50.4500, 30.5500
        );

        assertThat(lots).isNotEmpty();
        assertThat(lots).hasSize(1);
        assertThat(lots.getFirst().getName()).isEqualTo("Test Khreshchatyk");
    }
}
