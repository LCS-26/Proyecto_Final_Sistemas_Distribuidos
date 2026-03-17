package org.toolset.grupo1.security.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.toolset.grupo1.security.domain.SensorEvent;
import org.toolset.grupo1.security.domain.SensorType;

public interface SensorEventRepository extends JpaRepository<SensorEvent, Long> {

    List<SensorEvent> findTop20ByTypeOrderByReceivedAtDesc(SensorType type);
}

