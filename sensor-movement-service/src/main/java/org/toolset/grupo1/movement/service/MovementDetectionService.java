package org.toolset.grupo1.movement.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.movement.api.SensorEventRequest;
import org.toolset.grupo1.movement.api.SensorEventResponse;
import org.toolset.grupo1.movement.client.AlertClient;

@Service
public class MovementDetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementDetectionService.class);

    private static final double CRITICAL_MOVEMENT_THRESHOLD = 1.0;

    private final CopyOnWriteArrayList<SensorEventResponse> events = new CopyOnWriteArrayList<>();
    private final AlertClient alertClient;

    public MovementDetectionService(AlertClient alertClient) {
        this.alertClient = alertClient;
    }

    public SensorEventResponse processMovementEvent(SensorEventRequest request) {
        LOGGER.info("Processing movement event from sensor: {} at location: {} with intensity: {}",
                request.source(), "unknown", request.value());

        boolean critical = request.value() >= CRITICAL_MOVEMENT_THRESHOLD;

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
            sendCriticalAlert(request, response);
        }

        return response;
    }

    @Async
    public void sendCriticalAlert(SensorEventRequest request, SensorEventResponse response) {
        String alertMessage = String.format(
                "Movement Detected: Sensor %s detected movement with intensity %.2f. Details: %s",
                request.source(),
                request.value(),
                request.details()
        );

        LOGGER.warn(alertMessage);

        try {
            alertClient.sendCriticalAlert(
                    request.source(),
                    request.value(),
                    "Movement detected"
            );
        } catch (Exception e) {
            LOGGER.error("Failed to send critical alert for movement event", e);
        }
    }

    public List<SensorEventResponse> getLatestEvents() {
        List<SensorEventResponse> latest = new ArrayList<>(events);
        Collections.reverse(latest);
        return latest;
    }
}
