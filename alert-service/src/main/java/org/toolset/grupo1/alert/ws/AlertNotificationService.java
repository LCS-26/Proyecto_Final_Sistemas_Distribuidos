package org.toolset.grupo1.alert.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.alert.api.AlertRequest;

@Service
public class AlertNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public AlertNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(AlertRequest request) {
        AlertMessage message = new AlertMessage(
                request.severity(),
                request.message(),
                request.sensorType(),
                request.source(),
                request.value(),
                request.timestamp());

        messagingTemplate.convertAndSend("/topic/alerts", message);
    }
}

