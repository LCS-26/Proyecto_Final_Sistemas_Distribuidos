package org.toolset.grupo1.alert.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.alert.api.AlertRequest;

@Service
public class AlertNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public AlertNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(AlertRequest request, String correlationId) {
        AlertMessage message = new AlertMessage(
                request.severity(),
                request.message(),
                request.sensorType(),
                request.source(),
                request.value(),
                request.timestamp());

        messagingTemplate.convertAndSend("/topic/alerts", message);
        LOGGER.info("event=alert_emitted source=alert-service cid={} topic=/topic/alerts sensorType={} sensorSource={} severity={}",
                correlationId, request.sensorType(), request.source(), request.severity());
    }
}

