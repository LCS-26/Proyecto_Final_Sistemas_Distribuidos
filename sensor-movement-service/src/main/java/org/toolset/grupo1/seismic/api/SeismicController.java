package org.toolset.grupo1.seismic.api;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.toolset.grupo1.seismic.service.SeismicDoorService;

@RestController
@RequestMapping("/api/seismic")
public class SeismicController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeismicController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final SeismicDoorService seismicDoorService;

    public SeismicController(SeismicDoorService seismicDoorService) {
        this.seismicDoorService = seismicDoorService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SeismicEventResponse registerSeismicEvent(
            @RequestBody SeismicEventRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=sensor_received source=seismic-service cid={} endpoint=/api/seismic/events sensorSource={} impactForce={} details={}",
                cid, request.sensorId(), request.impactForce(), request.details());
        return seismicDoorService.processSeismicEvent(request, cid);
    }

    @GetMapping("/events")
    public List<SeismicEventResponse> getLatestSeismicEvents() {
        return seismicDoorService.getLatestEvents();
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        return incomingCorrelationId;
    }
}

