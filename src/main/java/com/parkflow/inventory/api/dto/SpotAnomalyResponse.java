package com.parkflow.inventory.api.dto;

import com.parkflow.inventory.domain.enums.SpotAnomalyType;
import java.time.Instant;
import java.util.UUID;

public record SpotAnomalyResponse(
    UUID id,
    UUID spotId,
    SpotAnomalyType type,
    String details,
    Instant detectedAt,
    Instant resolvedAt
) {}
