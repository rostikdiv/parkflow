package com.parkflow.sensor.api.dto;

import com.parkflow.inventory.domain.enums.PhysicalStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for a single sensor event.
 */
public record SensorEventDto(
        @NotNull String externalEventId,
        @NotNull UUID spotId,
        @NotNull UUID lotId,
        @NotNull PhysicalStatus status,
        @NotNull Instant timestamp
) {
}
