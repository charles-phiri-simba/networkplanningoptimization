package com.simba.snip.npo.productionwritegateway.adapter;

import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;

public record VendorMutationResult(
        MutationOutcome outcome,
        ProductionReasonCode reasonCode,
        boolean applied
) {
    public static VendorMutationResult accepted() {
        return new VendorMutationResult(MutationOutcome.VENDOR_ACCEPTED, null, true);
    }

    public static VendorMutationResult rejected(ProductionReasonCode reasonCode) {
        return new VendorMutationResult(MutationOutcome.REJECTED, reasonCode, false);
    }

    public static VendorMutationResult unknown(ProductionReasonCode reasonCode, boolean applied) {
        return new VendorMutationResult(MutationOutcome.OUTCOME_UNKNOWN, reasonCode, applied);
    }

    public static VendorMutationResult notSent(ProductionReasonCode reasonCode) {
        return new VendorMutationResult(MutationOutcome.NOT_SENT, reasonCode, false);
    }
}
