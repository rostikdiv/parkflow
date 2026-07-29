package com.parkflow.inventory.application;

import com.parkflow.inventory.api.dto.SpotResponse;
import com.parkflow.inventory.domain.ParkingLot;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.domain.enums.ParkingLotType;
import com.parkflow.inventory.domain.enums.SpotType;
import com.parkflow.inventory.infra.SpotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotServiceTest {

    @Mock
    private SpotRepository spotRepository;

    @InjectMocks
    private SpotService spotService;

    @Test
    void shouldReturnSpotsByParkingLotId() {
        ParkingLot lot = new ParkingLot(
                "Test Lot", "Test Address", 50.0, 30.0,
                ParkingLotType.OPEN_AIR, BigDecimal.TEN, LocalTime.of(8, 0), LocalTime.of(20, 0),
                ZoneId.of("Europe/Kyiv")
        );
        UUID lotId = UUID.randomUUID();
        ReflectionTestUtils.setField(lot, "id", lotId);
        
        Spot spot = new Spot("A-1", SpotType.STANDARD, 0.0, 0.0);
        lot.addSpot(spot);

        when(spotRepository.findByParkingLotId(lotId)).thenReturn(List.of(spot));

        List<SpotResponse> responses = spotService.getByParkingLotId(lotId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().code()).isEqualTo("A-1");
    }
}
