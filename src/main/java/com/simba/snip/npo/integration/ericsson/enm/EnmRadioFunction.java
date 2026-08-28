package com.simba.snip.npo.integration.ericsson.enm;

public record EnmRadioFunction(
        String moId,
        String dn,
        String parentManagedElementId,
        String userLabel,
        String snipCanonicalId,
        String parentSiteCanonicalId
) {
}
