package com.parkflow.reservation.application;

import com.parkflow.inventory.application.AnomalyService;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.domain.SpotAnomaly;
import com.parkflow.inventory.domain.enums.PhysicalStatus;
import com.parkflow.inventory.domain.enums.SpotAnomalyType;
import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.reservation.domain.Reservation;
import com.parkflow.reservation.infra.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);
    
    // Grace period for NO-SHOW in minutes
    private static final int GRACE_PERIOD_MINUTES = 15;
    // Timeout for missing sensor updates in minutes
    private static final int SENSOR_TIMEOUT_MINUTES = 10;

    private final SpotRepository spotRepository;
    private final ReservationRepository reservationRepository;
    private final AnomalyService anomalyService;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    public ReconciliationService(SpotRepository spotRepository,
                                 ReservationRepository reservationRepository,
                                 AnomalyService anomalyService,
                                 org.springframework.transaction.support.TransactionTemplate transactionTemplate) {
        this.spotRepository = spotRepository;
        this.reservationRepository = reservationRepository;
        this.anomalyService = anomalyService;
        this.transactionTemplate = transactionTemplate;
    }

    // See plan §10
    @Scheduled(fixedDelay = 120000) // Every 2 minutes
    public void runReconciliation() {
        log.info("Starting reconciliation job");
        Instant now = Instant.now();
        List<Spot> spots = spotRepository.findAll();

        for (Spot spot : spots) {
            try {
                transactionTemplate.execute(status -> {
                    Spot freshSpot = spotRepository.findById(spot.getId()).orElse(null);
                    if (freshSpot != null) {
                        checkSpot(freshSpot, now);
                    }
                    return null;
                });
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                log.debug("Concurrent update for spot {}. Will retry next cycle.", spot.getId());
            } catch (Exception e) {
                log.error("Error processing spot {}", spot.getId(), e);
            }
        }
    }

    private void checkSpot(Spot spot, Instant now) {
        Optional<Reservation> activeReservationOpt = reservationRepository.findActiveReservationForSpot(spot.getId(), now);
        
        SpotAnomalyType currentAnomalyType = null;
        String currentAnomalyDetails = null;

        // 1. SENSOR_SILENT
        if (spot.getLastSensorUpdate() == null || spot.getLastSensorUpdate().isBefore(now.minus(SENSOR_TIMEOUT_MINUTES, ChronoUnit.MINUTES))) {
            currentAnomalyType = SpotAnomalyType.SENSOR_SILENT;
            currentAnomalyDetails = "No updates for more than 10 mins";
            
            if (spot.getPhysicalStatus() != PhysicalStatus.UNKNOWN) {
                // See plan §6 and §10: graceful degradation, mark as UNKNOWN
                spot.updatePhysicalStatus(PhysicalStatus.UNKNOWN, spot.getLastSensorUpdate());
                spotRepository.save(spot);
            }
        } 
        // 2. OCCUPIED_WITHOUT_RESERVATION
        else if (spot.getPhysicalStatus() == PhysicalStatus.OCCUPIED && activeReservationOpt.isEmpty()) {
            currentAnomalyType = SpotAnomalyType.OCCUPIED_WITHOUT_RESERVATION;
            currentAnomalyDetails = "Sensor is OCCUPIED but no active reservation";
        }
        // 3. RESERVED_BUT_EMPTY_TOO_LONG
        else if (spot.getPhysicalStatus() == PhysicalStatus.FREE && activeReservationOpt.isPresent()) {
            Reservation res = activeReservationOpt.get();
            // If the reservation started more than GRACE_PERIOD_MINUTES ago and spot is still free
            if (res.getStartTime().isBefore(now.minus(GRACE_PERIOD_MINUTES, ChronoUnit.MINUTES))) {
                currentAnomalyType = SpotAnomalyType.RESERVED_BUT_EMPTY_TOO_LONG;
                currentAnomalyDetails = "Reservation active for > 15 mins but spot is FREE";
            }
        }

        // Handle Deduplication and Auto-Resolution
        List<SpotAnomaly> unresolvedAnomalies = anomalyService.getUnresolvedAnomaliesForSpot(spot.getId());
        boolean alreadyExists = false;

        for (SpotAnomaly anomaly : unresolvedAnomalies) {
            if (currentAnomalyType != null && anomaly.getType() == currentAnomalyType) {
                // The anomaly is still valid, no need to create a duplicate
                alreadyExists = true;
            } else {
                // The anomaly condition is no longer met, auto-resolve it
                anomaly.resolve();
                anomalyService.reportAnomaly(anomaly);
                log.info("Auto-resolved anomaly {} for spot {}", anomaly.getType(), spot.getId());
            }
        }

        // If a new anomaly condition is met and it doesn't already exist, create it
        if (currentAnomalyType != null && !alreadyExists) {
            SpotAnomaly newAnomaly = new SpotAnomaly(spot, currentAnomalyType, currentAnomalyDetails, now);
            anomalyService.reportAnomaly(newAnomaly);
            log.warn("New anomaly {} detected for spot {}", currentAnomalyType, spot.getId());
        }
    }
}
