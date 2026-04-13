package org.toolset.grupo1.gateway.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final RestClient movementClient;
    private final RestClient temperatureClient;
    private final RestClient accessClient;
    private final RestClient alertClient;

    public GatewayController(
            @Value("${services.movement-url:http://localhost:8081}") String movementUrl,
            @Value("${services.temperature-url:http://localhost:8082}") String temperatureUrl,
            @Value("${services.access-url:http://localhost:8083}") String accessUrl,
            @Value("${services.alert-url:http://localhost:8090}") String alertUrl) {
        this.movementClient = RestClient.builder().baseUrl(movementUrl).build();
        this.temperatureClient = RestClient.builder().baseUrl(temperatureUrl).build();
        this.accessClient = RestClient.builder().baseUrl(accessUrl).build();
        this.alertClient = RestClient.builder().baseUrl(alertUrl).build();
    }

    @PostMapping("/movement/events")
    public ResponseEntity<String> movement(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/movement/events payload={}",
                cid, summarizePayload(payload));
        return forwardPost(movementClient, "/api/events", payload, cid);
    }

    @GetMapping("/movement/events")
    public ResponseEntity<String> movementEvents(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/movement/events", cid);
        return forwardGet(movementClient, "/api/events", cid, "/api/movement/events");
    }

    @PostMapping("/seismic/events")
    public ResponseEntity<String> seismic(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/seismic/events payload={}",
                cid, summarizePayload(payload));
        return forwardPost(movementClient, "/api/seismic/events", payload, cid);
    }

    @GetMapping("/seismic/events")
    public ResponseEntity<String> seismicEvents(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/seismic/events", cid);
        return forwardGet(movementClient, "/api/seismic/events", cid, "/api/seismic/events");
    }

    @PostMapping("/temperature/events")
    public ResponseEntity<String> temperature(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/temperature/events payload={}",
                cid, summarizePayload(payload));
        return forwardPost(temperatureClient, "/api/events", payload, cid);
    }

    @GetMapping("/temperature/events")
    public ResponseEntity<String> temperatureEvents(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/temperature/events", cid);
        return forwardGet(temperatureClient, "/api/events", cid, "/api/temperature/events");
    }

    @PostMapping("/access/events")
    public ResponseEntity<String> access(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/access/events payload={}",
                cid, summarizePayload(payload));
        return forwardPost(accessClient, "/api/events", payload, cid);
    }

    @GetMapping("/access/events")
    public ResponseEntity<String> accessEvents(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/access/events", cid);
        return forwardGet(accessClient, "/api/events", cid, "/api/access/events");
    }

    @PostMapping("/door-open/events")
    public ResponseEntity<String> doorOpen(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/door-open/events payload={}",
                cid, summarizePayload(payload));
        return forwardPost(accessClient, "/api/door-open/events", payload, cid);
    }

    @GetMapping("/door-open/events")
    public ResponseEntity<String> doorOpenEvents(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/door-open/events", cid);
        return forwardGet(accessClient, "/api/door-open/events", cid, "/api/door-open/events");
    }

    @PostMapping("/access/check")
    public ResponseEntity<String> accessCheck(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/access/check payload={}",
                cid, summarizePayload(payload));
        return forwardPost(accessClient, "/api/access/check", payload, cid);
    }

    @GetMapping("/alerts")
    public ResponseEntity<String> alerts(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=gateway_received source=gateway-service cid={} endpoint=/api/alerts", cid);
        return forwardGet(alertClient, "/api/alerts", cid, "/api/alerts");
    }

    private ResponseEntity<String> forwardPost(
            RestClient client,
            String path,
            Map<String, Object> payload,
            String correlationId) {
        LOGGER.info("event=gateway_forward source=gateway-service cid={} method=POST endpoint={} downstream={} payload={}",
                correlationId, path, path, summarizePayload(payload));
        ResponseEntity<String> downstream = client.post()
                .uri(path)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(payload)
                .retrieve()
                .toEntity(String.class);
        return ResponseEntity.status(downstream.getStatusCode())
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(downstream.getBody());
    }

    private ResponseEntity<String> forwardGet(RestClient client, String path, String correlationId, String endpoint) {
        LOGGER.info("event=gateway_forward source=gateway-service cid={} method=GET endpoint={} downstream={}",
                correlationId, endpoint, path);
        ResponseEntity<String> downstream = client.get()
                .uri(path)
                .header(CORRELATION_ID_HEADER, correlationId)
                .retrieve()
                .toEntity(String.class);
        return ResponseEntity.status(downstream.getStatusCode())
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(downstream.getBody());
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return incomingCorrelationId;
    }

    private String summarizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }

        List<String> parts = new ArrayList<>();
        appendIfPresent(parts, payload, "source");
        appendIfPresent(parts, payload, "value");
        appendIfPresent(parts, payload, "details");
        appendIfPresent(parts, payload, "sensorId");
        appendIfPresent(parts, payload, "impactForce");
        appendIfPresent(parts, payload, "location");
        appendIfPresent(parts, payload, "doorId");
        appendIfPresent(parts, payload, "isOpen");
        appendIfPresent(parts, payload, "username");
        appendIfPresent(parts, payload, "area");
        appendIfPresent(parts, payload, "badgeValid");

        return parts.isEmpty() ? "keys=" + payload.keySet() : "{" + String.join(", ", parts) + "}";
    }

    private void appendIfPresent(List<String> parts, Map<String, Object> payload, String key) {
        if (payload.containsKey(key)) {
            parts.add(key + "=" + payload.get(key));
        }
    }
}
