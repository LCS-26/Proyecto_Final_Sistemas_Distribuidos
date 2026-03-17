package org.toolset.grupo1.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sensor_events")
public class SensorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SensorType type;

    @Column(nullable = false)
    private String source;

    @Column(name = "sensor_value", nullable = false)
    private double value;

    @Column(nullable = false)
    private boolean critical;

    @Column(nullable = false)
    private String details;

    protected SensorEvent() {
    }

    public SensorEvent(SensorType type, String source, double value, String details) {
        this.receivedAt = Instant.now();
        this.type = type;
        this.source = source;
        this.value = value;
        this.details = details;
        this.critical = false;
    }

    public Long getId() {
        return id;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public SensorType getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public double getValue() {
        return value;
    }

    public boolean isCritical() {
        return critical;
    }

    public String getDetails() {
        return details;
    }

    public void markCritical() {
        this.critical = true;
    }
}

