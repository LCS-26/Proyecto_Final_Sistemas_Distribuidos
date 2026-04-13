package org.toolset.grupo1.movement.publisher;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.toolset.grupo1.movement.config.RabbitMQConfig;

/**
 * Implementación PRINCIPAL de AlertPublisher que publica alertas via RabbitMQ (AMQP).
 *
 * Al estar anotado con @Primary, Spring lo inyecta automáticamente cuando se solicita
 * un bean de tipo AlertPublisher sin especificar @Qualifier.
 *
 * Estrategia de resiliencia: si RabbitMQ no está disponible (por ejemplo, en tests E2E
 * que arrancan los servicios sin broker), la excepción se captura y se hace fallback
 * al HttpAlertPublisher para mantener la funcionalidad.
 *
 * @ConditionalOnProperty: este bean solo se crea cuando stark.amqp.enabled=true (por defecto).
 * En tests E2E (stark.amqp.enabled=false), HttpAlertPublisher actúa como único publisher.
 *
 * Conceptos de los apuntes demostrados:
 * - @Primary: bean preferido cuando hay múltiples candidatos del mismo tipo
 * - @Component: bean gestionado por el contenedor IoC de Spring (Singleton por defecto)
 * - RabbitMQ / Spring AMQP: publicación de mensajes JSON en un exchange
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
                    "HIGH", message, "MOVEMENT", source, value, Instant.now(), correlationId);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, amqpMessage);
            LOGGER.info("event=alert_amqp_published source=movement-service cid={} sensorSource={} value={}",
                    correlationId, source, value);
        } catch (Exception ex) {
            LOGGER.warn("event=alert_amqp_failed source=movement-service cid={} sensorSource={} error={} fallback=http",
                    correlationId, source, ex.getMessage());
            httpFallback.publish(source, value, message, correlationId);
        }
    }

    /**
     * DTO para serializar el mensaje AMQP como JSON.
     * Jackson (Jackson2JsonMessageConverter) convierte este record a JSON
     * antes de enviarlo al broker RabbitMQ.
     */
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
