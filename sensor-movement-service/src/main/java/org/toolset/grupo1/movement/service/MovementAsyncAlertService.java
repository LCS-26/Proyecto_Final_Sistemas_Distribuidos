package org.toolset.grupo1.movement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.movement.api.SensorEventRequest;
import org.toolset.grupo1.movement.publisher.AlertPublisher;

/**
 * Servicio asíncrono para publicar alertas críticas de movimiento.
 *
 * Usa @Async para ejecutar el envío de alertas en un hilo separado del pool
 * configurado en AsyncConfig (ThreadPoolTaskExecutor), evitando bloquear
 * el hilo HTTP que procesa la petición del sensor.
 *
 * Conceptos de los apuntes demostrados:
 * - @Async: ejecuta el método en un hilo del pool de ExecutorService
 * - AlertPublisher @Primary (AmqpAlertPublisher): Spring inyecta automáticamente
 *   la implementación marcada como @Primary cuando hay múltiples beans AlertPublisher.
 *   Si RabbitMQ falla, AmqpAlertPublisher hace fallback a HttpAlertPublisher (@Qualifier).
 */
@Service
public class MovementAsyncAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementAsyncAlertService.class);

    private final AlertPublisher alertPublisher;

    public MovementAsyncAlertService(AlertPublisher alertPublisher) {
        this.alertPublisher = alertPublisher;
    }

    @Async
    public void sendCriticalAlert(SensorEventRequest request, String correlationId) {
        LOGGER.info("event=alert_prepare source=movement-service cid={} sensorSource={} intensity={}",
                correlationId, request.source(), request.value());
        try {
            alertPublisher.publish(request.source(), request.value(), "Movement detected", correlationId);
        } catch (Exception e) {
            LOGGER.error("cid={} failed to send critical movement alert", correlationId, e);
        }
    }
}
