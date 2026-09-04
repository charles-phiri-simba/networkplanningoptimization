package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.productionchange.protocol.TransportHealthState;
import org.springframework.stereotype.Component;

/**
 * Phase 17 transport health AND Phase 16 target health. Unknown denies.
 * Healthy Phase 17 cannot override Phase 16 denial.
 */
@Component
public class TransportHealthComposer {

    public Phase17DenialCode blockingReason(
            String phase16TargetState,
            String phase17Health,
            String targetCertificationStatus
    ) {
        if (phase16TargetState != null) {
            String p16 = phase16TargetState.toUpperCase();
            if ("SUSPENDED".equals(p16) || "DISABLED".equals(p16) || "INACTIVE".equals(p16)) {
                return Phase17DenialCode.P17_HEALTH_BLOCKING;
            }
        }
        if (phase17Health == null || phase17Health.isBlank()) {
            return Phase17DenialCode.P17_HEALTH_BLOCKING;
        }
        TransportHealthState health;
        try {
            health = TransportHealthState.valueOf(phase17Health);
        } catch (IllegalArgumentException ex) {
            return Phase17DenialCode.P17_HEALTH_BLOCKING;
        }
        if (health != TransportHealthState.HEALTHY) {
            return Phase17DenialCode.P17_HEALTH_BLOCKING;
        }
        if (!"CURRENT".equals(targetCertificationStatus)) {
            return Phase17DenialCode.P17_CERTIFICATION_STALE;
        }
        return null;
    }
}
