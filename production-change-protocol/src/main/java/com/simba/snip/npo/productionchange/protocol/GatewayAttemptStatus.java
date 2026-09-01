package com.simba.snip.npo.productionchange.protocol;

public enum GatewayAttemptStatus {
    PRE_SEND,
    SEND_ELIGIBLE,
    MAY_HAVE_SENT,
    VENDOR_REJECTED,
    VENDOR_ACCEPTED,
    OUTCOME_UNKNOWN,
    VERIFYING,
    VERIFIED,
    VERIFICATION_FAILED,
    RECOVERY_REQUIRED,
    MANUAL_INTERVENTION_REQUIRED
}
