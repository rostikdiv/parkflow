package com.parkflow.sensor.application;

import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.domain.enums.PhysicalStatus;
import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.sensor.api.dto.SensorEventDto;
import com.parkflow.sensor.infra.SensorEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@Profile("local")
public class SensorSimulator {

    private final SpotRepository spotRepository;
    private final SensorEventPublisher publisher;
    private final Random random = new Random();
    private List<Spot> cache;

    public SensorSimulator(SpotRepository spotRepository, SensorEventPublisher publisher) {
        this.spotRepository = spotRepository;
        this.publisher = publisher;
    }

    // Run every 3 seconds to simulate live map activity
    @Scheduled(fixedRate = 3000)
    public void simulateSensorEvents() {
        if (cache == null || cache.isEmpty()) {
            cache = spotRepository.findAll();
        }
        if (cache.isEmpty()) return;

        // Pick a random spot
        Spot spot = cache.get(random.nextInt(cache.size()));
        
        // Random status
        PhysicalStatus status = random.nextBoolean() ? 
            PhysicalStatus.OCCUPIED : 
            PhysicalStatus.FREE;

        SensorEventDto event = new SensorEventDto(
            UUID.randomUUID().toString(),
            spot.getId(),
            spot.getParkingLot().getId(),
            status,
            Instant.now()
        );

        publisher.publishEvent(event);
    }
}
