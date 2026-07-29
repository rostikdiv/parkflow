package com.parkflow.reservation.api;

import com.parkflow.reservation.api.dto.ReservationRequest;
import com.parkflow.reservation.api.dto.ReservationResponse;
import com.parkflow.reservation.application.ReservationService;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API Controller for managing reservations.
 * Follows CQRS-lite approach: Handles commands (POST/DELETE) and simple queries
 * (GET),
 * while complex real-time updates are offloaded to GraphQL (M6).
 * Requires Idempotency-Key header for POST requests.
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    // Temporary user ID for M2 until M8 (Security) is implemented
    private static final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(CURRENT_USER_ID, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @RequestParam(required = false) ReservationStatusType status) {
        return ResponseEntity.ok(reservationService.getMyReservations(CURRENT_USER_ID, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.getReservation(id, CURRENT_USER_ID));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, CURRENT_USER_ID));
    }
}
