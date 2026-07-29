package com.parkflow.inventory.infra;

import com.parkflow.inventory.domain.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, UUID> {

    @Query("SELECT pl FROM ParkingLot pl WHERE " +
           "pl.latitude >= :minLat AND pl.latitude <= :maxLat AND " +
           "pl.longitude >= :minLng AND pl.longitude <= :maxLng AND " +
           "pl.status = 'ACTIVE'")
    List<ParkingLot> findActiveInBbox(@Param("minLat") Double minLat,
                                      @Param("minLng") Double minLng,
                                      @Param("maxLat") Double maxLat,
                                      @Param("maxLng") Double maxLng);
}
