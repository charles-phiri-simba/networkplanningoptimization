package com.simba.snip.npo.productionchange.protocol;

import java.util.UUID;

public record GatewayExecuteResponse(
        UUID productionChangeId,
        UUID grantId,
        UUID attemptId,
        String productionChangeStatus,
        GatewayAttemptStatus attemptStatus,
        MutationOutcome mutationOutcome,
        String reasonCode,
        Integer mutationInvocationCount
) {
}
