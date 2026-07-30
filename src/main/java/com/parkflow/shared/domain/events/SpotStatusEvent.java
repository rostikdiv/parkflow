package com.parkflow.shared.domain.events;

import java.util.UUID;

public record SpotStatusEvent(
    UUID spotId,
    UUID lotId,
    String status,
    String at
) {}
