package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.TargetHealthState;
import com.simba.snip.npo.productionchange.entity.ProductionTargetHealthEntity;
import com.simba.snip.npo.productionchange.repository.ProductionTargetHealthRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ProductionTargetHealthService {

    private final ProductionTargetHealthRepository healthRepository;
    private final ProductionTargetAdministrationService administrationService;
    private final ProductionChangeProperties properties;
    private final Clock clock;

    public ProductionTargetHealthService(
            ProductionTargetHealthRepository healthRepository,
            ProductionTargetAdministrationService administrationService,
            ProductionChangeProperties properties,
            Clock clock
    ) {
        this.healthRepository = healthRepository;
        this.administrationService = administrationService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ProductionTargetHealthEntity require(String targetId) {
        return healthRepository.findByProductionTargetId(targetId)
                .orElseGet(() -> healthRepository.save(
                        ProductionTargetHealthEntity.initial(UUID.randomUUID(), targetId, clock.instant())
                ));
    }

    @Transactional
    public void recordOutcomeUnknown(String targetId, ActorPrincipal actor) {
        ProductionTargetHealthEntity health = require(targetId);
        health.setOutcomeUnknownCount(health.getOutcomeUnknownCount() + 1);
        health.setLastCheckedAt(clock.instant());
        maybeDegradeAndSuspend(health, actor);
    }

    @Transactional
    public void recordVerificationFailure(String targetId, ActorPrincipal actor) {
        ProductionTargetHealthEntity health = require(targetId);
        health.setVerificationFailureCount(health.getVerificationFailureCount() + 1);
        health.setLastCheckedAt(clock.instant());
        maybeDegradeAndSuspend(health, actor);
    }

    private void maybeDegradeAndSuspend(ProductionTargetHealthEntity health, ActorPrincipal actor) {
        boolean outcomeUnknownExceeded = health.getOutcomeUnknownCount() >= properties.getMaximumOutcomeUnknownBeforeSuspend();
        boolean verificationExceeded = health.getVerificationFailureCount() >= properties.getMaximumVerificationFailuresBeforeSuspend();
        if (outcomeUnknownExceeded || verificationExceeded) {
            health.setHealthState(TargetHealthState.UNHEALTHY.name());
            administrationService.suspend(health.getProductionTargetId(), actor);
        } else if (health.getOutcomeUnknownCount() > 0 || health.getVerificationFailureCount() > 0) {
            health.setHealthState(TargetHealthState.DEGRADED.name());
        }
    }
}
