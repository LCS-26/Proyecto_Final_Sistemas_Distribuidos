package org.toolset.grupo1.alert.api;

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
import org.toolset.grupo1.alert.service.AlertService;
import org.toolset.grupo1.alert.ws.AlertNotificationService;

/**
 * Controlador REST del alert-service.
 *
 * Expone dos endpoints:
 * - POST /internal/alerts : recibe alertas via HTTP desde los sensores (camino HTTP como fallback)
 * - GET  /api/alerts       : devuelve todas las alertas almacenadas en base de datos
 *
 * El camino principal de las alertas críticas es via RabbitMQ (ver AlertConsumer).
 * Este endpoint HTTP actúa como fallback cuando RabbitMQ no está disponible.
 */
@RestController
@RequestMapping
public class AlertController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final AlertService alertService;
    private final AlertNotificationService alertNotificationService;

    public AlertController(AlertService alertService, AlertNotificationService alertNotificationService) {
        this.alertService = alertService;
        this.alertNotificationService = alertNotificationService;
    }

    @PostMapping("/internal/alerts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receiveAlert(
            @RequestBody AlertRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        LOGGER.info("event=alert_received source=alert-service cid={} sensorType={} sensorSource={} severity={} path=http",
                cid, request.sensorType(), request.source(), request.severity());
        alertService.save(request, cid);
        alertNotificationService.publish(request, cid);
    }

    @GetMapping("/api/alerts")
    public List<AlertRequest> alerts(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        String cid = resolveCorrelationId(correlationId);
        List<AlertRequest> result = alertService.findAll();
        LOGGER.info("event=alert_query source=alert-service cid={} count={}", cid, result.size());
        return result;
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        return incomingCorrelationId;
    }
}
