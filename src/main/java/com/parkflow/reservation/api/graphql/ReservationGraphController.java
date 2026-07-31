package com.parkflow.reservation.api.graphql;

import com.parkflow.reservation.api.dto.ReservationResponse;
import com.parkflow.reservation.application.ReservationService;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import com.parkflow.security.domain.AppUser;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ReservationGraphController {

    private final ReservationService reservationService;

    public ReservationGraphController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @QueryMapping
    public org.springframework.data.domain.Page<ReservationResponse> myReservations(
            @AuthenticationPrincipal AppUser currentUser,
            @Argument String status,
            @Argument Integer page,
            @Argument Integer size) {
        ReservationStatusType statusType = status != null ? ReservationStatusType.valueOf(status) : null;
        int p = page != null ? page : 0;
        int s = size != null ? size : 10;
        return reservationService.getMyReservations(currentUser.getId(), statusType, org.springframework.data.domain.PageRequest.of(p, s));
    }
}
