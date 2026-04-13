package org.toolset.grupo1.temperature.publisher;

/**
 * Interfaz que abstrae el mecanismo de publicación de alertas críticas de temperatura.
 *
 * Implementaciones:
 * - AmqpAlertPublisher (@Primary): publica via RabbitMQ (camino principal)
 * - HttpAlertPublisher (@Qualifier("httpAlertPublisher")): publica via HTTP REST (fallback)
 *
 * Conceptos de los apuntes:
 * - @Primary y @Qualifier para resolución de conflictos entre beans del mismo tipo
 * - IoC / DI: inversión de dependencias hacia la abstracción, no la implementación
 */
public interface AlertPublisher {

    void publish(String source, double value, String message, String correlationId);
}
