package org.toolset.grupo1.alert.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.toolset.grupo1.alert.ws.AlertNotificationService;

@RestController
@RequestMapping
public class AlertController {

    private final CopyOnWriteArrayList<AlertRequest> alerts = new CopyOnWriteArrayList<>();
    private final AlertNotificationService alertNotificationService;

    public AlertController(AlertNotificationService alertNotificationService) {
        this.alertNotificationService = alertNotificationService;
    }

    @PostMapping("/internal/alerts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receiveAlert(@RequestBody AlertRequest request) {
        alerts.add(request);
        alertNotificationService.publish(request);
    }

    @GetMapping("/api/alerts")
    public List<AlertRequest> alerts() {
        List<AlertRequest> latest = new ArrayList<>(alerts);
        Collections.reverse(latest);
        return latest;
    }
}
