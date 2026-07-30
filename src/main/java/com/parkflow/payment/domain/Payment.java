package com.parkflow.payment.domain;

import com.parkflow.payment.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    private UUID id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private UUID reservationId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "external_ref")
    private String externalRef;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Payment(UUID id, UUID reservationId, BigDecimal amount) {
        this.id = id;
        this.reservationId = reservationId;
        this.amount = amount;
        this.status = PaymentStatus.INITIATED;
        this.attempts = 0;
    }

    public void markSucceeded(String externalRef) {
        this.status = PaymentStatus.SUCCEEDED;
        this.externalRef = externalRef;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = PaymentStatus.FAILED;
        this.lastError = error;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
