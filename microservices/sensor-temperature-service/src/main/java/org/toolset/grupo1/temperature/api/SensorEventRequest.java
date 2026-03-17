package org.toolset.grupo1.temperature.api;

public record SensorEventRequest(
        String source,
        double value,
        String details
) {
}

