package com.parkflow.sensor.application;

import com.parkflow.inventory.domain.enums.PhysicalStatus;
import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.sensor.api.dto.SensorEventDto;
import com.parkflow.shared.infra.RabbitMQConfig;
import com.parkflow.sensor.infra.SensorEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "CI", matches = "true")
@ActiveProfiles("test")
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "CI", matches = "true")
@org.springframework.context.annotation.Import(com.parkflow.TestcontainersConfiguration.class)
class SensorEventConsumerTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SensorEventRepository sensorEventRepository;

    @Autowired
    private SpotRepository spotRepository;

    @BeforeEach
    void setup() {
        sensorEventRepository.deleteAll();
    }

    @Test
    void testConsumerProcessesEventAndUpdatesSpot() {
        // Use an existing spot from seed data
        var spotId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var lotId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        var eventId = UUID.randomUUID().toString();

        var initialSpot = spotRepository.findById(spotId).orElseThrow();
        assertThat(initialSpot.getPhysicalStatus()).isEqualTo(PhysicalStatus.UNKNOWN);

        var event = new SensorEventDto(
                eventId,
                spotId,
                lotId,
                PhysicalStatus.OCCUPIED,
                Instant.now()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SENSOR, "sensor." + lotId, event);

        // Wait for the consumer to process the message asynchronously
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var savedEventOpt = sensorEventRepository.findAll().stream()
                    .filter(e -> e.getExternalEventId().equals(eventId))
                    .findFirst();
            
            assertThat(savedEventOpt).isPresent();
            assertThat(savedEventOpt.get().getStatus()).isEqualTo(PhysicalStatus.OCCUPIED);

            var updatedSpot = spotRepository.findById(spotId).orElseThrow();
            assertThat(updatedSpot.getPhysicalStatus()).isEqualTo(PhysicalStatus.OCCUPIED);
            assertThat(updatedSpot.getLastSensorUpdate()).isNotNull();
        });

        // Test Idempotency: Send the exact same event again
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SENSOR, "sensor." + lotId, event);

        // Wait a bit to ensure it would have been processed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify no duplicate records were created
        var eventsCount = sensorEventRepository.findAll().stream()
                .filter(e -> e.getExternalEventId().equals(eventId))
                .count();
        
        assertThat(eventsCount).isEqualTo(1);
    }
}
