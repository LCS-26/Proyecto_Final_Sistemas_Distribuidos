package org.toolset.grupo1.alert.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.alert.api.AlertRequest;
import org.toolset.grupo1.alert.config.RabbitMQConfig;
import org.toolset.grupo1.alert.service.AlertService;
import org.toolset.grupo1.alert.ws.AlertNotificationService;

/**
 * Consumidor RabbitMQ que procesa alertas críticas publicadas por los sensores.
 *
 * Flujo de mensajería (camino AMQP):
 *   Sensor → AmqpAlertPublisher → RabbitMQ Exchange → Queue → AlertConsumer
 *          → AlertService (persistencia JPA) + AlertNotificationService (WebSocket)
 *
 * @ConditionalOnProperty: solo activo cuando stark.amqp.enabled=true (por defecto).
 * En tests E2E sin broker RabbitMQ, se desactiva para evitar errores de conexión.
 *
 * Conceptos de los apuntes demostrados:
 * - @RabbitListener: procesa mensajes de la cola de forma asíncrona
 * - @Service: bean Singleton gestionado por el contenedor IoC
 * - Integración RabbitMQ con Spring AMQP (spring-boot-starter-amqp)
 */
@Service
@ConditionalOnProperty(name = "stark.amqp.enabled", havingValue = "true", matchIfMissing = true)
public class AlertConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertConsumer.class);

    private final AlertService alertService;
    private final AlertNotificationService alertNotificationService;

    public AlertConsumer(AlertService alertService, AlertNotificationService alertNotificationService) {
        this.alertService = alertService;
        this.alertNotificationService = alertNotificationService;
    }

    /**
     * Escucha la cola RabbitMQ y procesa cada mensaje de alerta recibido.
     * El Jackson2JsonMessageConverter deserializa el JSON al tipo AlertAmqpMessage
     * usando TypePrecedence.INFERRED (tipo inferido del parámetro del método).
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveAlert(AlertAmqpMessage amqpMessage) {
        String cid = amqpMessage.correlationId() != null ? amqpMessage.correlationId() : "unknown";
        LOGGER.info("event=alert_amqp_received source=alert-service cid={} sensorType={} sensorSource={} severity={}",
                cid, amqpMessage.sensorType(), amqpMessage.source(), amqpMessage.severity());

        AlertRequest request = new AlertRequest(
                amqpMessage.severity(),
                amqpMessage.message(),
                amqpMessage.sensorType(),
                amqpMessage.source(),
                amqpMessage.value(),
                amqpMessage.timestamp()
        );

        alertService.save(request, cid);
        alertNotificationService.publish(request, cid);

        LOGGER.info("event=alert_amqp_processed source=alert-service cid={} sensorType={} sensorSource={}",
                cid, amqpMessage.sensorType(), amqpMessage.source());
    }
}
