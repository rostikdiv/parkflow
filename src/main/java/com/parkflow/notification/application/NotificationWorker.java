package com.parkflow.notification.application;

import com.parkflow.security.domain.AppUser;
import com.parkflow.security.infra.AppUserRepository;
import com.parkflow.shared.domain.events.NotificationCommand;
import com.parkflow.shared.infra.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);

    private final JavaMailSender mailSender;
    private final AppUserRepository userRepository;

    public NotificationWorker(JavaMailSender mailSender, AppUserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_COMMANDS)
    public void processNotificationCommand(NotificationCommand command) {
        log.info("Received NotificationCommand for user: {}, type: {}", command.userId(), command.type());

        Optional<AppUser> userOpt = userRepository.findById(command.userId());
        if (userOpt.isEmpty()) {
            log.warn("User {} not found, skipping notification", command.userId());
            return;
        }

        AppUser user = userOpt.get();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@parkflow.com");
        message.setTo(user.getEmail());
        message.setSubject("ParkFlow: Reservation " + command.type());
        message.setText("Hello " + user.getFullName() + ",\n\nYour reservation " + command.reservationId() + " has been " + command.type() + ".");

        try {
            mailSender.send(message);
            log.info("Successfully sent email to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send email", e); // Let RabbitMQ retry or DLQ
        }
    }
}
