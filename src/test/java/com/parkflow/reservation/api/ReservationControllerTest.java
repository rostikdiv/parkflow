package com.parkflow.reservation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkflow.reservation.api.dto.ReservationRequest;
import com.parkflow.reservation.api.dto.ReservationResponse;
import com.parkflow.reservation.application.ReservationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for this test if any
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    @Autowired
    private ObjectMapper objectMapper;

    @org.junit.jupiter.api.BeforeEach
    void setupSecurity() {
        com.parkflow.security.domain.AppUser testUser = new com.parkflow.security.domain.AppUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"), 
                "test@test.com", "pass", "Test", "123", "USER");
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth = 
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldCreateReservation() throws Exception {
        UUID spotId = UUID.randomUUID();
        Instant from = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant to = from.plus(2, ChronoUnit.HOURS);
        ReservationRequest request = new ReservationRequest(spotId, from, to, "AA1234BB");

        ReservationResponse response = new ReservationResponse(
                UUID.randomUUID(), spotId, "M-01", UUID.randomUUID(), "Test Lot", "AA1234BB",
                from, to, "PENDING", BigDecimal.valueOf(100), Instant.now()
        );

        Mockito.when(reservationService.createReservation(any(UUID.class), eq("test-idempotency-key"), any(ReservationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations")
                        .header("Idempotency-Key", "test-idempotency-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("AA1234BB"));
    }

    @Test
    void shouldReturnBadRequestIfIdempotencyKeyMissing() throws Exception {
        UUID spotId = UUID.randomUUID();
        Instant from = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant to = from.plus(2, ChronoUnit.HOURS);
        ReservationRequest request = new ReservationRequest(spotId, from, to, "AA1234BB");

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCancelReservation() throws Exception {
        UUID resId = UUID.randomUUID();
        ReservationResponse response = new ReservationResponse(
                resId, UUID.randomUUID(), "M-01", UUID.randomUUID(), "Test Lot", "AA1234BB",
                Instant.now(), Instant.now(), "CANCELLED", BigDecimal.valueOf(100), Instant.now()
        );

        Mockito.when(reservationService.cancelReservation(eq(resId), any(UUID.class)))
                .thenReturn(response);

        mockMvc.perform(delete("/api/v1/reservations/" + resId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
