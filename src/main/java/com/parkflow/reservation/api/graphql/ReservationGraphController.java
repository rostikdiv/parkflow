package com.parkflow.reservation.api.graphql;

import com.parkflow.reservation.api.dto.ReservationResponse;
import com.parkflow.reservation.application.ReservationService;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class ReservationGraphController {

    private final ReservationService reservationService;
    
    // For MVP, we will use a hardcoded user ID as done in the REST controller.
    // In a real application, this would be injected via Spring Security context.
    private static final UUID CURRENT_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public ReservationGraphController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @QueryMapping
    public List<ReservationResponse> myReservations(@Argument String status) {
        ReservationStatusType statusType = status != null ? ReservationStatusType.valueOf(status) : null;
        return reservationService.getMyReservations(CURRENT_USER_ID, statusType);
    }
}
