package com.parkflow.inventory.domain;

import com.parkflow.inventory.domain.enums.PhysicalStatus;
import com.parkflow.inventory.domain.enums.SpotType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Required by JPA
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FetchType.LAZY is standard practice for ToOne relationships to prevent N+1 queries.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_lot_id", nullable = false)
    private ParkingLot parkingLot;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotType type;

    // Physical state is updated asynchronously by the sensor-ingestion module,
    // so we decouple it from the reservation status.
    @Enumerated(EnumType.STRING)
    @Column(name = "physical_status", nullable = false)
    private PhysicalStatus physicalStatus = PhysicalStatus.UNKNOWN;

    @Column(name = "last_sensor_update")
    private Instant lastSensorUpdate;

    // See plan §4.3, Level 2 (Baseline)
    // Optimistic locking serves as the foundational concurrency control before we implement
    // the advanced PostgreSQL exclusion constraint in M2.
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "layout_x")
    private Double layoutX;

    @Column(name = "layout_y")
    private Double layoutY;

    public Spot(String code, SpotType type, Double layoutX, Double layoutY) {
        this.code = code;
        this.type = type;
        this.layoutX = layoutX;
        this.layoutY = layoutY;
    }

    // Package-private setter allows ParkingLot to manage the bidirectional relationship
    // while hiding this mutation from outer layers.
    void setParkingLot(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    public void updatePhysicalStatus(PhysicalStatus newStatus, Instant timestamp) {
        this.physicalStatus = newStatus;
        this.lastSensorUpdate = timestamp;
    }
}
