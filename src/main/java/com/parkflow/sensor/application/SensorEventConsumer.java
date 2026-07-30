package com.parkflow.sensor.application;

import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.sensor.api.dto.SensorEventDto;
import com.parkflow.sensor.domain.SensorEvent;
import com.parkflow.shared.infra.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkflow.sensor.infra.SensorEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Consumer for processing sensor events from RabbitMQ.
 * This class uses the Virtual Thread executor defined in {@link RabbitMQConfig}.
 * It updates the physical status of the corresponding spot and records the event in the DB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorEventConsumer {

    private final SensorEventRepository sensorEventRepository;
    private final SpotRepository spotRepository;
    private final ObjectMapper objectMapper;

    /**
     * Listens to the `q.sensor.events` queue.
     * We use a robust two-layer idempotency approach:
     * 1. Soft check (existsByExternalEventId) - avoids unnecessary transactions.
     * 2. Hard check - DB unique constraint throws DataIntegrityViolationException on race conditions.
     * 
     * @param event The event payload deserialized from JSON.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_SENSOR_EVENTS)
    @Transactional
    public void consume(SensorEventDto event) {
        log.debug("Received sensor event {} for spot {}", event.externalEventId(), event.spotId());

        if (sensorEventRepository.existsByExternalEventId(event.externalEventId())) {
            log.debug("Event {} already processed, skipping", event.externalEventId());
            return;
        }

        try {
            var spot = spotRepository.findById(event.spotId()).orElse(null);
            
            if (spot == null) {
                log.warn("Spot {} not found for event {}", event.spotId(), event.externalEventId());
                return;
            }

            // Only update if this event is newer than the last recorded update
            if (spot.getLastSensorUpdate() == null || event.timestamp().isAfter(spot.getLastSensorUpdate())) {
                spot.updatePhysicalStatus(event.status(), event.timestamp());
                spotRepository.save(spot);
            }

            String rawPayload;
            try {
                rawPayload = objectMapper.writeValueAsString(event);
            } catch (Exception ex) {
                rawPayload = "{}";
                log.warn("Failed to serialize sensor event to JSON", ex);
            }

            var sensorEvent = new SensorEvent(
                    event.externalEventId(),
                    event.spotId(),
                    event.status(),
                    event.timestamp(),
                    Instant.now(), // receivedAt (technically processed at right now)
                    rawPayload
            );
            
            sensorEventRepository.save(sensorEvent);
            log.debug("Successfully processed event {}", event.externalEventId());
            
        } catch (DataIntegrityViolationException e) {
            // This happens if another thread/node processed the same event concurrently.
            // We can safely ignore it since the event is already handled.
            log.debug("Concurrent duplicate event {} detected and ignored via DB constraint", event.externalEventId());
        }
    }
}
