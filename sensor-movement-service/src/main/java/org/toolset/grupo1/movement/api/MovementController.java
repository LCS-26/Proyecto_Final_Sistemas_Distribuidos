package org.toolset.grupo1.movement.api;

import java.util.List;
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
import org.toolset.grupo1.movement.service.MovementDetectionService;

@RestController
@RequestMapping("/api/events")
public class MovementController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final MovementDetectionService movementDetectionService;

    public MovementController(MovementDetectionService movementDetectionService) {
        this.movementDetectionService = movementDetectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SensorEventResponse ingest(
            @RequestBody SensorEventRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=sensor_received source=movement-service cid={} endpoint=/api/events sensorSource={} value={} details={}",
                cid, request.source(), request.value(), request.details());
        return movementDetectionService.processMovementEvent(request, cid);
    }

    @GetMapping
    public List<SensorEventResponse> latest() {
        return movementDetectionService.getLatestEvents();
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        return incomingCorrelationId;
    }
}

