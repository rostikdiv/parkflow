package com.parkflow.reservation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    @PostMapping
    public ResponseEntity<?> createReservation(@RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey) {
        return ResponseEntity.created(URI.create("/api/v1/reservations/stub")).build();
    }
}
