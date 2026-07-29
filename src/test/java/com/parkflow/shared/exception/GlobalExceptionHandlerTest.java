package com.parkflow.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@WebMvcTest(
    controllers = GlobalExceptionHandlerTest.TestController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
@ContextConfiguration(classes = {GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn503WithRetryAfterWhenDatabaseIsDown() throws Exception {
        mockMvc.perform(get("/test/db-down"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "5"));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/db-down")
        public void throwException() {
            throw new DataAccessResourceFailureException("DB is down");
        }
    }
}
