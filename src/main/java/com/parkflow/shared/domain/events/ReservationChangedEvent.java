package com.parkflow.shared.domain.events;

import java.util.UUID;

public record ReservationChangedEvent(
    UUID reservationId,
    UUID spotId
) {}
