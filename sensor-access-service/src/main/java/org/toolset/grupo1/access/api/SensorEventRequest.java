package org.toolset.grupo1.access.api;

public record SensorEventRequest(
        String source,
        double value,
        String details
) {
}

