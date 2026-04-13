package org.toolset.grupo1.alert.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.toolset.grupo1.alert.model.Alert;

/**
 * Repositorio Spring Data JPA para la entidad Alert.
 *
 * Spring Data genera automáticamente la implementación de los métodos CRUD
 * en tiempo de arranque (IoC / DI). Solo declaramos la interfaz — Spring
 * crea el bean e inyecta la implementación concreta.
 *
 * Conceptos de los apuntes:
 * - Spring Data JPA: JpaRepository proporciona findAll, save, findById, etc.
 * - Spring JDBC/JPA: abstracción sobre JDBC y HikariCP connection pool
 */
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByOrderByTimestampDesc();
}
