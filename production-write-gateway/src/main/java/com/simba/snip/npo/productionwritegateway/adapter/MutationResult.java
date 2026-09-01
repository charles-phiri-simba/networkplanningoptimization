package com.simba.snip.npo.productionwritegateway.adapter;

import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;

public record MutationResult(
        MutationOutcome outcome,
        ProductionReasonCode reasonCode,
        String safeDetail
) {
}
