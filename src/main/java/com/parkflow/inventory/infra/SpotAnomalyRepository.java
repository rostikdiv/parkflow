package com.parkflow.inventory.infra;

import com.parkflow.inventory.domain.SpotAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpotAnomalyRepository extends JpaRepository<SpotAnomaly, UUID> {
    List<SpotAnomaly> findByResolvedAtIsNull();
    List<SpotAnomaly> findBySpotIdAndResolvedAtIsNull(UUID spotId);
}
