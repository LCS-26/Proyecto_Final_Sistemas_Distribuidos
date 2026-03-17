package org.toolset.grupo1.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.toolset.grupo1.security.domain.SensorType;

public record SensorEventRequest(
        @NotNull SensorType type,
        @NotBlank String source,
        double value,
        String details
) {
}

