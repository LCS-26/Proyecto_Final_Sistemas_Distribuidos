package org.toolset.grupo1.movement.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.toolset.grupo1.movement.client.AlertClient;

/**
 * Implementación HTTP de AlertPublisher — publica alertas via REST al alert-service.
 *
 * Este bean actúa como fallback cuando RabbitMQ no está disponible, y también
 * como implementación alternativa seleccionable explícitamente con @Qualifier.
 *
 * Para inyectar esta implementación específicamente (en vez de AmqpAlertPublisher @Primary):
 *   @Autowired
 *   @Qualifier("httpAlertPublisher")
 *   private AlertPublisher publisher;
 *
 * Conceptos de los apuntes demostrados:
 * - @Qualifier: permite seleccionar un bean específico cuando hay múltiples del mismo tipo
 * - @Component: bean Singleton gestionado por el contenedor IoC
 * - Patrón Decorator/Adapter: envuelve AlertClient para cumplir la interfaz AlertPublisher
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
        LOGGER.info("event=alert_http_published source=movement-service cid={} sensorSource={} value={}",
                correlationId, source, value);
        alertClient.sendCriticalAlert(source, value, message, correlationId);
    }
}
