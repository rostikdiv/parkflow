package com.parkflow.reservation.domain;

import com.parkflow.inventory.domain.Spot;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import com.parkflow.security.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a spot reservation.
 * Uses a unique constraint on idempotencyKey to prevent duplicate bookings,
 * and relies on PostgreSQL EXCLUDE USING gist constraint to prevent overlapping time intervals.
 */
@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private ReservationStatusType status;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Setter
    private Instant updatedAt;

    public Reservation(UUID id, AppUser user, Spot spot, String licensePlate,
                       Instant startTime, Instant endTime, ReservationStatusType status,
                       BigDecimal totalPrice, String idempotencyKey) {
        this.id = id;
        this.user = user;
        this.spot = spot;
        this.licensePlate = licensePlate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.totalPrice = totalPrice;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}
