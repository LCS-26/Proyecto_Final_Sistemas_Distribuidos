package org.toolset.grupo1.alert.api;

import java.time.Instant;

public record AlertRequest(
        String severity,
        String message,
        String sensorType,
        String source,
        double value,
        Instant timestamp
) {
}

