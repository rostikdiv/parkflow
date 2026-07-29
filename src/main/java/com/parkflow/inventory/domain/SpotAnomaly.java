package com.parkflow.inventory.domain;

import com.parkflow.inventory.domain.enums.SpotAnomalyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spot_anomaly")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotAnomalyType type;

    @Column
    private String details;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Setter
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public SpotAnomaly(Spot spot, SpotAnomalyType type, String details, Instant detectedAt) {
        this.spot = spot;
        this.type = type;
        this.details = details;
        this.detectedAt = detectedAt;
    }
    
    public void resolve() {
        this.resolvedAt = Instant.now();
    }
}
