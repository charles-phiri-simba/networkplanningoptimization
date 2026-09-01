package com.simba.snip.npo.productionchange.domain;

/**
 * Distinguishes vendor compare-and-set from observe-then-mutate.
 * READ_THEN_WRITE acknowledges residual TOCTOU; this is not an atomicity claim.
 */
public enum ExpectedStateGuardStrength {
    ATOMIC,
    READ_THEN_WRITE
}
