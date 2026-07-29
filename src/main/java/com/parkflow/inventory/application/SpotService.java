package com.parkflow.inventory.application;

import com.parkflow.inventory.api.dto.SpotResponse;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.infra.SpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public SpotService(SpotRepository spotRepository) {
        this.spotRepository = spotRepository;
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
     * Converts a Spot entity to a DTO, preventing the internal entity state
     * (and its JPA annotations/relationships) from leaking out to the REST controller.
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
                spot.getLayoutY()
        );
    }
}
