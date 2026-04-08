package org.toolset.grupo1.alert.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.toolset.grupo1.alert.ws.AlertNotificationService;

@RestController
@RequestMapping
public class AlertController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final CopyOnWriteArrayList<AlertRequest> alerts = new CopyOnWriteArrayList<>();
    private final AlertNotificationService alertNotificationService;

    public AlertController(AlertNotificationService alertNotificationService) {
        this.alertNotificationService = alertNotificationService;
    }

    @PostMapping("/internal/alerts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receiveAlert(
            @RequestBody AlertRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=alert_received source=alert-service cid={} sensorType={} sensorSource={} severity={}",
                cid, request.sensorType(), request.source(), request.severity());
        alerts.add(request);
        LOGGER.info("event=alert_saved source=alert-service cid={} sensorType={} sensorSource={} totalAlerts={}",
                cid, request.sensorType(), request.source(), alerts.size());
        alertNotificationService.publish(request, cid);
    }

    @GetMapping("/api/alerts")
    public List<AlertRequest> alerts(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=alert_query source=alert-service cid={} count={}", cid, alerts.size());
        List<AlertRequest> latest = new ArrayList<>(alerts);
        Collections.reverse(latest);
        return latest;
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        return incomingCorrelationId;
    }
}
