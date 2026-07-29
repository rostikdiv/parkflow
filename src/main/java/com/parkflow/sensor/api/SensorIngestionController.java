package com.parkflow.sensor.api;

import com.parkflow.sensor.api.dto.SensorEventBatchRequest;
import com.parkflow.sensor.infra.SensorEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal REST API for ingesting batches of events from physical parking sensors.
 * This is meant to be called exclusively by the sensor emulator (or the physical gateways in real life).
 * It acts purely as a fast ingest layer pushing data to RabbitMQ for asynchronous processing.
 */
@RestController
@RequestMapping("/api/internal/v1/sensor-events")
@RequiredArgsConstructor
@Slf4j
public class SensorIngestionController {

    private final SensorEventPublisher publisher;

    @PostMapping
    public ResponseEntity<Void> ingestBatch(@RequestBody @Valid SensorEventBatchRequest request) {
        log.debug("Received batch of {} sensor events", request.events().size());
        
        for (var event : request.events()) {
            publisher.publishEvent(event);
        }
        
        return ResponseEntity.accepted().build();
    }
}
