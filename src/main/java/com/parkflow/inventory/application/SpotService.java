package com.parkflow.inventory.application;

import com.parkflow.inventory.api.dto.SpotResponse;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.reservation.domain.Reservation;
import com.parkflow.inventory.infra.SpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import com.parkflow.inventory.api.dto.SpotAvailability;
import com.parkflow.inventory.domain.enums.PhysicalStatus;
import com.parkflow.reservation.infra.ReservationRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application service for managing Parking Spots.
 * 
 * Maps internal Spot entities to response DTOs for the API layer.
 */
@Service
@Transactional(readOnly = true)
public class SpotService {

    private final SpotRepository spotRepository;
    private final ReservationRepository reservationRepository;

    public SpotService(SpotRepository spotRepository, ReservationRepository reservationRepository) {
        this.spotRepository = spotRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Retrieves a single spot by its ID.
     * 
     * @param id the UUID of the spot
     * @return the mapped SpotResponse DTO
     * @throws IllegalArgumentException if the spot does not exist
     */
    public SpotResponse getById(UUID id) {
        return spotRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Spot not found"));
    }

    /**
     * Retrieves all spots for a specific parking lot.
     */
    public List<SpotResponse> getByParkingLotId(UUID parkingLotId) {
        return spotRepository.findByParkingLotId(parkingLotId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves availability of all spots in a parking lot for a given time range.
     * Cached in Redis.
     */
    @Cacheable(value = "availability", key = "#parkingLotId.toString() + '_' + #from.toString() + '_' + #to.toString()")
    public List<SpotAvailability> getAvailability(UUID parkingLotId, Instant from, Instant to) {
        List<Spot> spots = spotRepository.findByParkingLotId(parkingLotId);
        List<Reservation> overlappingReservations = reservationRepository.findOverlappingReservations(parkingLotId, from, to);
        
        Instant now = Instant.now();
        boolean isCurrentRange = !from.isAfter(now) && !to.isBefore(now);
        
        return spots.stream()
            .map(spot -> {
                Reservation overlapping = overlappingReservations.stream()
                    .filter(r -> r.getSpot().getId().equals(spot.getId()))
                    .findFirst()
                    .orElse(null);
                    
                boolean physicalOccupied = isCurrentRange && spot.getPhysicalStatus() == PhysicalStatus.OCCUPIED;
                boolean isAvailable = (overlapping == null) && !physicalOccupied;
                boolean isAnomaly = physicalOccupied && (overlapping == null);
                
                return new SpotAvailability(
                    spot.getId(),
                    spot.getCode(),
                    spot.getType().name(),
                    isAvailable,
                    spot.getLayoutX(),
                    spot.getLayoutY(),
                    overlapping != null ? overlapping.getEndTime() : null,
                    isAnomaly
                );
            })
            .toList();
    }

    /**
     * Converts a Spot entity to a DTO, preventing the internal entity state
     * (and its JPA annotations/relationships) from leaking out to the REST
     * controller.
     */
    private SpotResponse mapToResponse(Spot spot) {
        return new SpotResponse(
                spot.getId(),
                spot.getParkingLot().getId(),
                spot.getCode(),
                spot.getType(),
                spot.getPhysicalStatus(),
                spot.getLastSensorUpdate(),
                spot.getLayoutX(),
                spot.getLayoutY());
    }
}
