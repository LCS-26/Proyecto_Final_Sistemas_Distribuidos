package org.toolset.grupo1.access.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.access.api.DoorEventRequest;
import org.toolset.grupo1.access.client.AlertClient;

@Service
public class DoorOpenAsyncAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoorOpenAsyncAlertService.class);

    private final AlertClient alertClient;

    public DoorOpenAsyncAlertService(AlertClient alertClient) {
        this.alertClient = alertClient;
    }

    @Async
    public void sendCriticalAlert(DoorEventRequest request, String correlationId) {
        LOGGER.info("event=alert_prepare source=access-service cid={} sensorSource={} location={}",
                correlationId, request.doorId(), request.location());

        try {
            alertClient.sendCriticalAlert(
                    request.doorId(),
                    1.0,
                    String.format(
                            "Door Security Alert: Door %s at location %s is OPEN. Details: %s",
                            request.doorId(),
                            request.location(),
                            request.details()),
                    correlationId
            );
        } catch (Exception e) {
            LOGGER.error("cid={} failed to send critical door-open alert", correlationId, e);
        }
    }
}

