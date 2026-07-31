package com.parkflow.reservation.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

public record ReservationRequest(
        @NotNull UUID spotId,
        @NotNull @FutureOrPresent Instant from,
        @NotNull @Future Instant to,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}\\d{4}[A-Z]{2}$", message = "Invalid license plate format") String licensePlate
) {
}
