package org.toolset.grupo1.movement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.movement.api.SensorEventRequest;
import org.toolset.grupo1.movement.client.AlertClient;

@Service
public class MovementAsyncAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementAsyncAlertService.class);

    private final AlertClient alertClient;

    public MovementAsyncAlertService(AlertClient alertClient) {
        this.alertClient = alertClient;
    }

    @Async
    public void sendCriticalAlert(SensorEventRequest request, String correlationId) {

        LOGGER.info("event=alert_prepare source=movement-service cid={} sensorSource={} intensity={}",
                correlationId, request.source(), request.value());

        try {
            alertClient.sendCriticalAlert(
                    request.source(),
                    request.value(),
                    "Movement detected",
                    correlationId
            );
        } catch (Exception e) {
            LOGGER.error("cid={} failed to send critical movement alert", correlationId, e);
        }
    }
}

