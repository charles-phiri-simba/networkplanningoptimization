package com.simba.snip.npo.productionchange.protocol;

import java.util.UUID;

public record ProductionExecutionContext(
        UUID productionChangeId,
        UUID grantId,
        String productionTargetId,
        String productionFingerprint,
        int authorizationGeneration,
        long fencingToken,
        String operationBindingHash,
        GrantType grantType
) {
}
