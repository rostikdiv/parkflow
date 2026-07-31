package com.parkflow.reservation.infra;

import com.parkflow.reservation.domain.Reservation;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);
    Page<Reservation> findByUserIdAndStatus(UUID userId, ReservationStatusType status, Pageable pageable);
    Page<Reservation> findByUserId(UUID userId, Pageable pageable);

    Page<Reservation> findByStatus(ReservationStatusType status, Pageable pageable);
    // Inherited from JpaRepository: Page<Reservation> findAll(Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE r.spot.id = :spotId " +
           "AND r.startTime <= :now AND r.endTime >= :now " +
           "AND r.status IN ('CONFIRMED', 'ACTIVE')")
    Optional<Reservation> findActiveReservationForSpot(@Param("spotId") UUID spotId, @Param("now") Instant now);

    @Query("SELECT r FROM Reservation r WHERE r.spot.parkingLot.id = :lotId " +
           "AND r.startTime < :to AND r.endTime > :from " +
           "AND r.status NOT IN ('CANCELLED', 'EXPIRED', 'COMPLETED')")
    List<Reservation> findOverlappingReservations(@Param("lotId") UUID lotId,
                                                  @Param("from") Instant from,
                                                  @Param("to") Instant to);
}
