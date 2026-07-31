package com.parkflow.reservation.api;

import com.parkflow.reservation.api.dto.ReservationRequest;
import com.parkflow.reservation.api.dto.ReservationResponse;
import com.parkflow.reservation.application.ReservationService;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import com.parkflow.security.domain.AppUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @AuthenticationPrincipal AppUser currentUser,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(currentUser.getId(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal AppUser currentUser,
            @RequestParam(required = false) ReservationStatusType status,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        return ResponseEntity.ok(reservationService.getMyReservations(currentUser.getId(), status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.getReservation(id, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, currentUser.getId()));
    }
}
