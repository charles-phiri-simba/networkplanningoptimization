package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionTargetHealthEntity;
import com.simba.snip.npo.productionwritegateway.metrics.ProductionGatewayMetrics;
import com.simba.snip.npo.productionwritegateway.repository.ProductionNetworkTargetRepository;
import com.simba.snip.npo.productionwritegateway.repository.ProductionTargetHealthRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ProductionTargetHealthService {

    private final ProductionTargetHealthRepository healthRepository;
    private final ProductionNetworkTargetRepository targetRepository;
    private final ProductionChangeGatewayProperties properties;
    private final ProductionGatewayMetrics metrics;

    public ProductionTargetHealthService(
            ProductionTargetHealthRepository healthRepository,
            ProductionNetworkTargetRepository targetRepository,
            ProductionChangeGatewayProperties properties,
            ProductionGatewayMetrics metrics
    ) {
        this.healthRepository = healthRepository;
        this.targetRepository = targetRepository;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOutcomeUnknown(String targetId) {
        ProductionTargetHealthEntity health = healthRepository.findByProductionTargetId(targetId).orElse(null);
        if (health == null) {
            return;
        }
        health.setOutcomeUnknownCount(health.getOutcomeUnknownCount() + 1);
        health.setLastCheckedAt(Instant.now());
        maybeSuspend(targetId, health);
        healthRepository.saveAndFlush(health);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordVerificationFailure(String targetId) {
        ProductionTargetHealthEntity health = healthRepository.findByProductionTargetId(targetId).orElse(null);
        if (health == null) {
            return;
        }
        health.setVerificationFailureCount(health.getVerificationFailureCount() + 1);
        health.setLastCheckedAt(Instant.now());
        maybeSuspend(targetId, health);
        healthRepository.saveAndFlush(health);
    }

    private void maybeSuspend(String targetId, ProductionTargetHealthEntity health) {
        if (health.getOutcomeUnknownCount() >= properties.getMaximumOutcomeUnknownBeforeSuspend()
                || health.getVerificationFailureCount() >= properties.getMaximumVerificationFailuresBeforeSuspend()) {
            health.setHealthState("UNHEALTHY");
            ProductionNetworkTargetEntity target = targetRepository.findById(targetId).orElse(null);
            if (target != null && "ACTIVE".equals(target.getTargetState())) {
                target.setTargetState("SUSPENDED");
                target.setUpdatedAt(Instant.now());
                targetRepository.saveAndFlush(target);
                metrics.incrementTargetSuspensions();
            }
        } else if (health.getOutcomeUnknownCount() > 0 || health.getVerificationFailureCount() > 0) {
            health.setHealthState("DEGRADED");
        }
    }
}
