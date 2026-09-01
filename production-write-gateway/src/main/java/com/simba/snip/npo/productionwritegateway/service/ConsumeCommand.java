package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.GrantType;

import java.util.UUID;

public record ConsumeCommand(
        UUID grantId,
        UUID productionChangeId,
        UUID phase15ExecutionId,
        String targetId,
        String productionFingerprint,
        int authorizationGeneration,
        long fencingToken,
        String operationBindingHash,
        GrantType grantType
) {
}
