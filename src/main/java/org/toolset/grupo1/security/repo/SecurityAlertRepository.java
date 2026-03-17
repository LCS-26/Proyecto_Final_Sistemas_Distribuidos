package org.toolset.grupo1.security.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.toolset.grupo1.security.domain.SecurityAlert;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    List<SecurityAlert> findTop20ByOrderByCreatedAtDesc();
}

