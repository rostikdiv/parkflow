package com.parkflow.inventory.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parking-lots")
public class ParkingLotController {

    @GetMapping
    public ResponseEntity<?> getParkingLots() {
        return ResponseEntity.ok().build();
    }
}
