package org.toolset.grupo1.security.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.toolset.grupo1.security.processing.AccessControlService;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final AccessControlService accessControlService;

    public AccessController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @PostMapping("/check")
    public AccessDecisionResponse check(@Valid @RequestBody AccessRequest request) {
        return accessControlService.evaluate(request);
    }
}

