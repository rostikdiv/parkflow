package com.parkflow.sensor.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request payload for batch ingestion of sensor events.
 */
public record SensorEventBatchRequest(
        @NotEmpty @Valid List<SensorEventDto> events
) {
}
