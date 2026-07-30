package com.parkflow.sensor.infra;

import com.parkflow.sensor.api.dto.SensorEventDto;
import com.parkflow.shared.infra.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publisher for sending sensor events to RabbitMQ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a single sensor event to the topic exchange.
     * The routing key is derived as 'sensor.{lotId}'.
     * 
     * @param event The DTO containing the physical status update from the sensor.
     */
    public void publishEvent(SensorEventDto event) {
        String routingKey = "sensor." + event.lotId();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SENSOR, routingKey, event);
        log.debug("Published sensor event {} for spot {} to routing key {}", event.externalEventId(), event.spotId(), routingKey);
    }
}
