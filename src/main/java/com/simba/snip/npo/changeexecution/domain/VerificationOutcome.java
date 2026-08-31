package com.simba.snip.npo.changeexecution.domain;

public enum VerificationOutcome {
    VERIFIED,
    MISMATCH,
    UNKNOWN,
    TIMEOUT,
    SOURCE_UNAVAILABLE,
    STALE_OBSERVATION
}
