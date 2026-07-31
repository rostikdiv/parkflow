package com.parkflow.reservation.application;

import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.reservation.api.dto.AdminReservationResponse;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.parkflow.shared.domain.events.ReservationChangedEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.UUID;

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
    private final ApplicationEventPublisher applicationEventPublisher;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationAuditRepository auditRepository,
                              SpotRepository spotRepository,
                              AppUserRepository userRepository,
                              IdempotencyService idempotencyService,
                              RabbitTemplate rabbitTemplate,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.reservationRepository = reservationRepository;
        this.auditRepository = auditRepository;
        this.spotRepository = spotRepository;
        this.userRepository = userRepository;
        this.idempotencyService = idempotencyService;
        this.rabbitTemplate = rabbitTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public ReservationResponse createReservation(UUID userId, String idempotencyKey, ReservationRequest request) {
        if (request.from().isBefore(java.time.Instant.now().minusSeconds(60))) {
            throw new IllegalArgumentException("Cannot book in the past");
        }

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

        try {
            reservation = reservationRepository.save(reservation);
            // Flush is necessary here to trigger the exclusion constraint immediately before method returns
            reservationRepository.flush();
        } catch (DataIntegrityViolationException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getCause() != null && cause.getCause().getMessage() != null && 
                cause.getCause().getMessage().contains("no_overlapping_reservations")) {
                throw new IllegalStateException("Spot already reserved in requested time range");
            }
            throw e;
        }

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

        applicationEventPublisher.publishEvent(
            new ReservationChangedEvent(reservation.getId(), reservation.getSpot().getId())
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

        applicationEventPublisher.publishEvent(
            new ReservationChangedEvent(reservation.getId(), reservation.getSpot().getId())
        );

        return mapToResponse(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getMyReservations(UUID userId, ReservationStatusType status, Pageable pageable) {
        Page<Reservation> reservations = (status != null)
                ? reservationRepository.findByUserIdAndStatus(userId, status, pageable)
                : reservationRepository.findByUserId(userId, pageable);

        return reservations.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AdminReservationResponse> getAllReservations(ReservationStatusType status, Pageable pageable) {
        Page<Reservation> reservations = (status != null)
                ? reservationRepository.findByStatus(status, pageable)
                : reservationRepository.findAll(pageable);

        return reservations.map(this::mapToAdminResponse);
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
                reservation.getSpot().getCode(),
                reservation.getSpot().getParkingLot().getId(),
                reservation.getSpot().getParkingLot().getName(),
                reservation.getLicensePlate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus().name(),
                reservation.getTotalPrice(),
                reservation.getCreatedAt()
        );
    }

    private AdminReservationResponse mapToAdminResponse(Reservation reservation) {
        return new AdminReservationResponse(
                reservation.getId(),
                reservation.getSpot().getId(),
                reservation.getSpot().getCode(),
                reservation.getSpot().getParkingLot().getId(),
                reservation.getSpot().getParkingLot().getName(),
                reservation.getUser().getId(),
                reservation.getUser().getEmail(),
                reservation.getUser().getFullName(),
                reservation.getLicensePlate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus().name(),
                reservation.getTotalPrice(),
                reservation.getCreatedAt()
        );
    }
}
