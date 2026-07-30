package com.parkflow.shared.domain.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCommand(
        UUID reservationId,
        BigDecimal amount,
        UUID userId
) {}
