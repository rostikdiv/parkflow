package com.parkflow.inventory.api;

import com.parkflow.inventory.application.AnomalyService;
import com.parkflow.security.domain.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.parkflow.TestcontainersConfiguration.class)
class AdminAnomalyControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnomalyService anomalyService;

    @Test
    void shouldDenyAccessIfNoRoleAdmin() throws Exception {
        // Setup User without ADMIN role
        AppUser testUser = new AppUser(UUID.randomUUID(), "user@example.com", "pass", "User", "123", "USER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(anomalyService.getAnomalies(false)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/v1/anomalies"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAccessIfRoleAdmin() throws Exception {
        // Setup User with ADMIN role
        AppUser adminUser = new AppUser(UUID.randomUUID(), "admin@example.com", "pass", "Admin", "123", "ADMIN");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(anomalyService.getAnomalies(false)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/v1/anomalies"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessIfNotAuthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/admin/v1/anomalies"))
                .andExpect(status().isForbidden());
    }
}
