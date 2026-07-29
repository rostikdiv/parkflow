package com.parkflow.inventory.infra;

import com.parkflow.inventory.domain.Spot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpotRepository extends JpaRepository<Spot, UUID> {
}
