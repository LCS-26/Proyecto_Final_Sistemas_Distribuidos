package org.toolset.grupo1.security.processing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.toolset.grupo1.security.domain.SecurityAlert;
import org.toolset.grupo1.security.domain.SensorEvent;
import org.toolset.grupo1.security.domain.SensorType;
import org.toolset.grupo1.security.repo.SecurityAlertRepository;
import org.toolset.grupo1.security.repo.SensorEventRepository;
import org.toolset.grupo1.security.ws.AlertNotificationService;

@Service
public class EventProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessingService.class);

    private final SensorEventRepository sensorEventRepository;
    private final SecurityAlertRepository securityAlertRepository;
    private final AlertNotificationService alertNotificationService;
    private final Counter processedCounter;
    private final Counter criticalCounter;

    public EventProcessingService(
            SensorEventRepository sensorEventRepository,
            SecurityAlertRepository securityAlertRepository,
            AlertNotificationService alertNotificationService,
            MeterRegistry meterRegistry) {
        this.sensorEventRepository = sensorEventRepository;
        this.securityAlertRepository = securityAlertRepository;
        this.alertNotificationService = alertNotificationService;
        this.processedCounter = meterRegistry.counter("security.events.processed");
        this.criticalCounter = meterRegistry.counter("security.events.critical");
    }

    @Async("sensorTaskExecutor")
    @Transactional
    public CompletableFuture<Void> processEvent(Long eventId) {
        Optional<SensorEvent> eventOpt = sensorEventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            LOGGER.warn("eventId={} not found during asynchronous processing", eventId);
            return CompletableFuture.completedFuture(null);
        }

        SensorEvent event = eventOpt.get();
        processedCounter.increment();

        if (isCritical(event)) {
            event.markCritical();
            SecurityAlert alert = new SecurityAlert("HIGH", buildAlertMessage(event), event.getId());
            securityAlertRepository.save(alert);
            criticalCounter.increment();
            alertNotificationService.publish(alert);
            LOGGER.warn("eventId={} marked as CRITICAL for sensor={} source={}", event.getId(), event.getType(), event.getSource());
        } else {
            LOGGER.info("eventId={} processed with no critical threshold", event.getId());
        }

        return CompletableFuture.completedFuture(null);
    }

    private boolean isCritical(SensorEvent event) {
        if (event.getType() == SensorType.MOVEMENT) {
            return event.getValue() >= 1;
        }
        if (event.getType() == SensorType.TEMPERATURE) {
            return event.getValue() >= 60;
        }
        return event.getType() == SensorType.ACCESS && event.getValue() <= 0;
    }

    private String buildAlertMessage(SensorEvent event) {
        return "Critical " + event.getType() + " event from " + event.getSource() + " (value=" + event.getValue() + ")";
    }
}

