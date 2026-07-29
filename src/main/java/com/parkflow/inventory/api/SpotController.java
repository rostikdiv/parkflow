package com.parkflow.inventory.api;

import com.parkflow.inventory.api.dto.SpotResponse;
import com.parkflow.inventory.application.SpotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API for Spot (Parking space) operations.
 * 
 * Provides endpoints for retrieving individual parking spot details,
 * which are usually fetched when a user clicks on a specific spot
 * on the internal parking lot map.
 */
@RestController
@RequestMapping("/api/v1/spots")
public class SpotController {

    private final SpotService spotService;

    public SpotController(SpotService spotService) {
        this.spotService = spotService;
    }

    /**
     * Retrieves the details of a specific parking spot by its ID.
     */
    @GetMapping("/{id}")
    public SpotResponse getSpotById(@PathVariable UUID id) {
        return spotService.getById(id);
    }
}
