package com.parkflow.inventory.infra;

import com.parkflow.inventory.domain.Spot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpotRepository extends JpaRepository<Spot, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"parkingLot"})
    List<Spot> findByParkingLotId(UUID parkingLotId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"parkingLot"})
    List<Spot> findByParkingLotIdIn(java.util.Collection<UUID> parkingLotIds);
}
