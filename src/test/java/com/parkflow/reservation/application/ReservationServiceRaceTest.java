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
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "CI", matches = "true")
@ActiveProfiles("test")
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "CI", matches = "true")
@org.springframework.context.annotation.Import(com.parkflow.TestcontainersConfiguration.class)
class ReservationServiceRaceTest {

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
        if (testUser == null) {
            testUser = new AppUser(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    "test@example.com",
                    "hash",
                    "Test User",
                    "+380991234567",
                    "USER"
            );
            userRepository.save(testUser);
        }

        if (testSpot == null) {
            testSpot = spotRepository.findAll().get(0);
        }
    }

    @Test
    void testConcurrencyOnBookingSameSpotSameTime() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        Instant from = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant to = from.plus(2, ChronoUnit.HOURS);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    // Using unique idempotency key for each request so it doesn't fail at Redis SETNX layer,
                    // but goes to DB to hit the exclusion constraint.
                    ReservationRequest req = new ReservationRequest(testSpot.getId(), from, to, "AA1234BB");
                    reservationService.createReservation(testUser.getId(), "idem-race-" + index, req);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // start all threads at once
        doneLatch.await(); // wait for all to finish

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(49);
    }
}
