package com.parkflow.inventory.api;

import com.parkflow.inventory.api.dto.GeoJsonFeatureCollection;
import com.parkflow.inventory.api.dto.ParkingLotResponse;
import com.parkflow.inventory.application.ParkingLotService;
import com.parkflow.inventory.application.SpotService;
import com.parkflow.inventory.domain.enums.ParkingLotStatus;
import com.parkflow.inventory.domain.enums.ParkingLotType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkflow.security.application.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParkingLotController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class ParkingLotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingLotService parkingLotService;

    @MockBean
    private SpotService spotService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnLotsInBbox() throws Exception {
        ParkingLotResponse response = new ParkingLotResponse(
                UUID.randomUUID(), "Test Lot", "Test Address", 50.0, 30.0,
                ParkingLotType.OPEN_AIR, BigDecimal.TEN, LocalTime.of(8, 0), LocalTime.of(20, 0),
                "Europe/Kyiv", ParkingLotStatus.ACTIVE
        );

        when(parkingLotService.findInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/parking-lots")
                        .param("bbox", "30.0,50.0,31.0,51.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Lot"));
    }

    @Test
    void shouldReturnGeoJson() throws Exception {
        when(parkingLotService.getGeoJson()).thenReturn(new GeoJsonFeatureCollection("FeatureCollection", List.of()));

        mockMvc.perform(get("/api/v1/parking-lots/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }

    @Test
    void shouldReturnSpotsForLot() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(spotService.getByParkingLotId(lotId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/parking-lots/" + lotId + "/spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
