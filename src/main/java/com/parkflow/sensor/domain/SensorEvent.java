package com.parkflow.sensor.domain;

import com.parkflow.inventory.domain.enums.PhysicalStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only entity representing an event emitted by a physical parking sensor.
 * We store every event to allow for historical analysis, reconciliation, and audit.
 * 
 * The `externalEventId` ensures idempotency: if the sensor emulator or RabbitMQ
 * delivers the same event twice, the unique constraint at the DB level will
 * prevent duplicate processing.
 * 
 * Reference: parkflow_final_plan.md §4.1
 */
@Entity
@Table(name = "sensor_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SensorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_event_id", nullable = false, unique = true)
    private String externalEventId;

    @Column(name = "spot_id", nullable = false)
    private UUID spotId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhysicalStatus status;

    @Column(name = "sensor_timestamp", nullable = false)
    private Instant sensorTimestamp;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public SensorEvent(String externalEventId, UUID spotId, PhysicalStatus status, Instant sensorTimestamp, Instant receivedAt, String rawPayload) {
        this.externalEventId = externalEventId;
        this.spotId = spotId;
        this.status = status;
        this.sensorTimestamp = sensorTimestamp;
        this.receivedAt = receivedAt;
        this.processedAt = Instant.now();
        this.rawPayload = rawPayload;
    }
}
