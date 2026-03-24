package org.toolset.grupo1.access.api;

public record DoorEventRequest(
        String doorId,
        String location,
        boolean isOpen,
        String details
) {
}

