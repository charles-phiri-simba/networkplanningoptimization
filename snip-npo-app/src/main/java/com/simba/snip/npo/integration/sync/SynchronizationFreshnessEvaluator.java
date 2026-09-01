package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class SynchronizationFreshnessEvaluator {

    public SynchronizationFreshness evaluate(
            SynchronizationPolicy policy,
            Optional<SynchronizationCheckpointEntity> checkpoint,
            Instant now,
            boolean recoveryRequired,
            boolean recentFailure
    ) {
        if (checkpoint.isEmpty()
                || checkpoint.get().getLastSuccessfulCompletedAt() == null
                || !SynchronizationCheckpointStatus.VALID.name().equals(checkpoint.get().getStatus())) {
            return SynchronizationFreshness.UNKNOWN;
        }
        if (recoveryRequired || recentFailure) {
            return SynchronizationFreshness.DEGRADED;
        }
        Duration age = Duration.between(checkpoint.get().getLastSuccessfulCompletedAt(), now);
        if (age.compareTo(policy.staleAfter()) >= 0) {
            return SynchronizationFreshness.STALE;
        }
        if (age.compareTo(policy.agingAfter()) >= 0) {
            return SynchronizationFreshness.AGING;
        }
        return SynchronizationFreshness.FRESH;
    }
}
