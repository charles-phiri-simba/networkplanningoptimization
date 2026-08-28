package com.simba.snip.npo.integration.ericsson.enm;

public record EnmCell(
        String moId,
        String ldn,
        String parentRadioFunctionId,
        String userLabel,
        String snipCanonicalId,
        String parentGnbCanonicalId,
        Integer configuredMaxTxPowerTenthsDbm
) {
}
