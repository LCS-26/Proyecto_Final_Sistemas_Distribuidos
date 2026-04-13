package org.toolset.grupo1.temperature.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.temperature.api.SensorEventRequest;
import org.toolset.grupo1.temperature.publisher.AlertPublisher;

/**
 * Servicio asíncrono para publicar alertas críticas de temperatura.
 *
 * Separa la lógica de publicación de alertas del controlador HTTP,
 * ejecutándola en un hilo del pool (AsyncConfig / ThreadPoolTaskExecutor)
 * para no bloquear el hilo de la petición entrante.
 *
 * Conceptos de los apuntes demostrados:
 * - @Async: ejecución en hilo separado del pool ExecutorService
 * - @Service: bean Singleton gestionado por el contenedor IoC
 * - AlertPublisher: inyección del bean @Primary (AmqpAlertPublisher → RabbitMQ)
 *   con fallback a HttpAlertPublisher cuando RabbitMQ no está disponible
 */
@Service
public class TemperatureAsyncAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemperatureAsyncAlertService.class);

    private final AlertPublisher alertPublisher;

    public TemperatureAsyncAlertService(AlertPublisher alertPublisher) {
        this.alertPublisher = alertPublisher;
    }

    @Async
    public void sendCriticalAlert(SensorEventRequest request, String correlationId) {
        LOGGER.info("event=alert_prepare source=temperature-service cid={} sensorSource={} temperature={}",
                correlationId, request.source(), request.value());
        try {
            alertPublisher.publish(request.source(), request.value(), "Overheat detected", correlationId);
        } catch (Exception e) {
            LOGGER.error("cid={} failed to send critical temperature alert", correlationId, e);
        }
    }
}
