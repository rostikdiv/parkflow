package com.parkflow.reservation.infra;

import com.parkflow.reservation.domain.Reservation;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);
    List<Reservation> findByUserIdAndStatus(UUID userId, ReservationStatusType status);
    List<Reservation> findByUserId(UUID userId);
}
