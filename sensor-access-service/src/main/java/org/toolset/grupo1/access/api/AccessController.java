package org.toolset.grupo1.access.api;

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
import org.toolset.grupo1.access.service.AccessAsyncAlertService;

/**
 * Controlador REST del sensor de acceso.
 *
 * Corregido: ahora delega las alertas críticas al AccessAsyncAlertService (@Async)
 * en lugar de llamar directamente a AlertClient de forma síncrona (bug anterior).
 * Esto garantiza que el hilo HTTP no se bloquea esperando la respuesta del alert-service.
 */
@RestController
@RequestMapping("/api/events")
public class AccessController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final CopyOnWriteArrayList<SensorEventResponse> events = new CopyOnWriteArrayList<>();
    private final AccessAsyncAlertService accessAsyncAlertService;

    public AccessController(AccessAsyncAlertService accessAsyncAlertService) {
        this.accessAsyncAlertService = accessAsyncAlertService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SensorEventResponse ingest(
            @RequestBody SensorEventRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=sensor_received source=access-service cid={} endpoint=/api/events sensorSource={} value={} details={}",
                cid, request.source(), request.value(), request.details());
        boolean critical = request.value() <= 0.0;
        LOGGER.info("event=sensor_decision source=access-service cid={} sensorSource={} critical={} threshold=<=0.0",
                cid, request.source(), critical);
        SensorEventResponse response = new SensorEventResponse(
                "ACCESS",
                request.source(),
                request.value(),
                request.details(),
                critical,
                Instant.now());
        events.add(response);

        if (critical) {
            accessAsyncAlertService.sendCriticalAlert(request, cid);
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
