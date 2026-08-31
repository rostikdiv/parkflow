package com.parkflow.shared.domain;

import com.parkflow.inventory.domain.ParkingLot;
import com.parkflow.inventory.domain.Spot;
import com.parkflow.inventory.domain.SpotAnomaly;
import com.parkflow.inventory.domain.enums.ParkingLotType;
import com.parkflow.inventory.domain.enums.PhysicalStatus;
import com.parkflow.inventory.domain.enums.SpotAnomalyType;
import com.parkflow.inventory.domain.enums.SpotType;
import com.parkflow.payment.domain.Payment;
import com.parkflow.reservation.domain.Reservation;
import com.parkflow.reservation.domain.ReservationAudit;
import com.parkflow.reservation.domain.enums.ReservationStatusType;
import com.parkflow.security.domain.AppUser;
import com.parkflow.sensor.domain.SensorEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA Entity Equals, HashCode and ToString Contract Tests")
class EntityEqualsHashCodeToStringTest {

    private void setId(Object target, UUID id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id on " + target.getClass().getSimpleName(), e);
        }
    }

    @Test
    @DisplayName("ParkingLot: equals, hashCode, and safe toString")
    void testParkingLot() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ParkingLot lot1 = new ParkingLot("Central Lot", "Main St 1", 50.45, 30.52,
                ParkingLotType.UNDERGROUND, BigDecimal.valueOf(25.0),
                LocalTime.of(8, 0), LocalTime.of(22, 0), ZoneId.of("UTC"));
        setId(lot1, id1);

        ParkingLot lot2 = new ParkingLot("Central Lot Copy", "Main St 1", 50.45, 30.52,
                ParkingLotType.UNDERGROUND, BigDecimal.valueOf(25.0),
                LocalTime.of(8, 0), LocalTime.of(22, 0), ZoneId.of("UTC"));
        setId(lot2, id1);

        ParkingLot lot3 = new ParkingLot("Other Lot", "Second St 2", 50.46, 30.53,
                ParkingLotType.OPEN_AIR, BigDecimal.valueOf(15.0),
                LocalTime.of(0, 0), LocalTime.of(23, 59), ZoneId.of("UTC"));
        setId(lot3, id2);

        // Reflexive
        assertThat(lot1.equals(lot1)).isTrue();
        // Symmetric
        assertThat(lot1.equals(lot2)).isTrue();
        assertThat(lot2.equals(lot1)).isTrue();
        // Different IDs
        assertThat(lot1.equals(lot3)).isFalse();
        // Null and other types
        assertThat(lot1.equals(null)).isFalse();
        assertThat(lot1.equals("not an entity")).isFalse();

        // HashCode consistency
        assertThat(lot1.hashCode()).isEqualTo(lot2.hashCode());
        assertThat(lot1.hashCode()).isEqualTo(ParkingLot.class.hashCode());

        // ToString safe and informative
        String str = lot1.toString();
        assertThat(str).contains("Central Lot", id1.toString(), "UNDERGROUND");
        assertThat(str).doesNotContain("spots=");
    }

    @Test
    @DisplayName("Spot: equals, hashCode, and safe toString")
    void testSpot() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Spot spot1 = new Spot("A-1", SpotType.STANDARD, 10.0, 20.0);
        setId(spot1, id1);

        Spot spot2 = new Spot("A-1-Duplicate", SpotType.STANDARD, 10.0, 20.0);
        setId(spot2, id1);

        Spot spot3 = new Spot("B-2", SpotType.EV_CHARGING, 30.0, 40.0);
        setId(spot3, id2);

        assertThat(spot1.equals(spot1)).isTrue();
        assertThat(spot1.equals(spot2)).isTrue();
        assertThat(spot1.equals(spot3)).isFalse();
        assertThat(spot1.equals(null)).isFalse();

        assertThat(spot1.hashCode()).isEqualTo(Spot.class.hashCode());

        String str = spot1.toString();
        assertThat(str).contains("A-1", "STANDARD", id1.toString());
    }

    @Test
    @DisplayName("SpotAnomaly: equals, hashCode, and safe toString")
    void testSpotAnomaly() {
        UUID spotId = UUID.randomUUID();
        Spot spot = new Spot("A-1", SpotType.STANDARD, 0.0, 0.0);
        setId(spot, spotId);

        UUID anomalyId1 = UUID.randomUUID();
        UUID anomalyId2 = UUID.randomUUID();

        SpotAnomaly anomaly1 = new SpotAnomaly(spot, SpotAnomalyType.OCCUPIED_WITHOUT_RESERVATION, "No reservation", Instant.now());
        setId(anomaly1, anomalyId1);

        SpotAnomaly anomaly2 = new SpotAnomaly(spot, SpotAnomalyType.OCCUPIED_WITHOUT_RESERVATION, "Different details", Instant.now());
        setId(anomaly2, anomalyId1);

        SpotAnomaly anomaly3 = new SpotAnomaly(spot, SpotAnomalyType.SENSOR_SILENT, "Timeout", Instant.now());
        setId(anomaly3, anomalyId2);

        assertThat(anomaly1.equals(anomaly2)).isTrue();
        assertThat(anomaly1.equals(anomaly3)).isFalse();
        assertThat(anomaly1.equals(null)).isFalse();

        assertThat(anomaly1.hashCode()).isEqualTo(SpotAnomaly.class.hashCode());

        String str = anomaly1.toString();
        assertThat(str).contains(anomalyId1.toString(), "OCCUPIED_WITHOUT_RESERVATION", spotId.toString());
    }

    @Test
    @DisplayName("AppUser: equals, hashCode, and toString without passwordHash")
    void testAppUser() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        String secretHash = "$2a$12$eX4mpL3S3cr3tH4shV4lu3";

        AppUser user1 = new AppUser(userId1, "john@example.com", secretHash, "John Doe", "+380501234567", "USER");
        AppUser user2 = new AppUser(userId1, "john.updated@example.com", secretHash, "John Doe Updated", "+380501234567", "USER");
        AppUser user3 = new AppUser(userId2, "jane@example.com", secretHash, "Jane Doe", "+380507654321", "ADMIN");

        assertThat(user1.equals(user2)).isTrue();
        assertThat(user1.equals(user3)).isFalse();
        assertThat(user1.equals(null)).isFalse();

        assertThat(user1.hashCode()).isEqualTo(AppUser.class.hashCode());

        String str = user1.toString();
        assertThat(str).contains("john@example.com", "John Doe", "USER", userId1.toString());
        // Critical security test: passwordHash must NOT be leaked into toString
        assertThat(str).doesNotContain(secretHash);
        assertThat(str).doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("Reservation: equals, hashCode, and safe toString")
    void testReservation() {
        UUID resId1 = UUID.randomUUID();
        UUID resId2 = UUID.randomUUID();

        AppUser user = new AppUser(UUID.randomUUID(), "u@test.com", "hash", "User", "123", "USER");
        Spot spot = new Spot("S-1", SpotType.STANDARD, 0.0, 0.0);
        setId(spot, UUID.randomUUID());

        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        Reservation res1 = new Reservation(resId1, user, spot, "AA1234BB", start, end,
                ReservationStatusType.CONFIRMED, BigDecimal.valueOf(50.0), "key-1");
        Reservation res2 = new Reservation(resId1, user, spot, "AA1234BB", start, end,
                ReservationStatusType.CONFIRMED, BigDecimal.valueOf(50.0), "key-1");
        Reservation res3 = new Reservation(resId2, user, spot, "BC5678CD", start, end,
                ReservationStatusType.PENDING, BigDecimal.valueOf(30.0), "key-2");

        assertThat(res1.equals(res2)).isTrue();
        assertThat(res1.equals(res3)).isFalse();
        assertThat(res1.equals(null)).isFalse();

        assertThat(res1.hashCode()).isEqualTo(Reservation.class.hashCode());

        String str = res1.toString();
        assertThat(str).contains(resId1.toString(), "AA1234BB", "CONFIRMED", "key-1");
    }

    @Test
    @DisplayName("ReservationAudit: equals, hashCode, and toString")
    void testReservationAudit() {
        UUID auditId1 = UUID.randomUUID();
        UUID auditId2 = UUID.randomUUID();
        UUID resId = UUID.randomUUID();

        ReservationAudit audit1 = new ReservationAudit(auditId1, resId, ReservationStatusType.PENDING,
                ReservationStatusType.CONFIRMED, "PAYMENT", "{\"ref\":\"tx_123\"}");
        ReservationAudit audit2 = new ReservationAudit(auditId1, resId, ReservationStatusType.PENDING,
                ReservationStatusType.CONFIRMED, "PAYMENT", "{\"ref\":\"tx_123\"}");
        ReservationAudit audit3 = new ReservationAudit(auditId2, resId, ReservationStatusType.CONFIRMED,
                ReservationStatusType.ACTIVE, "SENSOR", null);

        assertThat(audit1.equals(audit2)).isTrue();
        assertThat(audit1.equals(audit3)).isFalse();
        assertThat(audit1.equals(null)).isFalse();

        assertThat(audit1.hashCode()).isEqualTo(ReservationAudit.class.hashCode());

        String str = audit1.toString();
        assertThat(str).contains(auditId1.toString(), resId.toString(), "PAYMENT", "CONFIRMED");
    }

    @Test
    @DisplayName("Payment: equals, hashCode, and toString")
    void testPayment() {
        UUID payId1 = UUID.randomUUID();
        UUID payId2 = UUID.randomUUID();
        UUID resId = UUID.randomUUID();

        Payment payment1 = new Payment(payId1, resId, BigDecimal.valueOf(100.0));
        Payment payment2 = new Payment(payId1, resId, BigDecimal.valueOf(100.0));
        Payment payment3 = new Payment(payId2, resId, BigDecimal.valueOf(200.0));

        assertThat(payment1.equals(payment2)).isTrue();
        assertThat(payment1.equals(payment3)).isFalse();
        assertThat(payment1.equals(null)).isFalse();

        assertThat(payment1.hashCode()).isEqualTo(Payment.class.hashCode());

        String str = payment1.toString();
        assertThat(str).contains(payId1.toString(), resId.toString(), "100", "INITIATED");
    }

    @Test
    @DisplayName("SensorEvent: equals, hashCode, and toString")
    void testSensorEvent() {
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        UUID spotId = UUID.randomUUID();

        SensorEvent event1 = new SensorEvent("ext-1", spotId, PhysicalStatus.OCCUPIED, Instant.now(), Instant.now(), "{}");
        setId(event1, eventId1);

        SensorEvent event2 = new SensorEvent("ext-1-dup", spotId, PhysicalStatus.OCCUPIED, Instant.now(), Instant.now(), "{}");
        setId(event2, eventId1);

        SensorEvent event3 = new SensorEvent("ext-2", spotId, PhysicalStatus.FREE, Instant.now(), Instant.now(), "{}");
        setId(event3, eventId2);

        assertThat(event1.equals(event2)).isTrue();
        assertThat(event1.equals(event3)).isFalse();
        assertThat(event1.equals(null)).isFalse();

        assertThat(event1.hashCode()).isEqualTo(SensorEvent.class.hashCode());

        String str = event1.toString();
        assertThat(str).contains(eventId1.toString(), "ext-1", spotId.toString(), "OCCUPIED");
    }

    @Test
    @DisplayName("Transient entities with null ID must not be equal to other transient entities")
    void testTransientEntitiesNotEqual() {
        ParkingLot lot1 = new ParkingLot("Lot 1", "Addr 1", 10.0, 20.0,
                ParkingLotType.OPEN_AIR, BigDecimal.TEN, LocalTime.MIN, LocalTime.MAX, ZoneId.of("UTC"));
        ParkingLot lot2 = new ParkingLot("Lot 2", "Addr 2", 10.0, 20.0,
                ParkingLotType.OPEN_AIR, BigDecimal.TEN, LocalTime.MIN, LocalTime.MAX, ZoneId.of("UTC"));

        // Both lot1 and lot2 have id == null
        assertThat(lot1.equals(lot2)).isFalse();
    }
}
