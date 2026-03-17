package org.toolset.grupo1.temperature.client;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AlertClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertClient.class);

    private final RestClient restClient;

    public AlertClient(@Value("${alert.service-url:http://localhost:8090}") String alertServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(alertServiceUrl).build();
    }

    public void sendCriticalAlert(String source, double value, String message) {
        try {
            restClient.post()
                    .uri("/internal/alerts")
                    .body(new AlertRequest("HIGH", message, "TEMPERATURE", source, value, Instant.now()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IllegalStateException("Alert service returned status " + response.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (Exception ex) {
            LOGGER.warn("Could not deliver critical alert to alert-service: {}", ex.getMessage());
        }
    }

    private record AlertRequest(
            String severity,
            String message,
            String sensorType,
            String source,
            double value,
            Instant timestamp
    ) {
    }
}

