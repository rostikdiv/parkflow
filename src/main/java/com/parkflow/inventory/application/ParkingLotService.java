package com.parkflow.inventory.application;

import com.parkflow.inventory.api.dto.GeoJsonFeature;
import com.parkflow.inventory.api.dto.GeoJsonFeatureCollection;
import com.parkflow.inventory.api.dto.GeoJsonGeometry;
import com.parkflow.inventory.api.dto.ParkingLotResponse;
import com.parkflow.inventory.domain.ParkingLot;
import com.parkflow.inventory.infra.ParkingLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application service for managing Parking Lots.
 * 
 * Handles business logic and transactions for the ParkingLot aggregate.
 * It is responsible for mapping internal domain entities to API DTOs.
 */
@Service
@Transactional(readOnly = true)
public class ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;

    public ParkingLotService(ParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
    }

    /**
     * Retrieves a list of active parking lots within a specific bounding box.
     * 
     * @param minLng Minimum longitude (west)
     * @param minLat Minimum latitude (south)
     * @param maxLng Maximum longitude (east)
     * @param maxLat Maximum latitude (north)
     * @return List of parking lots mapped to response DTOs
     */
    public List<ParkingLotResponse> findInBoundingBox(Double minLng, Double minLat, Double maxLng, Double maxLat) {
        List<ParkingLot> lots = parkingLotRepository.findActiveInBbox(minLat, minLng, maxLat, maxLng);
        return lots.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves all parking lots.
     */
    public List<ParkingLotResponse> getAll() {
        return parkingLotRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Fetches a single parking lot by its unique identifier.
     * 
     * @param id the UUID of the parking lot
     * @return the parking lot DTO
     * @throws IllegalArgumentException if the lot is not found
     */
    public ParkingLotResponse getById(UUID id) {
        return parkingLotRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Parking lot not found")); // In a real app, use a custom NotFoundException
    }

    /**
     * Retrieves all parking lots and formats them as a GeoJSON FeatureCollection.
     * This format is natively supported by modern mapping libraries (e.g. Mapbox GL JS)
     * and avoids custom parsing logic on the client side.
     * 
     * Lots without coordinates are filtered out to prevent mapping errors.
     * 
     * @return a GeoJSON FeatureCollection representing all mappable parking lots
     */
    public GeoJsonFeatureCollection getGeoJson() {
        List<ParkingLot> allLots = parkingLotRepository.findAll();
        List<GeoJsonFeature> features = allLots.stream()
                .filter(lot -> lot.getLatitude() != null && lot.getLongitude() != null)
                .map(lot -> GeoJsonFeature.of(
                        GeoJsonGeometry.point(lot.getLongitude(), lot.getLatitude()),
                        Map.of(
                                "id", lot.getId(),
                                "name", lot.getName(),
                                "hourlyRate", lot.getHourlyRate(),
                                "status", lot.getStatus(),
                                "type", lot.getType(),
                                "address", lot.getAddress()
                        )
                ))
                .toList();
        return GeoJsonFeatureCollection.of(features);
    }

    private ParkingLotResponse mapToResponse(ParkingLot lot) {
        return new ParkingLotResponse(
                lot.getId(),
                lot.getName(),
                lot.getAddress(),
                lot.getLatitude(),
                lot.getLongitude(),
                lot.getType(),
                lot.getHourlyRate(),
                lot.getOpensAt(),
                lot.getClosesAt(),
                lot.getTimeZoneAsZoneId().getId(),
                lot.getStatus()
        );
    }
}
