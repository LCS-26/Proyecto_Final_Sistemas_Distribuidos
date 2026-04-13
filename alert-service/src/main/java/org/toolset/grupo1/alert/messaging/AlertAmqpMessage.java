package org.toolset.grupo1.alert.messaging;

import java.time.Instant;

/**
 * DTO para deserializar mensajes de alerta recibidos por RabbitMQ desde los sensores.
 *
 * Los sensores (movement, temperature, access) publican mensajes JSON con esta estructura
 * en el exchange "stark.alerts.exchange". El AlertConsumer los deserializa a este record
 * usando el Jackson2JsonMessageConverter configurado en RabbitMQConfig.
 */
public record AlertAmqpMessage(
        String severity,
        String message,
        String sensorType,
        String source,
        double value,
        Instant timestamp,
        String correlationId
) {
}
