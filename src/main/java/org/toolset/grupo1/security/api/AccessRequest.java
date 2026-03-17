package org.toolset.grupo1.security.api;

import jakarta.validation.constraints.NotBlank;

public record AccessRequest(
        @NotBlank String username,
        @NotBlank String area,
        boolean badgeValid
) {
}

