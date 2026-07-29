package com.parkflow.inventory.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingLotController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParkingLotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnOkForParkingLots() throws Exception {
        mockMvc.perform(get("/api/v1/parking-lots"))
                .andExpect(status().isOk());
    }
}
