package com.simba.snip.npo.productionwritegateway.vendortransport;

import org.springframework.stereotype.Component;

/**
 * Binds Phase 10 secret resolution to the certified target + profile + workload identity.
 * Does not resolve secret values.
 */
@Component
public class Phase17CredentialResolutionBinder {

    public boolean matches(
            String grantTargetId,
            String certifiedCredentialTargetId,
            String credentialStatus
    ) {
        if (grantTargetId == null || grantTargetId.isBlank()) {
            return false;
        }
        if (certifiedCredentialTargetId == null || certifiedCredentialTargetId.isBlank()) {
            return false;
        }
        if (!grantTargetId.equals(certifiedCredentialTargetId)) {
            return false;
        }
        return "ACTIVE".equals(credentialStatus);
    }
}
