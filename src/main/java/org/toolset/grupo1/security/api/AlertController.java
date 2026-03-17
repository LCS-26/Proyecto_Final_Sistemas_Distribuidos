package org.toolset.grupo1.security.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.toolset.grupo1.security.domain.SecurityAlert;
import org.toolset.grupo1.security.processing.AlertQueryService;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertQueryService alertQueryService;

    public AlertController(AlertQueryService alertQueryService) {
        this.alertQueryService = alertQueryService;
    }

    @GetMapping
    public List<SecurityAlert> latestAlerts() {
        return alertQueryService.latestAlerts();
    }
}

