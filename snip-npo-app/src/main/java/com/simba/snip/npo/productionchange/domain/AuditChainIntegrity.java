package com.simba.snip.npo.productionchange.domain;

public enum AuditChainIntegrity {
    VALID,
    UNVERIFIED,
    INVALID,
    UNAVAILABLE;

    public boolean blocksMutation() {
        return this == INVALID || this == UNAVAILABLE;
    }
}
