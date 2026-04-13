package org.toolset.grupo1.seismic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.movement.publisher.AlertPublisher;
import org.toolset.grupo1.seismic.api.SeismicEventRequest;

/**
 * Servicio asíncrono para publicar alertas sísmicas críticas via AlertPublisher.
 * Usa la implementación @Primary (AmqpAlertPublisher → RabbitMQ) con fallback HTTP.
 */
@Service
public class SeismicAsyncAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeismicAsyncAlertService.class);

    private final AlertPublisher alertPublisher;

    public SeismicAsyncAlertService(AlertPublisher alertPublisher) {
        this.alertPublisher = alertPublisher;
    }

    @Async
    public void sendCriticalAlert(SeismicEventRequest request, String correlationId) {
        LOGGER.info("event=alert_prepare source=seismic-service cid={} sensorSource={} impactForce={} location={}",
                correlationId, request.sensorId(), request.impactForce(), request.location());

        try {
            String message = String.format(
                    "Seismic Door Alert: Sensor %s at location %s detected HIGH IMPACT FORCE (%.2f). Details: %s",
                    request.sensorId(), request.location(), request.impactForce(), request.details());
            alertPublisher.publish(request.sensorId(), request.impactForce(), message, correlationId);
        } catch (Exception e) {
            LOGGER.error("cid={} failed to send critical seismic alert", correlationId, e);
        }
    }
}
