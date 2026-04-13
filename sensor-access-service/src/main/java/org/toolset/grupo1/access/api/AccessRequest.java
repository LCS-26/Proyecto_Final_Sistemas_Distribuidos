package org.toolset.grupo1.access.api;

public record AccessRequest(
        String username,
        String area,
        boolean badgeValid
) {
}

