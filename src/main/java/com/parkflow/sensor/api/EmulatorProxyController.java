package com.parkflow.sensor.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Proxy controller for the Node.js Sensor Emulator.
 * This allows the frontend to control the emulator without direct network access,
 * and solves CORS issues by routing traffic through the Spring Boot backend.
 */
@RestController
@RequestMapping("/api/v1/emulator")
public class EmulatorProxyController {

    private static final Logger log = LoggerFactory.getLogger(EmulatorProxyController.class);

    private final HttpClient httpClient;
    private final String emulatorBaseUrl;

    public EmulatorProxyController(@Value("${parkflow.emulator.url:http://localhost:8081}") String emulatorBaseUrl) {
        this.emulatorBaseUrl = emulatorBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return forwardRequest("GET", "/api/emulator/status");
    }

    @PostMapping("/start")
    public ResponseEntity<String> startEmulator() {
        return forwardRequest("POST", "/api/emulator/start");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopEmulator() {
        return forwardRequest("POST", "/api/emulator/stop");
    }

    private ResponseEntity<String> forwardRequest(String method, String path) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(emulatorBaseUrl + path))
                    .timeout(Duration.ofSeconds(5));

            if ("POST".equalsIgnoreCase(method)) {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            } else {
                requestBuilder.GET();
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            return ResponseEntity
                    .status(response.statusCode())
                    .header("Content-Type", "application/json")
                    .body(response.body());

        } catch (Exception e) {
            log.error("Failed to forward request to emulator at {}{}: {}", emulatorBaseUrl, path, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\": \"Emulator is unreachable\"}");
        }
    }
}
