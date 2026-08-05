package com.parkflow.reservation.application;

import com.parkflow.reservation.domain.Reservation;
import com.parkflow.reservation.domain.ReservationAudit;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import com.parkflow.reservation.infra.ReservationAuditRepository;
import com.parkflow.reservation.infra.ReservationRepository;
import com.parkflow.shared.domain.events.NotificationCommand;
import com.parkflow.shared.domain.events.PaymentResult;
import com.parkflow.shared.infra.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultConsumer.class);

    private final ReservationRepository reservationRepository;
    private final ReservationAuditRepository auditRepository;
    private final RabbitTemplate rabbitTemplate;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    public PaymentResultConsumer(ReservationRepository reservationRepository,
                                 ReservationAuditRepository auditRepository,
                                 RabbitTemplate rabbitTemplate,
                                 org.springframework.context.ApplicationEventPublisher applicationEventPublisher) {
        this.reservationRepository = reservationRepository;
        this.auditRepository = auditRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_RESULTS)
    @Transactional
    public void processPaymentResult(PaymentResult result) {
        log.info("Received PaymentResult for reservation: {}, success: {}", result.reservationId(), result.success());

        Reservation reservation = reservationRepository.findById(result.reservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + result.reservationId()));

        ReservationStatusType fromStatus = reservation.getStatus();
        ReservationStatusType toStatus = result.success() ? ReservationStatusType.CONFIRMED : ReservationStatusType.EXPIRED;

        reservation.setStatus(toStatus);
        reservation.setUpdatedAt(java.time.Instant.now());

        ReservationAudit audit = new ReservationAudit(
                UUID.randomUUID(),
                reservation.getId(),
                fromStatus,
                toStatus,
                "SYSTEM",
                result.success() ? "{\"reason\":\"Payment Succeeded\"}" : "{\"reason\":\"Payment Failed\"}"
        );
        auditRepository.save(audit);

        applicationEventPublisher.publishEvent(
            new com.parkflow.shared.domain.events.ReservationChangedEvent(reservation.getId(), reservation.getSpot().getId())
        );

        if (result.success()) {
            NotificationCommand notifyCmd = new NotificationCommand(reservation.getUser().getId(), "CONFIRMED", reservation.getId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NOTIFICATION, "notify.email", notifyCmd);
        }
    }
}
