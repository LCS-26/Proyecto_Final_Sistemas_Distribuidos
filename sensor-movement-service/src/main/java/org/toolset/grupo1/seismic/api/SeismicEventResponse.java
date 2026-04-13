package org.toolset.grupo1.seismic.api;

import java.time.Instant;

public record SeismicEventResponse(
        String sensorType,
        String sensorId,
        String location,
        double impactForce,
        String details,
        boolean critical,
        Instant timestamp
) {
}

