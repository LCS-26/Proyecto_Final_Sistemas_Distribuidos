package org.toolset.grupo1.security.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.toolset.grupo1.security.domain.SensorEvent;
import org.toolset.grupo1.security.domain.SensorType;
import org.toolset.grupo1.security.processing.SensorEventService;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorEventService sensorEventService;

    public SensorController(SensorEventService sensorEventService) {
        this.sensorEventService = sensorEventService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SensorEvent ingest(@Valid @RequestBody SensorEventRequest request) {
        return sensorEventService.ingest(request);
    }

    @GetMapping("/events/{type}")
    public List<SensorEvent> latestByType(@PathVariable SensorType type) {
        return sensorEventService.latestByType(type);
    }
}

