package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;

public record ConsumeResult(
        boolean succeeded,
        int rowsUpdated,
        ProductionReasonCode denyReason
) {
    public static ConsumeResult success() {
        return new ConsumeResult(true, 1, null);
    }

    public static ConsumeResult denied(ProductionReasonCode reason) {
        return new ConsumeResult(false, 0, reason);
    }
}
