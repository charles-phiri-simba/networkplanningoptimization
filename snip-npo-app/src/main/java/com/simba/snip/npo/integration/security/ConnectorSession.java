package com.simba.snip.npo.integration.security;

import java.time.Instant;
import java.util.UUID;

public record ConnectorSession(
        UUID sessionId,
        UUID executionId,
        String connectorId,
        String sourceSystem,
        String credentialRef,
        String credentialVersion,
        String trustProfileId,
        String endpointRef,
        String serverCertificateFingerprint,
        Instant startedAt,
        Instant endedAt,
        ConnectorSessionStatus status
) {
}
