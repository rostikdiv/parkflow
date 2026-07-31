package com.parkflow.inventory.api;

import com.parkflow.inventory.api.dto.SpotResponse;
import com.parkflow.inventory.application.SpotService;
import com.parkflow.inventory.domain.enums.PhysicalStatus;
import com.parkflow.inventory.domain.enums.SpotType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkflow.security.application.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpotController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class SpotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpotService spotService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnSpotById() throws Exception {
        UUID spotId = UUID.randomUUID();
        SpotResponse response = new SpotResponse(
                spotId, UUID.randomUUID(), "A-1", SpotType.STANDARD, PhysicalStatus.FREE, Instant.now(), 0.0, 0.0
        );

        when(spotService.getById(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/spots/{id}", spotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A-1"))
                .andExpect(jsonPath("$.type").value("STANDARD"));
    }
}
