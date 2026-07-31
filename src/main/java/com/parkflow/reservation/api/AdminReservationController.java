package com.parkflow.reservation.api;

import com.parkflow.reservation.api.dto.AdminReservationResponse;
import com.parkflow.reservation.application.ReservationService;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/reservations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationController {

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminReservationResponse>> getAllReservations(
            @RequestParam(required = false) ReservationStatusType status,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        return ResponseEntity.ok(reservationService.getAllReservations(status, pageable));
    }
}
