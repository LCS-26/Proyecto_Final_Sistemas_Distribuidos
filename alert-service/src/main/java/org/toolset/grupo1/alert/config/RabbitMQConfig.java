package org.toolset.grupo1.alert.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el alert-service (consumidor de mensajes).
 *
 * Topología de mensajería:
 *   Sensores → DirectExchange (stark.alerts.exchange)
 *              → routing key: stark.alert.critical
 *              → Queue (stark.alerts.queue)
 *              → AlertConsumer (@RabbitListener)
 *
 * Conceptos de los apuntes demostrados:
 * - @Configuration: clase de configuración Spring (IoC / DI programático)
 * - @Bean: declara beans gestionados por el contenedor IoC
 * - @PostConstruct: se ejecuta UNA VEZ justo después de que Spring inyecta
 *   todas las dependencias del bean (lifecycle callback)
 * - @PreDestroy: se ejecuta justo antes de que el contenedor destruya el bean
 *   (útil para liberar recursos)
 */
@Configuration
public class RabbitMQConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConfig.class);

    public static final String QUEUE_NAME    = "stark.alerts.queue";
    public static final String EXCHANGE_NAME = "stark.alerts.exchange";
    public static final String ROUTING_KEY   = "stark.alert.critical";

    /**
     * Cola durable: sobrevive a reinicios del broker.
     * Los mensajes se acumulan hasta que AlertConsumer los procesa.
     * Solo se declara cuando stark.amqp.enabled=true (evita conexión en tests E2E).
     */
    @Bean
    @ConditionalOnProperty(name = "stark.amqp.enabled", havingValue = "true", matchIfMissing = true)
    public Queue alertQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    /**
     * DirectExchange: enruta mensajes a la cola cuyo binding key coincida
     * exactamente con el routing key del mensaje.
     */
    @Bean
    @ConditionalOnProperty(name = "stark.amqp.enabled", havingValue = "true", matchIfMissing = true)
    public DirectExchange alertExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    /**
     * Binding: vincula la cola al exchange mediante la routing key.
     * Solo los mensajes con key "stark.alert.critical" llegan a esta cola.
     */
    @Bean
    @ConditionalOnProperty(name = "stark.amqp.enabled", havingValue = "true", matchIfMissing = true)
    public Binding alertBinding(Queue alertQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(alertQueue).to(alertExchange).with(ROUTING_KEY);
    }

    /**
     * Convertidor Jackson para serializar/deserializar mensajes AMQP como JSON.
     * Usa TypePrecedence.INFERRED para deserializar por el tipo del parámetro
     * del @RabbitListener, ignorando el header __TypeId__ del mensaje.
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(
                org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    /**
     * @PostConstruct: lifecycle callback — se ejecuta tras la inyección de dependencias.
     * Aquí registramos que el sistema de mensajería está listo para recibir alertas.
     */
    @PostConstruct
    public void onStartup() {
        LOGGER.info("event=rabbitmq_ready source=alert-service queue={} exchange={} routingKey={}",
                QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
        LOGGER.info("event=alert_consumer_initialized source=alert-service " +
                "message='Sistema de mensajeria RabbitMQ configurado y listo para recibir alertas criticas'");
    }

    /**
     * @PreDestroy: lifecycle callback — se ejecuta antes de que Spring destruya el bean.
     * Aquí registramos el cierre limpio del sistema de mensajería.
     */
    @PreDestroy
    public void onShutdown() {
        LOGGER.info("event=rabbitmq_shutdown source=alert-service " +
                "message='Cerrando sistema de mensajeria RabbitMQ del alert-service'");
    }
}
