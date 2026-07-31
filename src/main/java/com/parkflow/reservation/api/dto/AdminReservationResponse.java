package com.parkflow.reservation.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminReservationResponse(
        UUID id,
        UUID spotId,
        String spotCode,
        UUID lotId,
        String lotName,
        UUID userId,
        String userEmail,
        String userFullName,
        String licensePlate,
        Instant startTime,
        Instant endTime,
        String status,
        BigDecimal totalPrice,
        Instant createdAt
) {
}
