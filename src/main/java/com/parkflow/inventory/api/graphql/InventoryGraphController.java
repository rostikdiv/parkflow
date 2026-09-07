package com.parkflow.inventory.api.graphql;

import com.parkflow.inventory.api.dto.ParkingLotResponse;
import com.parkflow.inventory.api.dto.SpotAvailability;
import com.parkflow.inventory.application.ParkingLotService;
import com.parkflow.inventory.application.SpotService;
import com.parkflow.shared.domain.events.SpotStatusEvent;
import org.springframework.context.event.EventListener;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class InventoryGraphController {

    private final ParkingLotService parkingLotService;
    private final SpotService spotService;
    private final Sinks.Many<SpotStatusEvent> spotStatusSink;

    public InventoryGraphController(ParkingLotService parkingLotService, SpotService spotService) {
        this.parkingLotService = parkingLotService;
        this.spotService = spotService;
        this.spotStatusSink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @QueryMapping
    public List<ParkingLotResponse> parkingLots(@Argument Map<String, Double> bbox) {
        if (bbox == null) {
            return parkingLotService.getAll();
        }
        return parkingLotService.findInBoundingBox(
                bbox.get("minLng"),
                bbox.get("minLat"),
                bbox.get("maxLng"),
                bbox.get("maxLat")
        );
    }

    @QueryMapping
    public ParkingLotResponse parkingLot(@Argument UUID id) {
        return parkingLotService.getById(id);
    }

    @QueryMapping
    public List<SpotAvailability> availability(@Argument UUID lotId, @Argument String from, @Argument String to) {
        return spotService.getAvailability(lotId, Instant.parse(from), Instant.parse(to));
    }

    /**
     * Batch mapping for ParkingLot.spots.
     * Resolves spots for all parking lots in the GraphQL query using a single SQL query,
     * completely eliminating the classic GraphQL N+1 problem.
     */
    @org.springframework.graphql.data.method.annotation.BatchMapping(typeName = "ParkingLot", field = "spots")
    public Map<ParkingLotResponse, List<com.parkflow.inventory.api.dto.SpotResponse>> spots(List<ParkingLotResponse> lots) {
        List<UUID> lotIds = lots.stream().map(ParkingLotResponse::id).toList();
        Map<UUID, List<com.parkflow.inventory.api.dto.SpotResponse>> spotsByLot = spotService.getByParkingLotIds(lotIds);
        Map<ParkingLotResponse, List<com.parkflow.inventory.api.dto.SpotResponse>> result = new java.util.HashMap<>();
        for (ParkingLotResponse lot : lots) {
            result.put(lot, spotsByLot.getOrDefault(lot.id(), List.of()));
        }
        return result;
    }

    @EventListener
    public void onSpotStatusEvent(SpotStatusEvent event) {
        spotStatusSink.tryEmitNext(event);
    }

    @SubscriptionMapping
    public Flux<SpotStatusEvent> spotStatusChanged(@Argument UUID lotId) {
        return spotStatusSink.asFlux()
            .filter(event -> event.lotId().equals(lotId));
    }
}
