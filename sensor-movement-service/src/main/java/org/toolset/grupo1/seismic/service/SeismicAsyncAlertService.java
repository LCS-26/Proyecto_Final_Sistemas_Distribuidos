package org.toolset.grupo1.seismic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.movement.client.AlertClient;
import org.toolset.grupo1.seismic.api.SeismicEventRequest;

@Service
public class SeismicAsyncAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeismicAsyncAlertService.class);

    private final AlertClient alertClient;

    public SeismicAsyncAlertService(AlertClient alertClient) {
        this.alertClient = alertClient;
    }

    @Async
    public void sendCriticalAlert(SeismicEventRequest request, String correlationId) {
        LOGGER.info("event=alert_prepare source=seismic-service cid={} sensorSource={} impactForce={} location={}",
                correlationId, request.sensorId(), request.impactForce(), request.location());

        try {
            alertClient.sendCriticalAlert(
                    request.sensorId(),
                    request.impactForce(),
                    String.format(
                            "Seismic Door Alert: Sensor %s at location %s detected HIGH IMPACT FORCE (%.2f). Details: %s",
                            request.sensorId(),
                            request.location(),
                            request.impactForce(),
                            request.details()),
                    correlationId
            );
        } catch (Exception e) {
            LOGGER.error("cid={} failed to send critical seismic alert", correlationId, e);
        }
    }
}

