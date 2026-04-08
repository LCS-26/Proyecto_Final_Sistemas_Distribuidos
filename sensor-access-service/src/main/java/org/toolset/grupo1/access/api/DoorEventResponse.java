package org.toolset.grupo1.access.api;

import java.time.Instant;

public record DoorEventResponse(
        String sensorType,
        String doorId,
        String location,
        boolean isOpen,
        String details,
        boolean critical,
        Instant timestamp
) {
}

