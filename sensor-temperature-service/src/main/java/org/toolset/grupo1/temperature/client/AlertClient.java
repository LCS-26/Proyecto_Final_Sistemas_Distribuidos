package org.toolset.grupo1.temperature.client;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AlertClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertClient.class);
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final RestClient restClient;
    private final String internalToken;

    public AlertClient(
            @Value("${alert.service-url:http://localhost:8090}") String alertServiceUrl,
            @Value("${internal.token:stark-internal-token}") String internalToken) {
        this.restClient = RestClient.builder().baseUrl(alertServiceUrl).build();
        this.internalToken = internalToken;
    }

    public void sendCriticalAlert(String source, double value, String message, String correlationId) {
        String cid = correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId;
        try {
            LOGGER.info("event=alert_dispatch source=alert-client cid={} sensorSource={} value={} target=alert-service endpoint=/internal/alerts",
                    cid, source, value);
            restClient.post()
                    .uri("/internal/alerts")
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .header(CORRELATION_ID_HEADER, cid)
                    .body(new AlertRequest("HIGH", message, "TEMPERATURE", source, value, Instant.now()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IllegalStateException("Alert service returned status " + response.getStatusCode());
                    })
                    .toBodilessEntity();
            LOGGER.info("event=alert_delivered source=alert-client cid={} sensorSource={} value={}", cid, source, value);
        } catch (Exception ex) {
            LOGGER.warn("event=alert_failed source=alert-client cid={} sensorSource={} value={} error={}",
                    cid, source, value, ex.getMessage());
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

