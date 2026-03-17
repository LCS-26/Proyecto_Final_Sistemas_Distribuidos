package org.toolset.grupo1.security.processing;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.toolset.grupo1.security.api.SensorEventRequest;
import org.toolset.grupo1.security.domain.SensorEvent;
import org.toolset.grupo1.security.domain.SensorType;
import org.toolset.grupo1.security.repo.SensorEventRepository;

@Service
public class SensorEventService {

    private final SensorEventRepository sensorEventRepository;
    private final EventProcessingService eventProcessingService;

    public SensorEventService(SensorEventRepository sensorEventRepository, EventProcessingService eventProcessingService) {
        this.sensorEventRepository = sensorEventRepository;
        this.eventProcessingService = eventProcessingService;
    }

    @Transactional
    public SensorEvent ingest(SensorEventRequest request) {
        SensorEvent event = new SensorEvent(
                request.type(),
                request.source(),
                request.value(),
                request.details() == null ? "" : request.details());
        SensorEvent stored = sensorEventRepository.save(event);
        eventProcessingService.processEvent(stored.getId());
        return stored;
    }

    @Transactional(readOnly = true)
    public List<SensorEvent> latestByType(SensorType type) {
        return sensorEventRepository.findTop20ByTypeOrderByReceivedAtDesc(type);
    }
}

