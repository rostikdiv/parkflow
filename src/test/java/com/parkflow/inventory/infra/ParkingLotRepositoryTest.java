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

    @Test
    void shouldFindActiveParkingLotsInBoundingBox() {
        // V3__seed_data.sql should be applied automatically by Flyway during context startup.
        // The Khreshchatyk lot is at 50.4475, 30.5225.
        // We will query a bounding box around it.

        List<ParkingLot> lots = parkingLotRepository.findActiveInBbox(
                50.4000, 30.5000, 50.4500, 30.5500
        );

        assertThat(lots).isNotEmpty();
        // Since V3 creates 3 lots in Kyiv, all of them should fall within this large bbox
        // Khreshchatyk: 50.4475, 30.5225
        // Gulliver: 50.4389, 30.5229
        // Ocean Plaza: 50.4124, 30.5226
        assertThat(lots).hasSize(3);
        assertThat(lots).extracting(ParkingLot::getName).contains("Khreshchatyk Open Air");
    }
}
