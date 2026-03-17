package org.toolset.grupo1.security.ws;

import java.time.Instant;

public record AlertMessage(
        Long alertId,
        String severity,
        String message,
        Instant createdAt,
        Long eventId
) {
}

