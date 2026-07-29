package com.parkflow.reservation.infra;

import com.parkflow.reservation.domain.ReservationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationAuditRepository extends JpaRepository<ReservationAudit, UUID> {
}
