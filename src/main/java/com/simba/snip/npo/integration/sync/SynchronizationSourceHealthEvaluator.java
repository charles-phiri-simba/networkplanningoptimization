package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

@Component
public class SynchronizationSourceHealthEvaluator {

    public SynchronizationSourceHealth mapFailure(ImportFailureCode code) {
        return switch (code) {
            case VENDOR_AUTHENTICATION_FAILED, CONNECTOR_AUTHENTICATION_FAILED, VAULT_AUTHENTICATION_FAILED ->
                    SynchronizationSourceHealth.AUTHENTICATION_FAILED;
            case VENDOR_AUTHORIZATION_DENIED, CONNECTOR_AUTHORIZATION_DENIED, VAULT_ACCESS_DENIED ->
                    SynchronizationSourceHealth.AUTHORIZATION_FAILED;
            case VENDOR_RATE_LIMITED -> SynchronizationSourceHealth.THROTTLED;
            case VENDOR_UNAVAILABLE, VENDOR_TIMEOUT, VENDOR_PROTOCOL_ERROR -> SynchronizationSourceHealth.UNREACHABLE;
            case RECOVERY_REQUIRED, CHECKPOINT_UNCERTAIN, CHECKPOINT_REJECTED, CHECKPOINT_EXPIRED, SEQUENCE_GAP ->
                    SynchronizationSourceHealth.RECOVERING;
            case SYNCHRONIZATION_DISABLED -> SynchronizationSourceHealth.DISABLED;
            default -> SynchronizationSourceHealth.DEGRADED;
        };
    }

    public SynchronizationSourceHealth healthyAfterSuccess() {
        return SynchronizationSourceHealth.HEALTHY;
    }
}
