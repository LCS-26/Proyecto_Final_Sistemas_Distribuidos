package org.toolset.grupo1.access.api;

import java.time.Instant;
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
import org.toolset.grupo1.access.client.AlertClient;

@RestController
@RequestMapping("/api/events")
public class AccessController {

    private final CopyOnWriteArrayList<SensorEventResponse> events = new CopyOnWriteArrayList<>();
    private final AlertClient alertClient;

    public AccessController(AlertClient alertClient) {
        this.alertClient = alertClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SensorEventResponse ingest(@RequestBody SensorEventRequest request) {
        boolean critical = request.value() <= 0.0;
        SensorEventResponse response = new SensorEventResponse(
                "ACCESS",
                request.source(),
                request.value(),
                request.details(),
                critical,
                Instant.now());
        events.add(response);

        if (critical) {
            alertClient.sendCriticalAlert(request.source(), request.value(), "Access denied or invalid badge");
        }

        return response;
    }

    @GetMapping
    public List<SensorEventResponse> latest() {
        List<SensorEventResponse> latest = new ArrayList<>(events);
        Collections.reverse(latest);
        return latest;
    }
}
