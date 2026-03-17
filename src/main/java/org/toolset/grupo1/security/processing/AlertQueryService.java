package org.toolset.grupo1.security.processing;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.toolset.grupo1.security.domain.SecurityAlert;
import org.toolset.grupo1.security.repo.SecurityAlertRepository;

@Service
public class AlertQueryService {

    private final SecurityAlertRepository securityAlertRepository;

    public AlertQueryService(SecurityAlertRepository securityAlertRepository) {
        this.securityAlertRepository = securityAlertRepository;
    }

    @Transactional(readOnly = true)
    public List<SecurityAlert> latestAlerts() {
        return securityAlertRepository.findTop20ByOrderByCreatedAtDesc();
    }
}

