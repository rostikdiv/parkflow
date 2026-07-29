package com.parkflow.reservation.application;

import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.reservation.api.dto.ReservationRequest;
import com.parkflow.security.domain.AppUser;
import com.parkflow.security.infra.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class IdempotencyTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private SpotRepository spotRepository;

    private AppUser testUser;
    private Spot testSpot;

    @BeforeEach
    void setup() {
        if (userRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")).isEmpty()) {
            testUser = new AppUser(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    "test2@example.com",
                    "hash",
                    "Test User 2",
                    "+380991234568",
                    "USER"
            );
            userRepository.save(testUser);
        } else {
            testUser = userRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")).get();
        }

        testSpot = spotRepository.findAll().get(1); // Use a different spot just in case
    }

    @Test
    void testIdempotentRequests() {
        Instant from = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant to = from.plus(1, ChronoUnit.HOURS);
        ReservationRequest req = new ReservationRequest(testSpot.getId(), from, to, "KA1234KA");

        String idempotencyKey = UUID.randomUUID().toString();

        // First request should succeed
        var response = reservationService.createReservation(testUser.getId(), idempotencyKey, req);
        assertThat(response).isNotNull();

        // Second request with same key should fail with IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            reservationService.createReservation(testUser.getId(), idempotencyKey, req);
        });
    }
}
