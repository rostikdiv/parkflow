package com.parkflow.reservation.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID spotId,
        String licensePlate,
        Instant startTime,
        Instant endTime,
        String status,
        BigDecimal totalPrice,
        Instant createdAt
) {
}
