package com.parkflow.reservation.domain;

import java.time.Instant;

/**
 * Domain model for reservation status.
 * We use a sealed interface to model state transitions via exhaustive switch expressions.
 * The compiler ensures all states are handled without needing a default branch.
 */
public sealed interface ReservationStatus {
    record Pending(Instant createdAt) implements ReservationStatus {}
    record Confirmed(Instant confirmedAt, String paymentRef) implements ReservationStatus {}
    record Active(Instant checkedInAt) implements ReservationStatus {}
    record Completed(Instant completedAt) implements ReservationStatus {}
    record Expired(String reason) implements ReservationStatus {}
    record Cancelled(String cancelledBy, Instant at) implements ReservationStatus {}
}
