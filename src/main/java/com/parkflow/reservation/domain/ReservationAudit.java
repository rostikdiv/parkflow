package com.parkflow.reservation.domain;

import com.parkflow.reservation.domain.enums.ReservationStatusType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only log that records every state transition of a Reservation.
 * Written synchronously in the use-case service to maintain a complete history.
 * Uses JSONB for flexible metadata storage (e.g. payment references, cancellation reasons).
 */
@Entity
@Table(name = "reservation_audit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationAudit {

    @Id
    private UUID id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ReservationStatusType fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ReservationStatusType toStatus;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ReservationAudit(UUID id, UUID reservationId, ReservationStatusType fromStatus,
                            ReservationStatusType toStatus, String triggeredBy, String metadata) {
        this.id = id;
        this.reservationId = reservationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.triggeredBy = triggeredBy;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }
}
