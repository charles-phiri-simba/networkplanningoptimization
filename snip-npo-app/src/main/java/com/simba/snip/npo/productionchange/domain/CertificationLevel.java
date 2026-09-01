package com.simba.snip.npo.productionchange.domain;

public enum CertificationLevel {
    L0,
    L1,
    L2,
    L3,
    L4;

    public boolean meets(CertificationLevel minimum) {
        return ordinal() >= minimum.ordinal();
    }
}
