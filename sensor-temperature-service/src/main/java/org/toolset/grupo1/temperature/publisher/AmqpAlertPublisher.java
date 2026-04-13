package org.toolset.grupo1.temperature.publisher;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.toolset.grupo1.temperature.config.RabbitMQConfig;

/**
 * Implementación PRINCIPAL de AlertPublisher que publica alertas de temperatura via RabbitMQ.
 *
 * @Primary indica a Spring que este bean es el candidato preferido cuando se inyecta
 * AlertPublisher sin especificar @Qualifier. Si RabbitMQ no está disponible,
 * hace fallback al HttpAlertPublisher (@Qualifier("httpAlertPublisher")).
 *
 * @ConditionalOnProperty: solo activo cuando stark.amqp.enabled=true (por defecto).
 *
 * Conceptos de los apuntes:
 * - @Primary: bean predeterminado para resolución de conflictos entre múltiples candidatos
 * - @Component: bean Singleton gestionado por el contenedor IoC
 * - Spring AMQP: RabbitTemplate con Jackson2JsonMessageConverter
 */
@Primary
@Component
@ConditionalOnProperty(name = "stark.amqp.enabled", havingValue = "true", matchIfMissing = true)
public class AmqpAlertPublisher implements AlertPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpAlertPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final AlertPublisher httpFallback;

    public AmqpAlertPublisher(
            RabbitTemplate rabbitTemplate,
            @Qualifier("httpAlertPublisher") AlertPublisher httpFallback) {
        this.rabbitTemplate = rabbitTemplate;
        this.httpFallback = httpFallback;
    }

    @Override
    public void publish(String source, double value, String message, String correlationId) {
        try {
            AlertAmqpMessage amqpMessage = new AlertAmqpMessage(
                    "HIGH", message, "TEMPERATURE", source, value, Instant.now(), correlationId);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, amqpMessage);
            LOGGER.info("event=alert_amqp_published source=temperature-service cid={} sensorSource={} value={}",
                    correlationId, source, value);
        } catch (Exception ex) {
            LOGGER.warn("event=alert_amqp_failed source=temperature-service cid={} sensorSource={} error={} fallback=http",
                    correlationId, source, ex.getMessage());
            httpFallback.publish(source, value, message, correlationId);
        }
    }

    private record AlertAmqpMessage(
            String severity,
            String message,
            String sensorType,
            String source,
            double value,
            Instant timestamp,
            String correlationId
    ) {}
}
