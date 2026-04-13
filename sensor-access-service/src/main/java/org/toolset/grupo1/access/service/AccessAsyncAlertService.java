package org.toolset.grupo1.access.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.access.api.SensorEventRequest;
import org.toolset.grupo1.access.publisher.AlertPublisher;

/**
 * Servicio asíncrono para publicar alertas críticas del sensor de acceso.
 *
 * Separa la publicación de alertas del controlador HTTP, ejecutándola en un
 * hilo del pool (AsyncConfig / ThreadPoolTaskExecutor), evitando bloquear
 * el hilo de la petición HTTP entrante.
 *
 * Conceptos de los apuntes demostrados:
 * - @Async: ejecución en hilo separado del pool ExecutorService
 * - @Service: bean Singleton gestionado por el contenedor IoC
 * - AlertPublisher: inyección del bean @Primary (AmqpAlertPublisher → RabbitMQ)
 */
@Service
public class AccessAsyncAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessAsyncAlertService.class);

    private final AlertPublisher alertPublisher;

    public AccessAsyncAlertService(AlertPublisher alertPublisher) {
        this.alertPublisher = alertPublisher;
    }

    @Async
    public void sendCriticalAlert(SensorEventRequest request, String correlationId) {
        LOGGER.info("event=alert_prepare source=access-service cid={} sensorSource={} value={}",
                correlationId, request.source(), request.value());
        try {
            alertPublisher.publish(request.source(), request.value(),
                    "Access denied or invalid badge", correlationId);
        } catch (Exception e) {
            LOGGER.error("cid={} failed to send critical access alert", correlationId, e);
        }
    }
}
