package com.parkflow.inventory.api.dto;

import com.parkflow.inventory.domain.enums.PhysicalStatus;
import com.parkflow.inventory.domain.enums.SpotType;

import java.time.Instant;
import java.util.UUID;

public record SpotResponse(
        UUID id,
        UUID parkingLotId,
        String code,
        SpotType type,
        PhysicalStatus physicalStatus,
        Instant lastSensorUpdate,
        Double layoutX,
        Double layoutY
) {
}
