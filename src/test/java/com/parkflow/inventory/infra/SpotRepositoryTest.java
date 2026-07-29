package com.parkflow.inventory.infra;

import com.parkflow.inventory.domain.ParkingLot;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.domain.enums.ParkingLotType;
import com.parkflow.inventory.domain.enums.SpotType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class SpotRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

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
