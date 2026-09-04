package com.simba.snip.npo.productionchange.protocol;

import java.time.Instant;
import java.util.UUID;

public record RuntimeArtifactIdentity(
        String artifactDigest,
        String transportImplementationVersion,
        String sourceBaselineSha,
        String containerImageDigest
) {
    public RuntimeArtifactIdentity {
        if (artifactDigest == null || !artifactDigest.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("malformed runtime artifact digest");
        }
        if (sourceBaselineSha == null || !sourceBaselineSha.matches("^[0-9a-f]{40}$")) {
            throw new IllegalArgumentException("malformed source baseline sha");
        }
        if (transportImplementationVersion == null || transportImplementationVersion.isBlank()) {
            throw new IllegalArgumentException("missing transport implementation version");
        }
    }
}
