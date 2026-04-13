package org.toolset.grupo1.alert.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.toolset.grupo1.alert.api.AlertRequest;
import org.toolset.grupo1.alert.model.Alert;
import org.toolset.grupo1.alert.repository.AlertRepository;

/**
 * Servicio de negocio para la gestión de alertas con persistencia JPA.
 *
 * Conceptos de los apuntes demostrados:
 * - @Service: bean de Spring gestionado por el contenedor IoC (Singleton por defecto)
 * - @Transactional: garantiza que las operaciones de base de datos sean atómicas.
 *   Si falla el guardado, la transacción hace rollback automático.
 * - Spring Data JPA: inyección de AlertRepository por constructor (DI)
 * - Connection Pooling (HikariCP): gestionado automáticamente por Spring Boot
 */
@Service
public class AlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Persiste una alerta en la base de datos dentro de una transacción.
     * @Transactional asegura atomicidad: o todo se guarda o se hace rollback.
     */
    @Transactional
    public Alert save(AlertRequest request, String correlationId) {
        Alert alert = new Alert(
                request.severity(),
                request.message(),
                request.sensorType(),
                request.source(),
                request.value(),
                request.timestamp(),
                correlationId
        );
        Alert saved = alertRepository.save(alert);
        LOGGER.info("event=alert_persisted source=alert-service cid={} alertId={} sensorType={} source={}",
                correlationId, saved.getId(), saved.getSensorType(), saved.getSource());
        return saved;
    }

    /**
     * Recupera todas las alertas ordenadas por fecha descendente (sin modificar datos, readOnly).
     */
    @Transactional(readOnly = true)
    public List<AlertRequest> findAll() {
        return alertRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(a -> new AlertRequest(a.getSeverity(), a.getMessage(),
                        a.getSensorType(), a.getSource(), a.getValue(), a.getTimestamp()))
                .toList();
    }
}
