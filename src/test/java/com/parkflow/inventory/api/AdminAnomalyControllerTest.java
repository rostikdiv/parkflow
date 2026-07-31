package com.parkflow.inventory.api;

import com.parkflow.inventory.api.dto.SpotAnomalyResponse;
import com.parkflow.inventory.application.AnomalyService;
import com.parkflow.inventory.domain.enums.SpotAnomalyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import com.parkflow.security.application.JwtService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@WebMvcTest(
    controllers = AdminAnomalyController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
class AdminAnomalyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnomalyService anomalyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnAnomalies() throws Exception {
        UUID anomalyId = UUID.randomUUID();
        UUID spotId = UUID.randomUUID();
        SpotAnomalyResponse response = new SpotAnomalyResponse(
                anomalyId, spotId, "A-01", UUID.randomUUID(), "Test Lot", SpotAnomalyType.SENSOR_SILENT, "test", Instant.now(), null
        );

        when(anomalyService.getAnomalies(false)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/admin/v1/anomalies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(anomalyId.toString()))
                .andExpect(jsonPath("$[0].type").value("SENSOR_SILENT"));
    }

    @Test
    void shouldResolveAnomaly() throws Exception {
        UUID anomalyId = UUID.randomUUID();
        
        doNothing().when(anomalyService).resolveAnomaly(anomalyId);

        mockMvc.perform(post("/api/admin/v1/anomalies/{id}/resolve", anomalyId))
                .andExpect(status().isOk());
    }
}
