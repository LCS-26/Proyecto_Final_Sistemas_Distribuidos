package org.toolset.grupo1.seismic.api;

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
import org.toolset.grupo1.seismic.service.SeismicDoorService;

@RestController
@RequestMapping("/api/seismic")
public class SeismicController {

    private final SeismicDoorService seismicDoorService;

    public SeismicController(SeismicDoorService seismicDoorService) {
        this.seismicDoorService = seismicDoorService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SeismicEventResponse registerSeismicEvent(@RequestBody SeismicEventRequest request) {
        return seismicDoorService.processSeismicEvent(request);
    }

    @GetMapping("/events")
    public List<SeismicEventResponse> getLatestSeismicEvents() {
        return seismicDoorService.getLatestEvents();
    }
}

