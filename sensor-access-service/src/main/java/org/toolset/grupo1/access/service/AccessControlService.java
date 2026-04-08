package org.toolset.grupo1.access.service;

import org.springframework.stereotype.Service;
import org.toolset.grupo1.access.api.AccessDecisionResponse;
import org.toolset.grupo1.access.api.AccessRequest;

@Service
public class AccessControlService {

    public AccessDecisionResponse evaluate(AccessRequest request) {
        if (!request.badgeValid()) {
            return new AccessDecisionResponse(request.username(), request.area(), false, "Invalid badge");
        }

        if (request.area().toLowerCase().contains("vault") && !request.username().startsWith("stark")) {
            return new AccessDecisionResponse(request.username(), request.area(), false, "Restricted area policy");
        }

        return new AccessDecisionResponse(request.username(), request.area(), true, "Access granted");
    }
}

