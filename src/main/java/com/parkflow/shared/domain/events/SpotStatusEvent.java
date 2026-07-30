package com.parkflow.shared.domain.events;

import java.util.UUID;

public record SpotStatusEvent(
    UUID spotId,
    String status,
    String at
) {}
