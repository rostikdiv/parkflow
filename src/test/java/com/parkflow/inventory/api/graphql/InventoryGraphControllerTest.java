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
}
