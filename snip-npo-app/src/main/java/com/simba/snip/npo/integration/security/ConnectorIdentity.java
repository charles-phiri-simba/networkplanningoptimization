package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.Vendor;

public record ConnectorIdentity(
        String connectorId,
        String sourceSystem,
        Vendor vendor,
        String environment,
        String purpose,
        String credentialRef,
        String trustProfileId,
        String authorizationProfileId,
        String networkPolicyId,
        boolean enabled
) {
}
