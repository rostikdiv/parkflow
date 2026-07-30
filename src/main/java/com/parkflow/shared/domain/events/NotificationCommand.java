package com.parkflow.shared.domain.events;

import java.util.UUID;

public record NotificationCommand(
        UUID userId,
        String type, // e.g., "RESERVATION_CONFIRMED"
        UUID reservationId
) {}
