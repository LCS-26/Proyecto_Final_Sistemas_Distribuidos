package org.toolset.grupo1.temperature.api;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.toolset.grupo1.temperature.client.AlertClient;

@RestController
@RequestMapping("/api/events")
public class TemperatureController {

    private final CopyOnWriteArrayList<SensorEventResponse> events = new CopyOnWriteArrayList<>();
    private final AlertClient alertClient;

    public TemperatureController(AlertClient alertClient) {
        this.alertClient = alertClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SensorEventResponse ingest(@RequestBody SensorEventRequest request) {
        boolean critical = request.value() >= 60.0;
        SensorEventResponse response = new SensorEventResponse(
                "TEMPERATURE",
                request.source(),
                request.value(),
                request.details(),
                critical,
                Instant.now());
        events.add(response);

        if (critical) {
            alertClient.sendCriticalAlert(request.source(), request.value(), "Overheat detected");
        }

        return response;
    }

    @GetMapping
    public List<SensorEventResponse> latest() {
        return events.reversed();
    }
}

