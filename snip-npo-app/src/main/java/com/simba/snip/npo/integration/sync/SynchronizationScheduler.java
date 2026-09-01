package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.config.SynchronizationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "snip.integration.sync", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class SynchronizationScheduler {

    private final SynchronizationControlPlane controlPlane;
    private final SynchronizationProperties properties;

    public SynchronizationScheduler(SynchronizationControlPlane controlPlane, SynchronizationProperties properties) {
        this.controlPlane = controlPlane;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${snip.integration.sync.scheduler-tick:5000}")
    public void tick() {
        List<SynchronizationPolicy> due = controlPlane.configuredSources().stream()
                .filter(SynchronizationPolicy::enabled)
                .filter(controlPlane::isDue)
                .limit(properties.getMaxDueSourcesPerTick())
                .toList();
        for (SynchronizationPolicy policy : due) {
            try {
                controlPlane.triggerScheduled(policy);
            } catch (RuntimeException ignored) {
                // bounded scheduler; next cadence may retry
            }
        }
    }
}
