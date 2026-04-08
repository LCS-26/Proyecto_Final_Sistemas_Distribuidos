package org.toolset.grupo1.movement.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.movement.api.SensorEventRequest;
import org.toolset.grupo1.movement.api.SensorEventResponse;

@Service
public class MovementDetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementDetectionService.class);

    private static final double CRITICAL_MOVEMENT_THRESHOLD = 1.0;

    private final CopyOnWriteArrayList<SensorEventResponse> events = new CopyOnWriteArrayList<>();
    private final MovementAsyncAlertService movementAsyncAlertService;

    public MovementDetectionService(MovementAsyncAlertService movementAsyncAlertService) {
        this.movementAsyncAlertService = movementAsyncAlertService;
    }

    public SensorEventResponse processMovementEvent(SensorEventRequest request, String correlationId) {
        LOGGER.info("event=sensor_processing source=movement-service cid={} sensorSource={} value={}",
                correlationId, request.source(), request.value());

        boolean critical = request.value() >= CRITICAL_MOVEMENT_THRESHOLD;

        LOGGER.info("event=sensor_decision source=movement-service cid={} sensorSource={} critical={} threshold={}",
                correlationId, request.source(), critical, CRITICAL_MOVEMENT_THRESHOLD);

        SensorEventResponse response = new SensorEventResponse(
                "MOVEMENT",
                request.source(),
                request.value(),
                request.details(),
                critical,
                Instant.now()
        );

        events.add(response);

        if (critical) {
            movementAsyncAlertService.sendCriticalAlert(request, correlationId);
        }

        return response;
    }


    public List<SensorEventResponse> getLatestEvents() {
        List<SensorEventResponse> latest = new ArrayList<>(events);
        Collections.reverse(latest);
        return latest;
    }
}
