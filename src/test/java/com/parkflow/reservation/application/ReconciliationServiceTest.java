package com.parkflow.reservation.application;

import com.parkflow.inventory.application.AnomalyService;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.domain.SpotAnomaly;
import com.parkflow.inventory.domain.enums.PhysicalStatus;
import com.parkflow.inventory.domain.enums.SpotAnomalyType;
import com.parkflow.inventory.infra.SpotRepository;
import com.parkflow.reservation.domain.Reservation;
import com.parkflow.reservation.infra.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private SpotRepository spotRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private AnomalyService anomalyService;
    @Mock
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new org.springframework.transaction.support.SimpleTransactionStatus());
        });
        reconciliationService = new ReconciliationService(spotRepository, reservationRepository, anomalyService, transactionTemplate);
    }

    @Test
    void shouldReportSensorSilent() {
        Spot spot = mock(Spot.class);
        when(spot.getId()).thenReturn(UUID.randomUUID());
        when(spot.getLastSensorUpdate()).thenReturn(Instant.now().minus(11, ChronoUnit.MINUTES));
        when(spot.getPhysicalStatus()).thenReturn(PhysicalStatus.FREE);
        
        when(spotRepository.findAll()).thenReturn(List.of(spot));
        when(spotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
        when(reservationRepository.findActiveReservationForSpot(eq(spot.getId()), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(anomalyService.getUnresolvedAnomaliesForSpot(spot.getId())).thenReturn(List.of());

        reconciliationService.runReconciliation();

        verify(spot).updatePhysicalStatus(eq(PhysicalStatus.UNKNOWN), any(Instant.class));
        verify(spotRepository).save(spot);
        
        ArgumentCaptor<SpotAnomaly> captor = ArgumentCaptor.forClass(SpotAnomaly.class);
        verify(anomalyService).reportAnomaly(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(SpotAnomalyType.SENSOR_SILENT);
    }

    @Test
    void shouldReportOccupiedWithoutReservation() {
        Spot spot = mock(Spot.class);
        when(spot.getId()).thenReturn(UUID.randomUUID());
        when(spot.getLastSensorUpdate()).thenReturn(Instant.now());
        when(spot.getPhysicalStatus()).thenReturn(PhysicalStatus.OCCUPIED);
        
        when(spotRepository.findAll()).thenReturn(List.of(spot));
        when(spotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
        when(reservationRepository.findActiveReservationForSpot(eq(spot.getId()), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(anomalyService.getUnresolvedAnomaliesForSpot(spot.getId())).thenReturn(List.of());

        reconciliationService.runReconciliation();

        ArgumentCaptor<SpotAnomaly> captor = ArgumentCaptor.forClass(SpotAnomaly.class);
        verify(anomalyService).reportAnomaly(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(SpotAnomalyType.OCCUPIED_WITHOUT_RESERVATION);
    }

    @Test
    void shouldReportReservedButEmptyTooLong() {
        Spot spot = mock(Spot.class);
        when(spot.getId()).thenReturn(UUID.randomUUID());
        when(spot.getLastSensorUpdate()).thenReturn(Instant.now());
        when(spot.getPhysicalStatus()).thenReturn(PhysicalStatus.FREE);
        
        Reservation res = mock(Reservation.class);
        when(res.getStartTime()).thenReturn(Instant.now().minus(16, ChronoUnit.MINUTES));
        
        when(spotRepository.findAll()).thenReturn(List.of(spot));
        when(spotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
        when(reservationRepository.findActiveReservationForSpot(eq(spot.getId()), any(Instant.class)))
                .thenReturn(Optional.of(res));
        when(anomalyService.getUnresolvedAnomaliesForSpot(spot.getId())).thenReturn(List.of());

        reconciliationService.runReconciliation();

        ArgumentCaptor<SpotAnomaly> captor = ArgumentCaptor.forClass(SpotAnomaly.class);
        verify(anomalyService).reportAnomaly(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(SpotAnomalyType.RESERVED_BUT_EMPTY_TOO_LONG);
    }

    @Test
    void shouldNotCreateDuplicateAnomalyIfAlreadyExists() {
        Spot spot = mock(Spot.class);
        when(spot.getId()).thenReturn(UUID.randomUUID());
        when(spot.getLastSensorUpdate()).thenReturn(Instant.now());
        when(spot.getPhysicalStatus()).thenReturn(PhysicalStatus.OCCUPIED);
        
        when(spotRepository.findAll()).thenReturn(List.of(spot));
        when(spotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
        when(reservationRepository.findActiveReservationForSpot(eq(spot.getId()), any(Instant.class)))
                .thenReturn(Optional.empty());

        // Mock existing anomaly of the same type
        SpotAnomaly existingAnomaly = mock(SpotAnomaly.class);
        when(existingAnomaly.getType()).thenReturn(SpotAnomalyType.OCCUPIED_WITHOUT_RESERVATION);
        when(anomalyService.getUnresolvedAnomaliesForSpot(spot.getId())).thenReturn(List.of(existingAnomaly));

        reconciliationService.runReconciliation();

        // Should NOT create a new anomaly
        verify(anomalyService, never()).reportAnomaly(any());
        // Should NOT resolve the existing anomaly
        verify(existingAnomaly, never()).resolve();
    }

    @Test
    void shouldAutoResolveAnomalyIfConditionNoLongerMet() {
        Spot spot = mock(Spot.class);
        when(spot.getId()).thenReturn(UUID.randomUUID());
        when(spot.getLastSensorUpdate()).thenReturn(Instant.now());
        // Spot is now FREE (healthy state)
        when(spot.getPhysicalStatus()).thenReturn(PhysicalStatus.FREE);
        
        when(spotRepository.findAll()).thenReturn(List.of(spot));
        when(spotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
        // No reservation, spot is FREE -> Normal state, no anomalies generated
        when(reservationRepository.findActiveReservationForSpot(eq(spot.getId()), any(Instant.class)))
                .thenReturn(Optional.empty());

        // Mock an old unresolved anomaly (e.g., previously it was OCCUPIED)
        SpotAnomaly oldAnomaly = mock(SpotAnomaly.class);
        when(oldAnomaly.getType()).thenReturn(SpotAnomalyType.OCCUPIED_WITHOUT_RESERVATION);
        when(anomalyService.getUnresolvedAnomaliesForSpot(spot.getId())).thenReturn(List.of(oldAnomaly));

        reconciliationService.runReconciliation();

        // The old anomaly should be resolved and saved
        verify(oldAnomaly).resolve();
        verify(anomalyService).reportAnomaly(oldAnomaly);
    }
}
