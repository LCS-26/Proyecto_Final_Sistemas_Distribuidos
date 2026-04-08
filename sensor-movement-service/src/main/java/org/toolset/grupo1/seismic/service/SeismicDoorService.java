package org.toolset.grupo1.seismic.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.seismic.api.SeismicEventRequest;
import org.toolset.grupo1.seismic.api.SeismicEventResponse;

@Service
public class SeismicDoorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeismicDoorService.class);

    private static final double CRITICAL_IMPACT_THRESHOLD = 7.0;  // Force threshold for critical alert

    private final CopyOnWriteArrayList<SeismicEventResponse> events = new CopyOnWriteArrayList<>();
    private final SeismicAsyncAlertService seismicAsyncAlertService;

    public SeismicDoorService(SeismicAsyncAlertService seismicAsyncAlertService) {
        this.seismicAsyncAlertService = seismicAsyncAlertService;
    }

    public SeismicEventResponse processSeismicEvent(SeismicEventRequest request, String correlationId) {
        LOGGER.info("event=sensor_processing source=seismic-service cid={} sensorSource={} impactForce={}",
                correlationId, request.sensorId(), request.impactForce());

        boolean critical = request.impactForce() >= CRITICAL_IMPACT_THRESHOLD;

        LOGGER.info("event=sensor_decision source=seismic-service cid={} sensorSource={} critical={} threshold={}",
                correlationId, request.sensorId(), critical, CRITICAL_IMPACT_THRESHOLD);

        SeismicEventResponse response = new SeismicEventResponse(
                "SEISMIC_DOOR",
                request.sensorId(),
                request.location(),
                request.impactForce(),
                request.details(),
                critical,
                Instant.now()
        );

        events.add(response);

        if (critical) {
            seismicAsyncAlertService.sendCriticalAlert(request, correlationId);
        }

        return response;
    }


    public List<SeismicEventResponse> getLatestEvents() {
        List<SeismicEventResponse> latest = new ArrayList<>(events);
        Collections.reverse(latest);
        return latest;
    }
}
