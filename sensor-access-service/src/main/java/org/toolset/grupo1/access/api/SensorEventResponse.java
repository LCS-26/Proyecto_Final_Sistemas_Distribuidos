package org.toolset.grupo1.access.api;

import java.time.Instant;

public record SensorEventResponse(
        String sensorType,
        String source,
        double value,
        String details,
        boolean critical,
        Instant timestamp
) {
}

