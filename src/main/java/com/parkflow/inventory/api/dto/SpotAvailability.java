package com.parkflow.inventory.api.dto;

import java.io.Serializable;
import java.util.UUID;

public record SpotAvailability(
    UUID spotId,
    String code,
    String type,
    boolean isAvailable,
    Double layoutX,
    Double layoutY
) implements Serializable {}
