package org.toolset.grupo1.movement.publisher;

/**
 * Interfaz que abstrae el mecanismo de publicación de alertas críticas.
 *
 * Permite tener múltiples implementaciones:
 * - AmqpAlertPublisher (@Primary): publica via RabbitMQ (camino principal)
 * - HttpAlertPublisher (@Qualifier): publica via HTTP REST (fallback / legado)
 *
 * Conceptos de los apuntes demostrados:
 * - @Primary y @Qualifier: resolución de conflictos cuando hay múltiples beans
 *   del mismo tipo. Spring inyecta el @Primary por defecto; @Qualifier permite
 *   seleccionar una implementación concreta explícitamente.
 * - IoC / DI: los servicios dependen de esta interfaz, no de implementaciones concretas
 *   (principio de inversión de dependencias)
 */
public interface AlertPublisher {

    void publish(String source, double value, String message, String correlationId);
}
