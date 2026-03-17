package org.toolset.grupo1.security.api;

public record AccessDecisionResponse(
        String username,
        String area,
        boolean granted,
        String reason
) {
}

