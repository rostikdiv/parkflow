package com.parkflow.reservation.infra;

import com.parkflow.reservation.domain.Reservation;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);
    List<Reservation> findByUserIdAndStatus(UUID userId, ReservationStatusType status);
    List<Reservation> findByUserId(UUID userId);

    @Query("SELECT r FROM Reservation r WHERE r.spot.id = :spotId " +
           "AND r.startTime <= :now AND r.endTime >= :now " +
           "AND r.status IN ('CONFIRMED', 'ACTIVE')")
    Optional<Reservation> findActiveReservationForSpot(@Param("spotId") UUID spotId, @Param("now") Instant now);

    @Query("SELECT r.spot.id FROM Reservation r WHERE r.spot.parkingLot.id = :lotId " +
           "AND r.startTime < :to AND r.endTime > :from " +
           "AND r.status NOT IN ('CANCELLED', 'EXPIRED', 'COMPLETED')")
    List<UUID> findReservedSpotIdsInTimeRange(@Param("lotId") UUID lotId,
                                              @Param("from") Instant from,
                                              @Param("to") Instant to);
}
