package org.toolset.grupo1.movement.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración RabbitMQ del sensor-movement-service (productor de mensajes).
 *
 * Define los nombres del exchange y routing key para publicar alertas críticas
 * en el broker. El alert-service declara la cola y el binding correspondiente.
 *
 * @ConditionalOnProperty: solo activo cuando stark.amqp.enabled=true (por defecto).
 * En tests, se excluye RabbitAutoConfiguration y esta clase no se procesa.
 *
 * Conceptos de los apuntes:
 * - @Configuration: clase de configuración IoC
 * - @Bean: declaración de beans gestionados por Spring
 * - Spring AMQP: integración con RabbitMQ mediante RabbitTemplate
 */
@Configuration
@ConditionalOnProperty(name = "stark.amqp.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "stark.alerts.exchange";
    public static final String ROUTING_KEY   = "stark.alert.critical";

    /**
     * Convertidor Jackson para serializar los mensajes como JSON al enviarlos al broker.
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    /**
     * Configura RabbitTemplate para usar el convertidor Jackson.
     * Spring Boot auto-configura un RabbitTemplate; aquí le asignamos el messageConverter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }
}
