package org.toolset.grupo1.movement.api;

public record SensorEventRequest(
        String source,
        double value,
        String details
) {
}

