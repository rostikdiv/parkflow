package com.parkflow.inventory.application;

import com.parkflow.inventory.api.dto.SpotAnomalyResponse;
import com.parkflow.inventory.domain.SpotAnomaly;
import com.parkflow.inventory.infra.SpotAnomalyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AnomalyService {

    private final SpotAnomalyRepository repository;

    public AnomalyService(SpotAnomalyRepository repository) {
        this.repository = repository;
    }

    public List<SpotAnomalyResponse> getAnomalies(boolean resolved) {
        List<SpotAnomaly> anomalies = resolved ? repository.findAll() : repository.findByResolvedAtIsNull();
        return anomalies.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<SpotAnomaly> getUnresolvedAnomaliesForSpot(UUID spotId) {
        return repository.findBySpotIdAndResolvedAtIsNull(spotId);
    }

    @Transactional
    public void resolveAnomaly(UUID anomalyId) {
        SpotAnomaly anomaly = repository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found"));
        anomaly.resolve();
    }

    @Transactional
    public void reportAnomaly(SpotAnomaly anomaly) {
        repository.save(anomaly);
    }

    private SpotAnomalyResponse mapToResponse(SpotAnomaly anomaly) {
        return new SpotAnomalyResponse(
                anomaly.getId(),
                anomaly.getSpot().getId(),
                anomaly.getType(),
                anomaly.getDetails(),
                anomaly.getDetectedAt(),
                anomaly.getResolvedAt()
        );
    }
}
