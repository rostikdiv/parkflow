package com.parkflow.inventory.infra;

import com.parkflow.inventory.domain.ParkingLot;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.domain.enums.ParkingLotType;
import com.parkflow.inventory.domain.enums.SpotType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.context.annotation.Import(com.parkflow.TestcontainersConfiguration.class)
class SpotRepositoryTest {

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @org.junit.jupiter.api.BeforeEach
    void cleanDb() {
        spotRepository.deleteAll();
        // Don't delete parking lots here because ParkingLotRepositoryTest relies on seed data!
        // Or wait, if we delete them, we lose the V3__seed_data.sql!
        // The seed data has 3 parking lots. If we delete them, they won't be re-created unless Flyway runs again.
        // Actually, @DataJpaTest already rolls back. 
    }

    @Test
    void shouldFindByParkingLotId() {
        ParkingLot lot = new ParkingLot(
                "Test Lot", "Test Address", 50.0, 30.0,
                ParkingLotType.OPEN_AIR, BigDecimal.TEN, LocalTime.of(8, 0), LocalTime.of(20, 0),
                ZoneId.of("Europe/Kyiv")
        );
        parkingLotRepository.save(lot);

        Spot spot = new Spot("A-1", SpotType.STANDARD, 0.0, 0.0);
        lot.addSpot(spot);
        spotRepository.save(spot);

        List<Spot> found = spotRepository.findByParkingLotId(lot.getId());

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getCode()).isEqualTo("A-1");
    }
}
