package com.parkflow.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class PaymentGatewayClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String GATEWAY_URL = "http://localhost:8080/api/internal/mock-gateway/pay";

    public PaymentGatewayClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Retry(name = "paymentService")
    @CircuitBreaker(name = "paymentService")
    public String processPayment(Map<String, Object> payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Payment Gateway Error: " + response.statusCode());
            }

            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            return (String) responseBody.get("transactionId");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Payment failed: " + e.getMessage(), e);
        }
    }
}
