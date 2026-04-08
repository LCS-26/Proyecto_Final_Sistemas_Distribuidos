package org.toolset.grupo1.access.api;

public record AccessDecisionResponse(
        String username,
        String area,
        boolean granted,
        String reason
) {
}

