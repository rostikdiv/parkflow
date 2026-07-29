package com.parkflow.reservation.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReservationStatusTest {

    @Test
    void testExhaustiveSwitch() {
        ReservationStatus status = new ReservationStatus.Pending(Instant.now());

        String result = getStatusName(status);
        assertEquals("Pending", result);

        status = new ReservationStatus.Confirmed(Instant.now(), "REF-123");
        assertEquals("Confirmed with REF-123", getStatusName(status));

        status = new ReservationStatus.Active(Instant.now());
        assertEquals("Active", getStatusName(status));

        status = new ReservationStatus.Completed(Instant.now());
        assertEquals("Completed", getStatusName(status));

        status = new ReservationStatus.Expired("payment_failed");
        assertEquals("Expired: payment_failed", getStatusName(status));

        status = new ReservationStatus.Cancelled("user", Instant.now());
        assertEquals("Cancelled by user", getStatusName(status));
    }

    private String getStatusName(ReservationStatus status) {
        return switch (status) {
            case ReservationStatus.Pending p -> "Pending";
            case ReservationStatus.Confirmed c -> "Confirmed with " + c.paymentRef();
            case ReservationStatus.Active a -> "Active";
            case ReservationStatus.Completed c -> "Completed";
            case ReservationStatus.Expired e -> "Expired: " + e.reason();
            case ReservationStatus.Cancelled c -> "Cancelled by " + c.cancelledBy();
        };
    }
}
