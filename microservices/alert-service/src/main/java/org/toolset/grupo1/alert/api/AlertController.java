package org.toolset.grupo1.alert.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AlertController {

    private final CopyOnWriteArrayList<AlertRequest> alerts = new CopyOnWriteArrayList<>();

    @PostMapping("/internal/alerts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receiveAlert(@RequestBody AlertRequest request) {
        alerts.add(request);
    }

    @GetMapping("/api/alerts")
    public List<AlertRequest> alerts() {
        return alerts.reversed();
    }
}

