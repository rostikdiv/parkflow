package com.parkflow.inventory.domain;

import com.parkflow.inventory.domain.enums.ParkingLotStatus;
import com.parkflow.inventory.domain.enums.ParkingLotType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "parking_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Required by JPA, protected to enforce usage of the all-args constructor
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    // Using primitive wrappers (Double) to allow nulls if a lot hasn't been geocoded yet
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingLotType type;

    @Column(name = "hourly_rate", nullable = false)
    private BigDecimal hourlyRate;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    // Storing TimeZone as a String identifier rather than offset to account for DST changes
    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    // See plan §4.1: Soft delete implementation via status transitions rather than Hibernate's @SQLDelete
    // to keep domain logic explicit and visible in the application layer.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingLotStatus status = ParkingLotStatus.ACTIVE;

    @OneToMany(mappedBy = "parkingLot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Spot> spots = new ArrayList<>();

    public ParkingLot(String name, String address, Double latitude, Double longitude, ParkingLotType type, BigDecimal hourlyRate, LocalTime opensAt, LocalTime closesAt, ZoneId timeZone) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.hourlyRate = hourlyRate;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.timeZone = timeZone.getId();
    }

    public void addSpot(Spot spot) {
        spots.add(spot);
        spot.setParkingLot(this);
    }

    public ZoneId getTimeZoneAsZoneId() {
        return ZoneId.of(timeZone);
    }

    public void close() {
        this.status = ParkingLotStatus.CLOSED;
    }
}
