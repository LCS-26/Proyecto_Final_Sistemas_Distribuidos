package org.toolset.grupo1.access.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.toolset.grupo1.access.client.AlertClient;

/**
 * Implementación HTTP de AlertPublisher para alertas de acceso.
 *
 * Publica alertas via REST al alert-service. Actúa como fallback cuando RabbitMQ
 * no está disponible, y puede seleccionarse explícitamente con @Qualifier.
 */
@Component
@Qualifier("httpAlertPublisher")
public class HttpAlertPublisher implements AlertPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAlertPublisher.class);

    private final AlertClient alertClient;

    public HttpAlertPublisher(AlertClient alertClient) {
        this.alertClient = alertClient;
    }

    @Override
    public void publish(String source, double value, String message, String correlationId) {
        LOGGER.info("event=alert_http_published source=access-service cid={} sensorSource={} value={}",
                correlationId, source, value);
        alertClient.sendCriticalAlert(source, value, message, correlationId);
    }
}
