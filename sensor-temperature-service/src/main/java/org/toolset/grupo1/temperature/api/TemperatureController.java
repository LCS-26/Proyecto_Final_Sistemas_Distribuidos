package org.toolset.grupo1.temperature.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.toolset.grupo1.temperature.service.TemperatureAsyncAlertService;

/**
 * Controlador REST del sensor de temperatura.
 *
 * Corregido: ahora delega las alertas críticas al TemperatureAsyncAlertService (@Async)
 * en lugar de llamar directamente a AlertClient de forma síncrona (bug anterior).
 * Esto garantiza que el hilo HTTP no se bloquea esperando la respuesta del alert-service.
 */
@RestController
@RequestMapping("/api/events")
public class TemperatureController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemperatureController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final CopyOnWriteArrayList<SensorEventResponse> events = new CopyOnWriteArrayList<>();
    private final TemperatureAsyncAlertService temperatureAsyncAlertService;

    public TemperatureController(TemperatureAsyncAlertService temperatureAsyncAlertService) {
        this.temperatureAsyncAlertService = temperatureAsyncAlertService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SensorEventResponse ingest(
            @RequestBody SensorEventRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=sensor_received source=temperature-service cid={} endpoint=/api/events sensorSource={} value={} details={}",
                cid, request.source(), request.value(), request.details());
        boolean critical = request.value() >= 60.0;
        LOGGER.info("event=sensor_decision source=temperature-service cid={} sensorSource={} critical={} threshold=60.0",
                cid, request.source(), critical);
        SensorEventResponse response = new SensorEventResponse(
                "TEMPERATURE",
                request.source(),
                request.value(),
                request.details(),
                critical,
                Instant.now());
        events.add(response);

        if (critical) {
            temperatureAsyncAlertService.sendCriticalAlert(request, cid);
        }

        return response;
    }

    @GetMapping
    public List<SensorEventResponse> latest() {
        List<SensorEventResponse> latest = new ArrayList<>(events);
        Collections.reverse(latest);
        return latest;
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        return incomingCorrelationId;
    }
}
