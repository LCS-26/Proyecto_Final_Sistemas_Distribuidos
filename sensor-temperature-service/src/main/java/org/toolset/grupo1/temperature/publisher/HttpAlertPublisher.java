package org.toolset.grupo1.temperature.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.toolset.grupo1.temperature.client.AlertClient;

/**
 * Implementación HTTP de AlertPublisher para alertas de temperatura.
 *
 * Publica alertas via REST al alert-service. Actúa como fallback cuando RabbitMQ
 * no está disponible, y puede seleccionarse explícitamente con @Qualifier.
 *
 * Conceptos de los apuntes:
 * - @Qualifier("httpAlertPublisher"): identificador para selección explícita de bean
 * - @Component: bean Singleton en el contenedor IoC de Spring
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
        LOGGER.info("event=alert_http_published source=temperature-service cid={} sensorSource={} value={}",
                correlationId, source, value);
        alertClient.sendCriticalAlert(source, value, message, correlationId);
    }
}
