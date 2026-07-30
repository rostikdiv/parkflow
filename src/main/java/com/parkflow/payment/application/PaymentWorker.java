package com.parkflow.payment.application;

import com.parkflow.payment.domain.Payment;
import com.parkflow.payment.infra.PaymentRepository;
import com.parkflow.shared.domain.events.PaymentCommand;
import com.parkflow.shared.domain.events.PaymentResult;
import com.parkflow.shared.infra.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class PaymentWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentWorker.class);

    private final PaymentGatewayClient gatewayClient;
    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    public PaymentWorker(PaymentGatewayClient gatewayClient, PaymentRepository paymentRepository, RabbitTemplate rabbitTemplate) {
        this.gatewayClient = gatewayClient;
        this.paymentRepository = paymentRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_COMMANDS)
    public void processPaymentCommand(PaymentCommand command) {
        log.info("Received PaymentCommand for reservation: {}", command.reservationId());

        Payment payment = paymentRepository.findByReservationId(command.reservationId())
                .orElseGet(() -> paymentRepository.save(new Payment(UUID.randomUUID(), command.reservationId(), command.amount())));

        payment.incrementAttempts();

        try {
            Map<String, Object> payload = Map.of(
                    "reservationId", command.reservationId().toString(),
                    "amount", command.amount()
            );

            String transactionId = gatewayClient.processPayment(payload);
            
            payment.markSucceeded(transactionId);
            paymentRepository.save(payment);
            
            log.info("Payment SUCCESS for reservation: {}", command.reservationId());
            publishResult(new PaymentResult(command.reservationId(), true, transactionId, null));
            
        } catch (Exception e) {
            log.error("Payment FAILED for reservation: {}", command.reservationId(), e);
            payment.markFailed(e.getMessage());
            paymentRepository.save(payment);
            
            publishResult(new PaymentResult(command.reservationId(), false, null, e.getMessage()));
        }
    }

    private void publishResult(PaymentResult result) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_PAYMENT, "payment.result", result);
    }
}
