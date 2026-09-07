package com.parkflow.inventory.api.graphql;

import com.parkflow.inventory.application.ParkingLotService;
import com.parkflow.inventory.application.SpotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@GraphQlTest(InventoryGraphController.class)
class InventoryGraphControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ParkingLotService parkingLotService;

    @MockitoBean
    private SpotService spotService;

    @Test
    void shouldReturnEmptyParkingLotsWhenNoBboxProvided() {
        String document = """
            query {
                parkingLots {
                    id
                    name
                }
            }
        """;

        graphQlTester.document(document)
            .execute()
            .path("parkingLots")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void shouldResolveSpotsViaBatchMapping() {
        UUID lotId = UUID.randomUUID();
        com.parkflow.inventory.api.dto.ParkingLotResponse lot = new com.parkflow.inventory.api.dto.ParkingLotResponse(
                lotId, "Central Lot", "Main St 1", 50.0, 30.0,
                com.parkflow.inventory.domain.enums.ParkingLotType.OPEN_AIR,
                java.math.BigDecimal.valueOf(5.0),
                null, null, "UTC",
                com.parkflow.inventory.domain.enums.ParkingLotStatus.ACTIVE
        );
        when(parkingLotService.getAll()).thenReturn(List.of(lot));

        com.parkflow.inventory.api.dto.SpotResponse spot = new com.parkflow.inventory.api.dto.SpotResponse(
                UUID.randomUUID(), lotId, "A-1",
                com.parkflow.inventory.domain.enums.SpotType.STANDARD,
                com.parkflow.inventory.domain.enums.PhysicalStatus.FREE,
                null, 10.0, 20.0
        );
        when(spotService.getByParkingLotIds(any())).thenReturn(java.util.Map.of(lotId, List.of(spot)));

        String document = """
            query {
                parkingLots {
                    id
                    name
                    spots {
                        id
                        code
                        type
                        physicalStatus
                    }
                }
            }
        """;

        graphQlTester.document(document)
            .execute()
            .path("parkingLots[0].spots[0].code")
            .entity(String.class)
            .isEqualTo("A-1");
    }
}
