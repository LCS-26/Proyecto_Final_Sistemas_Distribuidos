package org.toolset.grupo1.movement.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.toolset.grupo1.movement.service.MovementDetectionService;

@RestController
@RequestMapping("/api/events")
public class MovementController {

    private final MovementDetectionService movementDetectionService;

    public MovementController(MovementDetectionService movementDetectionService) {
        this.movementDetectionService = movementDetectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SensorEventResponse ingest(@RequestBody SensorEventRequest request) {
        return movementDetectionService.processMovementEvent(request);
    }

    @GetMapping
    public List<SensorEventResponse> latest() {
        return movementDetectionService.getLatestEvents();
    }
}

