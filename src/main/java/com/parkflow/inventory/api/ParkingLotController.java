package com.parkflow.inventory.api;

import com.parkflow.inventory.api.dto.GeoJsonFeatureCollection;
import com.parkflow.inventory.api.dto.ParkingLotResponse;
import com.parkflow.inventory.api.dto.SpotResponse;
import com.parkflow.inventory.application.ParkingLotService;
import com.parkflow.inventory.application.SpotService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for Parking Lot operations.
 * 
 * Exposes endpoints for the map layer (GeoJSON) and local search (BBox).
 * This controller delegates to the application service and only handles
 * HTTP-level concerns, returning pure DTOs instead of domain entities
 * to prevent leaking the internal domain model.
 * 
 * See plan §7 (REST / GraphQL CQRS-Lite API definition).
 */
@RestController
@RequestMapping("/api/v1/parking-lots")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;
    private final SpotService spotService;

    public ParkingLotController(ParkingLotService parkingLotService, SpotService spotService) {
        this.parkingLotService = parkingLotService;
        this.spotService = spotService;
    }

    /**
     * Retrieves parking lots falling within the specified bounding box.
     * We use a bounding box (minLng, minLat, maxLng, maxLat) instead of radius
     * search
     * because it directly maps to the visible area of standard map libraries
     * (Mapbox/Google Maps),
     * avoiding unnecessary frontend coordinate math.
     * 
     * @param bbox Comma-separated coordinates: minLng,minLat,maxLng,maxLat
     */
    @GetMapping
    public List<ParkingLotResponse> getLotsInBoundingBox(
            @RequestParam("bbox") String bbox // minLng,minLat,maxLng,maxLat
    ) {
        String[] parts = bbox.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("bbox must contain exactly 4 coordinates");
        }
        double minLng = Double.parseDouble(parts[0]);
        double minLat = Double.parseDouble(parts[1]);
        double maxLng = Double.parseDouble(parts[2]);
        double maxLat = Double.parseDouble(parts[3]);

        return parkingLotService.findInBoundingBox(minLng, minLat, maxLng, maxLat);
    }

    /**
     * Fetches details of a specific parking lot.
     */
    @GetMapping("/{id}")
    public ParkingLotResponse getLotById(@PathVariable UUID id) {
        return parkingLotService.getById(id);
    }

    /**
     * Fetches all spots for a specific parking lot.
     */
    @GetMapping("/{id}/spots")
    public List<SpotResponse> getSpotsByLotId(@PathVariable UUID id) {
        return spotService.getByParkingLotId(id);
    }

    /**
     * Exposes the entire parking lot dataset as a standard GeoJSON
     * FeatureCollection.
     * This allows map libraries to ingest the data natively as a vector source
     * without custom mapping logic on the client side.
     */
    @GetMapping("/geojson")
    public GeoJsonFeatureCollection getLotsAsGeoJson() {
        return parkingLotService.getGeoJson();
    }
}
