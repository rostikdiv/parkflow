package com.parkflow.sensor.infra;

import com.parkflow.sensor.domain.SensorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SensorEventRepository extends JpaRepository<SensorEvent, UUID> {
    boolean existsByExternalEventId(String externalEventId);
}
