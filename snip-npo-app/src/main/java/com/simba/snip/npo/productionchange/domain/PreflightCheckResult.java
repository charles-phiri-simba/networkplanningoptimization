package com.simba.snip.npo.productionchange.domain;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;

public record PreflightCheckResult(
        String checkName,
        PreflightOutcome outcome,
        ProductionReasonCode reasonCode,
        String detail
) {
    public static PreflightCheckResult pass(String checkName) {
        return new PreflightCheckResult(checkName, PreflightOutcome.PASS, null, null);
    }

    public static PreflightCheckResult deny(String checkName, ProductionReasonCode reasonCode, String detail) {
        return new PreflightCheckResult(checkName, PreflightOutcome.DENY, reasonCode, detail);
    }

    public static PreflightCheckResult unknown(String checkName, ProductionReasonCode reasonCode, String detail) {
        return new PreflightCheckResult(checkName, PreflightOutcome.UNKNOWN, reasonCode, detail);
    }

    public boolean permitsGrant() {
        return outcome == PreflightOutcome.PASS;
    }
}
