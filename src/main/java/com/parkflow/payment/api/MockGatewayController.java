package com.parkflow.payment.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/mock-gateway")
public class MockGatewayController {

    private final Random random = new Random();

    @Value("${mock-gateway.chaos.error-rate:0.3}")
    private double errorRate;

    @Value("${mock-gateway.chaos.timeout-rate:0.2}")
    private double timeoutRate;

    @Value("${mock-gateway.chaos.max-delay-ms:5000}")
    private int maxDelayMs;

    @PostMapping("/pay")
    public ResponseEntity<Map<String, String>> processPayment(@RequestBody Map<String, Object> payload) throws InterruptedException {
        double chance = random.nextDouble();

        if (chance < timeoutRate) {
            // Simulate a timeout (e.g. hanging for longer than TimeLimiter allows)
            Thread.sleep(maxDelayMs);
        } else if (chance < (timeoutRate + errorRate)) {
            // Simulate a 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Simulated gateway failure"));
        }

        // Simulate normal processing time
        Thread.sleep(200 + random.nextInt(300));

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "transactionId", "txn_" + UUID.randomUUID().toString()
        ));
    }
}
