package com.simba.snip.npo.productionwritegateway.vendortransport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DestinationTrustValidatorTest {

    @Test
    void t17Int035DestinationHarness() {
        DestinationTrustValidator validator = new DestinationTrustValidator();
        ProductionCertificationAuthority.ResolvedCurrentness current = current("enm.lab.invalid", 443, "enm.lab.invalid", "LAB", "zone-a");
        assertEquals(DestinationTrustValidator.Mismatch.NONE, validator.compare(current,
                TestDestinationIdentityDouble.approved("enm.lab.invalid", 443, "enm.lab.invalid", "LAB", "zone-a")));
        assertEquals(DestinationTrustValidator.Mismatch.FQDN, validator.compare(current,
                TestDestinationIdentityDouble.wrongFqdn("evil.example", 443, "enm.lab.invalid", "LAB", "zone-a")));
        assertEquals(DestinationTrustValidator.Mismatch.PORT, validator.compare(current,
                new DestinationTrustValidator.ObservedDestination("enm.lab.invalid", 8443, "enm.lab.invalid", true, true, "LAB", "zone-a")));
        assertEquals(DestinationTrustValidator.Mismatch.TLS_IDENTITY, validator.compare(current,
                new DestinationTrustValidator.ObservedDestination("enm.lab.invalid", 443, "other", true, true, "LAB", "zone-a")));
        assertEquals(DestinationTrustValidator.Mismatch.TRUST_CHAIN, validator.compare(current,
                new DestinationTrustValidator.ObservedDestination("enm.lab.invalid", 443, "enm.lab.invalid", false, true, "LAB", "zone-a")));
        assertEquals(DestinationTrustValidator.Mismatch.HOSTNAME, validator.compare(current,
                new DestinationTrustValidator.ObservedDestination("enm.lab.invalid", 443, "enm.lab.invalid", true, false, "LAB", "zone-a")));
        assertEquals(DestinationTrustValidator.Mismatch.NETWORK_PROFILE, validator.compare(current,
                new DestinationTrustValidator.ObservedDestination("enm.lab.invalid", 443, "enm.lab.invalid", true, true, "PROD", "zone-a")));
        assertEquals(DestinationTrustValidator.Mismatch.MISSING, validator.compare(current, null));
        assertEquals(DestinationTrustValidator.Mismatch.FQDN, validator.compare(current,
                new DestinationTrustValidator.ObservedDestination(null, 443, "enm.lab.invalid", true, true, "LAB", "zone-a")));
        assertEquals(DestinationTrustValidator.Mismatch.PORT, validator.compare(current,
                new DestinationTrustValidator.ObservedDestination("enm.lab.invalid", null, "enm.lab.invalid", true, true, "LAB", "zone-a")));
    }

    private static ProductionCertificationAuthority.ResolvedCurrentness current(
            String fqdn, int port, String tls, String domain, String zone
    ) {
        return new ProductionCertificationAuthority.ResolvedCurrentness(
                null, "ACTIVE", "PRODUCTION_REGISTERED", fqdn, port, tls, domain, zone,
                "ERICSSON", "ENM", 1, true, "ACTIVE", "ACTIVE", "t", "ACTIVE",
                "CELL", "txPower", false, "READ_THEN_WRITE", "ACTIVE", "ACTIVE", "APPROVED", "ACTIVE"
        );
    }
}
