package com.parkflow.inventory.api;

import com.parkflow.inventory.api.dto.SpotAnomalyResponse;
import com.parkflow.inventory.application.AnomalyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/v1/anomalies")
public class AdminAnomalyController {

    private final AnomalyService anomalyService;

    public AdminAnomalyController(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    @GetMapping
    public List<SpotAnomalyResponse> getAnomalies(@RequestParam(defaultValue = "false") boolean resolved) {
        return anomalyService.getAnomalies(resolved);
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolveAnomaly(@PathVariable UUID id) {
        try {
            anomalyService.resolveAnomaly(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
