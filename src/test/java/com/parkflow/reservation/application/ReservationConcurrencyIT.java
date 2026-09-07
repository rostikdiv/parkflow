package com.parkflow.reservation.application;

import com.parkflow.TestcontainersConfiguration;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.reservation.api.dto.ReservationRequest;
import com.parkflow.security.domain.AppUser;
import com.parkflow.security.infra.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See plan §4.3, level 3: Exclusion constraint test.
 * 50 concurrent threads simultaneously booking the exact same spot and time window.
 * Verifies that the PostgreSQL EXCLUDE USING gist constraint atomically rejects 49 threads
 * and allows exactly 1 successful reservation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ReservationConcurrencyIT {

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
            testUser = userRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                    .orElseGet(() -> userRepository.save(new AppUser(
                            UUID.fromString("00000000-0000-0000-0000-000000000001"),
                            "driver@parkflow.com",
                            "hash",
                            "Test User",
                            "+380991234567",
                            "USER"
                    )));
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

        long offsetDays = java.util.concurrent.ThreadLocalRandom.current().nextLong(500, 10000);
        Instant from = Instant.now().plus(offsetDays, ChronoUnit.DAYS);
        Instant to = from.plus(2, ChronoUnit.HOURS);
        String runId = UUID.randomUUID().toString().substring(0, 8);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    // Using unique idempotency key for each request so it doesn't fail at Redis SETNX layer,
                    // but reaches PostgreSQL to hit the btree_gist exclusion constraint.
                    ReservationRequest req = new ReservationRequest(testSpot.getId(), from, to, "AA1234BB");
                    reservationService.createReservation(testUser.getId(), "race-" + runId + "-" + index, req);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // start all threads simultaneously
        doneLatch.await(); // wait for all 50 threads to finish

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(49);
    }
}
