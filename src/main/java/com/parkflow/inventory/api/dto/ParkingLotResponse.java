package com.parkflow.inventory.api.dto;

import com.parkflow.inventory.domain.enums.ParkingLotStatus;
import com.parkflow.inventory.domain.enums.ParkingLotType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record ParkingLotResponse(
        UUID id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        ParkingLotType type,
        BigDecimal hourlyRate,
        LocalTime opensAt,
        LocalTime closesAt,
        String timeZone,
        ParkingLotStatus status
) {
}
