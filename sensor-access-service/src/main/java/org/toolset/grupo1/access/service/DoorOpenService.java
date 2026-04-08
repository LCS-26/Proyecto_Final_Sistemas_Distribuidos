package org.toolset.grupo1.access.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.access.api.DoorEventRequest;
import org.toolset.grupo1.access.api.DoorEventResponse;

@Service
public class DoorOpenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoorOpenService.class);

    private final CopyOnWriteArrayList<DoorEventResponse> events = new CopyOnWriteArrayList<>();
    private final DoorOpenAsyncAlertService doorOpenAsyncAlertService;

    public DoorOpenService(DoorOpenAsyncAlertService doorOpenAsyncAlertService) {
        this.doorOpenAsyncAlertService = doorOpenAsyncAlertService;
    }

    public DoorEventResponse processDoorOpenEvent(DoorEventRequest request, String correlationId) {
        LOGGER.info("event=sensor_processing source=access-service cid={} sensorSource={} location={}",
                correlationId, request.doorId(), request.location());

        boolean critical = request.isOpen();

        LOGGER.info("event=sensor_decision source=access-service cid={} sensorSource={} critical={} threshold=open=true",
                correlationId, request.doorId(), critical);

        DoorEventResponse response = new DoorEventResponse(
                "DOOR_OPEN",
                request.doorId(),
                request.location(),
                request.isOpen(),
                request.details(),
                critical,
                Instant.now()
        );

        events.add(response);

        if (critical) {
            doorOpenAsyncAlertService.sendCriticalAlert(request, correlationId);
        }

        return response;
    }


    public List<DoorEventResponse> getLatestEvents() {
        List<DoorEventResponse> latest = new ArrayList<>(events);
        Collections.reverse(latest);
        return latest;
    }
}
