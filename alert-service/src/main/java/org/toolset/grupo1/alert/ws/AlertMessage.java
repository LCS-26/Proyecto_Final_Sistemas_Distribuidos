package org.toolset.grupo1.alert.ws;

import java.time.Instant;

public record AlertMessage(
        String severity,
        String message,
        String sensorType,
        String source,
        double value,
        Instant timestamp
) {
}

