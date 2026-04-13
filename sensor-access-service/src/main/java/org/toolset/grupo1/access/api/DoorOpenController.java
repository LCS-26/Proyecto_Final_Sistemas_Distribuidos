package org.toolset.grupo1.access.api;

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
import org.toolset.grupo1.access.service.DoorOpenService;

@RestController
@RequestMapping("/api/door-open")
public class DoorOpenController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoorOpenController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final DoorOpenService doorOpenService;

    public DoorOpenController(DoorOpenService doorOpenService) {
        this.doorOpenService = doorOpenService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DoorEventResponse registerDoorOpen(
            @RequestBody DoorEventRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=sensor_received source=access-service cid={} endpoint=/api/door-open/events sensorSource={} open={} details={}",
                cid, request.doorId(), request.isOpen(), request.details());
        return doorOpenService.processDoorOpenEvent(request, cid);
    }

    @GetMapping("/events")
    public List<DoorEventResponse> getLatestDoorEvents() {
        return doorOpenService.getLatestEvents();
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        return incomingCorrelationId;
    }
}

