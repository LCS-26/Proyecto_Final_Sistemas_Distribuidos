package org.toolset.grupo1.access.api;

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
import org.toolset.grupo1.access.service.DoorOpenService;

@RestController
@RequestMapping("/api/door-open")
public class DoorOpenController {

    private final DoorOpenService doorOpenService;

    public DoorOpenController(DoorOpenService doorOpenService) {
        this.doorOpenService = doorOpenService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DoorEventResponse registerDoorOpen(@RequestBody DoorEventRequest request) {
        return doorOpenService.processDoorOpenEvent(request);
    }

    @GetMapping("/events")
    public List<DoorEventResponse> getLatestDoorEvents() {
        return doorOpenService.getLatestEvents();
    }
}

