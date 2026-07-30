package com.parkflow.reservation.application;

import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.reservation.api.dto.ReservationRequest;
import com.parkflow.reservation.api.dto.ReservationResponse;
import com.parkflow.reservation.domain.Reservation;
import com.parkflow.reservation.domain.ReservationAudit;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import com.parkflow.reservation.infra.ReservationAuditRepository;
import com.parkflow.reservation.infra.ReservationRepository;
import com.parkflow.security.domain.AppUser;
import com.parkflow.security.infra.AppUserRepository;
import com.parkflow.shared.domain.events.PaymentCommand;
import com.parkflow.shared.infra.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business logic for managing reservations.
 * Orchestrates idempotency checks, booking creation, and audit logging.
 */
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationAuditRepository auditRepository;
    private final SpotRepository spotRepository;
    private final AppUserRepository userRepository;
    private final IdempotencyService idempotencyService;
    private final RabbitTemplate rabbitTemplate;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationAuditRepository auditRepository,
                              SpotRepository spotRepository,
                              AppUserRepository userRepository,
                              IdempotencyService idempotencyService,
                              RabbitTemplate rabbitTemplate) {
        this.reservationRepository = reservationRepository;
        this.auditRepository = auditRepository;
        this.spotRepository = spotRepository;
        this.userRepository = userRepository;
        this.idempotencyService = idempotencyService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public ReservationResponse createReservation(UUID userId, String idempotencyKey, ReservationRequest request) {
        // See plan §5: Fast fail-fast for duplicate requests using Redis SETNX.
        // The unique constraint in the DB acts as a durable fallback.
        if (!idempotencyService.tryAcquire(idempotencyKey)) {
            // If we can't acquire the Redis lock, or if it already exists in DB
            reservationRepository.findByIdempotencyKey(idempotencyKey).ifPresent(r -> {
                throw new IllegalStateException("Reservation with this idempotency key already exists");
            });
            throw new IllegalStateException("Request is already being processed");
        }

        // Also check DB constraint as fallback (done by unique index implicitly, but we can check)
        reservationRepository.findByIdempotencyKey(idempotencyKey).ifPresent(r -> {
            throw new IllegalStateException("Reservation with this idempotency key already exists");
        });

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Spot spot = spotRepository.findById(request.spotId())
                .orElseThrow(() -> new IllegalArgumentException("Spot not found"));

        long minutes = Duration.between(request.from(), request.to()).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = spot.getParkingLot().getHourlyRate().multiply(hours);

        // See plan §4.3, level 3: Optimistic locking (@Version) is too coarse for time-range bookings.
        // We rely on the PostgreSQL exclusion constraint (EXCLUDE USING gist) on the reservation table
        // to atomicaly reject overlapping time intervals at the database level.
        Reservation reservation = new Reservation(
                UUID.randomUUID(),
                user,
                spot,
                request.licensePlate(),
                request.from(),
                request.to(),
                ReservationStatusType.PENDING,
                totalPrice,
                idempotencyKey
        );

        reservation = reservationRepository.save(reservation);

        ReservationAudit audit = new ReservationAudit(
                UUID.randomUUID(),
                reservation.getId(),
                null,
                ReservationStatusType.PENDING,
                "USER",
                "{}"
        );
        auditRepository.save(audit);

        PaymentCommand paymentCommand = new PaymentCommand(reservation.getId(), totalPrice, userId);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_PAYMENT, "payment.command", paymentCommand);
                }
            }
        );

        return mapToResponse(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(UUID id, UUID userId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to cancel this reservation");
        }

        if (reservation.getStatus() != ReservationStatusType.PENDING &&
            reservation.getStatus() != ReservationStatusType.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel reservation in status: " + reservation.getStatus());
        }

        ReservationStatusType fromStatus = reservation.getStatus();
        reservation.setStatus(ReservationStatusType.CANCELLED);
        reservation.setUpdatedAt(java.time.Instant.now());

        ReservationAudit audit = new ReservationAudit(
                UUID.randomUUID(),
                reservation.getId(),
                fromStatus,
                ReservationStatusType.CANCELLED,
                "USER",
                "{}"
        );
        auditRepository.save(audit);

        return mapToResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(UUID userId, ReservationStatusType status) {
        List<Reservation> reservations = (status != null)
                ? reservationRepository.findByUserIdAndStatus(userId, status)
                : reservationRepository.findByUserId(userId);

        return reservations.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(UUID id, UUID userId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized");
        }

        return mapToResponse(reservation);
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSpot().getId(),
                reservation.getLicensePlate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus().name(),
                reservation.getTotalPrice(),
                reservation.getCreatedAt()
        );
    }
}
