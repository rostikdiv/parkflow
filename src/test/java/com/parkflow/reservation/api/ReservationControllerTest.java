package com.parkflow.reservation.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnBadRequestWithoutIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/reservations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnCreatedWithIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                .header("Idempotency-Key", "test-key-123"))
                .andExpect(status().isCreated());
    }
}
