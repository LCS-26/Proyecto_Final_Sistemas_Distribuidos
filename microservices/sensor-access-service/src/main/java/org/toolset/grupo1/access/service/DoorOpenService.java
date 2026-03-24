package org.toolset.grupo1.access.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.access.api.DoorEventRequest;
import org.toolset.grupo1.access.api.DoorEventResponse;
import org.toolset.grupo1.access.client.AlertClient;

@Service
public class DoorOpenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoorOpenService.class);

    private final CopyOnWriteArrayList<DoorEventResponse> events = new CopyOnWriteArrayList<>();
    private final AlertClient alertClient;

    public DoorOpenService(AlertClient alertClient) {
        this.alertClient = alertClient;
    }

    public DoorEventResponse processDoorOpenEvent(DoorEventRequest request) {
        LOGGER.info("Processing door open event for door: {} at {}", request.doorId(), request.location());

        // Door is considered critical if it's open (security risk)
        boolean critical = request.isOpen();

        DoorEventResponse response = new DoorEventResponse(
                "DOOR_OPEN",
                request.doorId(),
                request.location(),
                request.isOpen(),
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
    public void sendCriticalAlert(DoorEventRequest request, DoorEventResponse response) {
        String alertMessage = String.format(
                "Door Security Alert: Door %s at location %s is OPEN. Details: %s",
                request.doorId(),
                request.location(),
                request.details()
        );

        LOGGER.warn(alertMessage);

        try {
            alertClient.sendCriticalAlert(
                    request.doorId(),
                    1.0,  // Value 1.0 indicates open door
                    alertMessage
            );
        } catch (Exception e) {
            LOGGER.error("Failed to send critical alert for door event", e);
        }
    }

    public List<DoorEventResponse> getLatestEvents() {
        return events.reversed();
    }
}

