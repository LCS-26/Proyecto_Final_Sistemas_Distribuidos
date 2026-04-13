package org.toolset.grupo1.access.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.access.api.DoorEventRequest;
import org.toolset.grupo1.access.publisher.AlertPublisher;

/**
 * Servicio asíncrono para alertas de puerta abierta via AlertPublisher.
 * Usa la implementación @Primary (AmqpAlertPublisher → RabbitMQ) con fallback HTTP.
 */
@Service
public class DoorOpenAsyncAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoorOpenAsyncAlertService.class);

    private final AlertPublisher alertPublisher;

    public DoorOpenAsyncAlertService(AlertPublisher alertPublisher) {
        this.alertPublisher = alertPublisher;
    }

    @Async
    public void sendCriticalAlert(DoorEventRequest request, String correlationId) {
        LOGGER.info("event=alert_prepare source=access-service cid={} sensorSource={} location={}",
                correlationId, request.doorId(), request.location());

        try {
            String message = String.format(
                    "Door Security Alert: Door %s at location %s is OPEN. Details: %s",
                    request.doorId(), request.location(), request.details());
            alertPublisher.publish(request.doorId(), 1.0, message, correlationId);
        } catch (Exception e) {
            LOGGER.error("cid={} failed to send critical door-open alert", correlationId, e);
        }
    }
}
