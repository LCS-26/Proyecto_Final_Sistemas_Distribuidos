package org.toolset.grupo1.movement.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Bean con ámbito Prototype que construye el contexto de un evento de alerta.
 *
 * A diferencia del ámbito Singleton (por defecto), Spring crea una NUEVA instancia
 * de este bean cada vez que se solicita con applicationContext.getBean() o ObjectProvider.
 *
 * Conceptos de los apuntes demostrados:
 * - @Scope("prototype"): nuevo objeto por cada solicitud al contenedor IoC.
 *   Contrasta con @Scope("singleton") donde hay una única instancia compartida.
 * - @PostConstruct: se ejecuta tras la inyección de dependencias, antes de usar el bean.
 *   Útil para validaciones o inicializaciones que requieren que las dependencias estén listas.
 * - @PreDestroy: se ejecuta antes de que el contenedor destruya el bean.
 *   NOTA: Para beans Prototype, Spring NO llama a @PreDestroy automáticamente;
 *   el bean owner debe gestionar el ciclo de vida.
 * - @Component: declaración del bean en el contenedor IoC
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AlertEventBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertEventBuilder.class);

    private String sensorType;
    private String source;
    private double value;
    private String message;

    /**
     * @PostConstruct: Spring lo llama justo después de crear la instancia e inyectar
     * dependencias. En un Prototype, esto ocurre cada vez que se crea una nueva instancia.
     */
    @PostConstruct
    public void init() {
        LOGGER.debug("event=alert_builder_created source=movement-service " +
                "message='Nueva instancia de AlertEventBuilder creada (Prototype scope)'");
    }

    /**
     * @PreDestroy: En beans Prototype, Spring NO gestiona el destroy automáticamente.
     * Este método quedaría a cargo del código que usa el bean.
     * En ámbito Singleton, Spring sí lo llama al cerrar el contexto.
     */
    @PreDestroy
    public void destroy() {
        LOGGER.debug("event=alert_builder_destroyed source=movement-service " +
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
