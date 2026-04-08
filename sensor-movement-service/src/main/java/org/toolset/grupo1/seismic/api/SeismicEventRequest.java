package org.toolset.grupo1.seismic.api;

public record SeismicEventRequest(
        String sensorId,
        String location,
        double impactForce,
        String details
) {
}

