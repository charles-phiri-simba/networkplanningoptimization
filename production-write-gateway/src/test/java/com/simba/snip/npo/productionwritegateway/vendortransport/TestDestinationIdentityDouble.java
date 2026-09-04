package com.simba.snip.npo.productionwritegateway.vendortransport;

/**
 * Test-only destination identity double. Not an EricssonWriteTransport.
 * Impossible to enable as production vendor transport.
 */
public final class TestDestinationIdentityDouble {

    private DestinationTrustValidator.ObservedDestination observed;

    public void setObserved(DestinationTrustValidator.ObservedDestination observed) {
        this.observed = observed;
    }

    public DestinationTrustValidator.ObservedDestination observed() {
        return observed;
    }

    public static DestinationTrustValidator.ObservedDestination approved(
            String fqdn, int port, String tls, String domain, String zone
    ) {
        return new DestinationTrustValidator.ObservedDestination(fqdn, port, tls, true, true, domain, zone);
    }

    public static DestinationTrustValidator.ObservedDestination wrongFqdn(
            String fqdn, int port, String tls, String domain, String zone
    ) {
        return new DestinationTrustValidator.ObservedDestination(fqdn, port, tls, true, true, domain, zone);
    }
}
