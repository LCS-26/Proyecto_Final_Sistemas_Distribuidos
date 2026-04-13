package org.toolset.grupo1.alert.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entidad JPA que representa una alerta persistida en base de datos.
 *
 * Conceptos de los apuntes demostrados:
 * - Spring Data JPA: mapeo objeto-relacional con @Entity y @Table
 * - Connection Pooling: Spring Boot configura HikariCP automáticamente
 * - @Transactional: las operaciones de guardado se ejecutan en transacciones (ver AlertService)
 */
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String sensorType;

    @Column(nullable = false)
    private String source;

    @Column(name = "sensor_value")
    private double value;

    @Column(nullable = false)
    private Instant timestamp;

    @Column
    private String correlationId;

    protected Alert() {
    }

    public Alert(String severity, String message, String sensorType,
                 String source, double value, Instant timestamp, String correlationId) {
        this.severity = severity;
        this.message = message;
        this.sensorType = sensorType;
        this.source = source;
        this.value = value;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }

    public Long getId() { return id; }
    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getSensorType() { return sensorType; }
    public String getSource() { return source; }
    public double getValue() { return value; }
    public Instant getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
}
