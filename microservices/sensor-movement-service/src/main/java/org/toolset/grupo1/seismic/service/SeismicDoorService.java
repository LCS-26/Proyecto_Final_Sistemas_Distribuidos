package org.toolset.grupo1.seismic.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.movement.client.AlertClient;
import org.toolset.grupo1.seismic.api.SeismicEventRequest;
import org.toolset.grupo1.seismic.api.SeismicEventResponse;

@Service
public class SeismicDoorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeismicDoorService.class);

    private static final double CRITICAL_IMPACT_THRESHOLD = 7.0;  // Force threshold for critical alert

    private final CopyOnWriteArrayList<SeismicEventResponse> events = new CopyOnWriteArrayList<>();
    private final AlertClient alertClient;

    public SeismicDoorService(AlertClient alertClient) {
        this.alertClient = alertClient;
    }

    public SeismicEventResponse processSeismicEvent(SeismicEventRequest request) {
        LOGGER.info("Processing seismic event for sensor: {} at {} with impact force: {}",
                request.sensorId(), request.location(), request.impactForce());

        // Event is critical if impact force exceeds threshold (potential forced entry)
        boolean critical = request.impactForce() >= CRITICAL_IMPACT_THRESHOLD;

        SeismicEventResponse response = new SeismicEventResponse(
                "SEISMIC_DOOR",
                request.sensorId(),
                request.location(),
                request.impactForce(),
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
    public void sendCriticalAlert(SeismicEventRequest request, SeismicEventResponse response) {
        String alertMessage = String.format(
                "Seismic Door Alert: Sensor %s at location %s detected HIGH IMPACT FORCE (%.2f). Details: %s",
                request.sensorId(),
                request.location(),
                request.impactForce(),
                request.details()
        );

        LOGGER.warn(alertMessage);

        try {
            alertClient.sendCriticalAlert(
                    request.sensorId(),
                    request.impactForce(),
                    alertMessage
            );
        } catch (Exception e) {
            LOGGER.error("Failed to send critical alert for seismic event", e);
        }
    }

    public List<SeismicEventResponse> getLatestEvents() {
        return events.reversed();
    }
}

