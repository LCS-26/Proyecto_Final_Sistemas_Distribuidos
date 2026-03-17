package org.toolset.grupo1.security.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.toolset.grupo1.security.domain.SecurityAlert;

@Service
public class AlertNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public AlertNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(SecurityAlert alert) {
        AlertMessage message = new AlertMessage(
                alert.getId(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getEventId());

        messagingTemplate.convertAndSend("/topic/alerts", message);
    }
}

