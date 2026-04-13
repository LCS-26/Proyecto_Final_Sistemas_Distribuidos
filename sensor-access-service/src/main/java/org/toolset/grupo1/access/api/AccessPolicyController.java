package org.toolset.grupo1.access.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.toolset.grupo1.access.service.AccessControlService;

@RestController
@RequestMapping("/api/access")
public class AccessPolicyController {

    private final AccessControlService accessControlService;

    public AccessPolicyController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @PostMapping("/check")
    public AccessDecisionResponse check(@RequestBody AccessRequest request) {
        return accessControlService.evaluate(request);
    }
}

