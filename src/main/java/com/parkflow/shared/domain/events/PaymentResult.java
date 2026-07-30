package com.parkflow.shared.domain.events;

import java.util.UUID;

public record PaymentResult(
        UUID reservationId,
        boolean success,
        String externalRef,
        String errorReason
) {}
