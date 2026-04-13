package org.toolset.grupo1.temperature.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Bean con ámbito Prototype para construir el contexto de eventos de temperatura.
 *
 * Conceptos de los apuntes demostrados:
 * - @Scope("prototype"): nueva instancia por cada solicitud al contenedor IoC
 * - @PostConstruct: inicialización tras inyección de dependencias
 * - @PreDestroy: limpieza antes de destrucción del bean
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AlertEventBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertEventBuilder.class);

    private String sensorType;
    private String source;
    private double value;
    private String message;

    @PostConstruct
    public void init() {
        LOGGER.debug("event=alert_builder_created source=temperature-service " +
                "message='Nueva instancia de AlertEventBuilder creada (Prototype scope)'");
    }

    @PreDestroy
    public void destroy() {
        LOGGER.debug("event=alert_builder_destroyed source=temperature-service " +
                "message='AlertEventBuilder siendo destruido'");
    }

    public AlertEventBuilder withSensorType(String sensorType) {
        this.sensorType = sensorType;
        return this;
    }

    public AlertEventBuilder withSource(String source) {
        this.source = source;
        return this;
    }

    public AlertEventBuilder withValue(double value) {
        this.value = value;
        return this;
    }

    public AlertEventBuilder withMessage(String message) {
        this.message = message;
        return this;
    }

    public String getSensorType() { return sensorType; }
    public String getSource() { return source; }
    public double getValue() { return value; }
    public String getMessage() { return message; }
}
