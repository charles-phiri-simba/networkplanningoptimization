package com.simba.snip.npo.integration.ericsson.enm;

public record EnmManagedElement(
        String moId,
        String dn,
        String userLabel,
        String snipCanonicalId
) {
}
