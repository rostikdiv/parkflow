package com.parkflow.reservation.application;

import com.parkflow.inventory.domain.ParkingLot;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.domain.enums.ParkingLotStatus;
import com.parkflow.inventory.domain.enums.ParkingLotType;
import com.parkflow.inventory.domain.enums.SpotType;
import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.reservation.api.dto.ReservationRequest;
import com.parkflow.reservation.api.dto.ReservationResponse;
import com.parkflow.reservation.domain.Reservation;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import com.parkflow.reservation.infra.ReservationAuditRepository;
import com.parkflow.reservation.infra.ReservationRepository;
import com.parkflow.security.domain.AppUser;
import com.parkflow.security.infra.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase B business logic unit tests for ReservationService.
 * Strictly tests domain business rules, pricing calculation, state transitions,
 * and error handling without external container overhead.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationAuditRepository auditRepository;
    @Mock
    private SpotRepository spotRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ReservationService reservationService;

    private UUID userId;
    private UUID spotId;
    private AppUser user;
    private ParkingLot parkingLot;
    private Spot spot;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                auditRepository,
                spotRepository,
                userRepository,
                idempotencyService,
                rabbitTemplate,
                applicationEventPublisher
        );

        userId = UUID.randomUUID();
        spotId = UUID.randomUUID();

        user = new AppUser(userId, "driver@parkflow.com", "secret", "John Driver", "+380501112233", "USER");

        parkingLot = new ParkingLot(
                "City Mall Lot", "Deribasivska 1", 46.48, 30.73,
                ParkingLotType.OPEN_AIR, BigDecimal.valueOf(10.00),
                LocalTime.of(8, 0), LocalTime.of(22, 0), ZoneId.of("Europe/Kyiv")
        );

        spot = new Spot("A-10", SpotType.STANDARD, 10.0, 15.0);
        try {
            java.lang.reflect.Field idField = Spot.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(spot, spotId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        parkingLot.addSpot(spot);
    }

    @Test
    @DisplayName("Should calculate total price correctly for 2-hour reservation")
    void shouldCalculateTotalPriceForTwoHours() {
        String idempotencyKey = "key-1";
        Instant from = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant to = from.plus(2, ChronoUnit.HOURS);
        ReservationRequest request = new ReservationRequest(spotId, from, to, "BH1234AA");

        when(idempotencyService.tryAcquire(idempotencyKey)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            ReservationResponse response = reservationService.createReservation(userId, idempotencyKey, request);

            assertThat(response).isNotNull();
            // 2 hours * 10.00 rate = 20.00
            assertThat(response.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
            assertThat(response.licensePlate()).isEqualTo("BH1234AA");
            assertThat(response.status()).isEqualTo("PENDING");

            verify(auditRepository).save(any());
            verify(applicationEventPublisher).publishEvent(any(Object.class));
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Should reject reservation if start time is in the past")
    void shouldRejectBookingInThePast() {
        Instant pastTime = Instant.now().minus(2, ChronoUnit.MINUTES);
        ReservationRequest request = new ReservationRequest(spotId, pastTime, pastTime.plus(1, ChronoUnit.HOURS), "AA1111AA");

        assertThatThrownBy(() -> reservationService.createReservation(userId, "key-past", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot book in the past");

        verifyNoInteractions(idempotencyService);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    @DisplayName("Should reject request if idempotency key is already processing")
    void shouldRejectConcurrentIdempotencyKey() {
        String idempotencyKey = "key-duplicate";
        Instant from = Instant.now().plus(1, ChronoUnit.HOURS);
        ReservationRequest request = new ReservationRequest(spotId, from, from.plus(1, ChronoUnit.HOURS), "AA1111AA");

        when(idempotencyService.tryAcquire(idempotencyKey)).thenReturn(false);
        when(reservationRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(userId, idempotencyKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Request is already being processed");
    }

    @Test
    @DisplayName("Should reject request if idempotency key already exists in DB")
    void shouldRejectExistingIdempotencyKeyInDb() {
        String idempotencyKey = "key-existing";
        Instant from = Instant.now().plus(1, ChronoUnit.HOURS);
        ReservationRequest request = new ReservationRequest(spotId, from, from.plus(1, ChronoUnit.HOURS), "AA1111AA");

        when(idempotencyService.tryAcquire(idempotencyKey)).thenReturn(true);
        when(reservationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(mock(Reservation.class)));

        assertThatThrownBy(() -> reservationService.createReservation(userId, idempotencyKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reservation with this idempotency key already exists");
    }

    @Test
    @DisplayName("Should convert exclusion constraint violation to friendly IllegalStateException")
    void shouldTranslateExclusionConstraintViolation() {
        String idempotencyKey = "key-overlap";
        Instant from = Instant.now().plus(1, ChronoUnit.HOURS);
        ReservationRequest request = new ReservationRequest(spotId, from, from.plus(1, ChronoUnit.HOURS), "AA1111AA");

        when(idempotencyService.tryAcquire(idempotencyKey)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));

        Throwable rootCause = new RuntimeException("ERROR: conflicting key value violates exclusion constraint \"no_overlapping_reservations\"");
        Throwable intermediateCause = new RuntimeException(rootCause);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Constraint violation", intermediateCause);

        when(reservationRepository.save(any(Reservation.class))).thenThrow(exception);

        assertThatThrownBy(() -> reservationService.createReservation(userId, idempotencyKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Spot already reserved in requested time range");
    }

    @Test
    @DisplayName("Should allow user to cancel their pending reservation")
    void shouldCancelPendingReservationSuccessfully() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = new Reservation(
                reservationId, user, spot, "AA1111AA",
                Instant.now().plus(1, ChronoUnit.HOURS), Instant.now().plus(2, ChronoUnit.HOURS),
                ReservationStatusType.PENDING, BigDecimal.TEN, "key-cancel"
        );

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.cancelReservation(reservationId, userId);

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatusType.CANCELLED);
        verify(auditRepository).save(any());
        verify(applicationEventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Should prevent canceling someone else's reservation")
    void shouldPreventCancelingAnotherUserReservation() {
        UUID reservationId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Reservation reservation = new Reservation(
                reservationId, user, spot, "AA1111AA",
                Instant.now().plus(1, ChronoUnit.HOURS), Instant.now().plus(2, ChronoUnit.HOURS),
                ReservationStatusType.PENDING, BigDecimal.TEN, "key-cancel-other"
        );

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId, otherUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not authorized to cancel this reservation");
    }

    @Test
    @DisplayName("Should prevent canceling completed or expired reservations")
    void shouldPreventCancelingCompletedReservation() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = new Reservation(
                reservationId, user, spot, "AA1111AA",
                Instant.now().minus(3, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS),
                ReservationStatusType.COMPLETED, BigDecimal.TEN, "key-completed"
        );

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel reservation in status: COMPLETED");
    }

    @Test
    @DisplayName("Should filter my reservations by status when provided")
    void shouldFilterMyReservationsByStatus() {
        PageRequest pageable = PageRequest.of(0, 10);
        Reservation reservation = new Reservation(
                UUID.randomUUID(), user, spot, "AA1111AA",
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
                ReservationStatusType.CONFIRMED, BigDecimal.TEN, "key-my-1"
        );
        Page<Reservation> page = new PageImpl<>(List.of(reservation));

        when(reservationRepository.findByUserIdAndStatus(userId, ReservationStatusType.CONFIRMED, pageable))
                .thenReturn(page);

        Page<ReservationResponse> result = reservationService.getMyReservations(userId, ReservationStatusType.CONFIRMED, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo("CONFIRMED");
        verify(reservationRepository, never()).findByUserId(any(), any());
    }
}
