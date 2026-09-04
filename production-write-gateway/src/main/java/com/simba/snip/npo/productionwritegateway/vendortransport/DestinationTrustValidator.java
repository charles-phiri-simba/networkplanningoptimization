package com.simba.snip.npo.productionwritegateway.vendortransport;

import org.springframework.stereotype.Component;

/**
 * Destination identity comparison. DNS alone is not vendor identity.
 * Expected non-null + observed null is DENY for that required dimension.
 */
@Component
public class DestinationTrustValidator {

    public enum Mismatch {
        NONE,
        MISSING,
        FQDN,
        PORT,
        TLS_IDENTITY,
        TRUST_CHAIN,
        HOSTNAME,
        NETWORK_PROFILE
    }

    public record ObservedDestination(
            String fqdn,
            Integer port,
            String tlsServerIdentity,
            boolean trustedChain,
            boolean hostnameVerified,
            String networkDomain,
            String networkZone
    ) {
    }

    public Mismatch compare(
            ProductionCertificationAuthority.ResolvedCurrentness current,
            ObservedDestination observed
    ) {
        if (observed == null) {
            return Mismatch.MISSING;
        }
        if (current.approvedFqdn() != null && (observed.fqdn() == null
                || !current.approvedFqdn().equalsIgnoreCase(observed.fqdn()))) {
            return Mismatch.FQDN;
        }
        if (current.approvedPort() != null && (observed.port() == null
                || !current.approvedPort().equals(observed.port()))) {
            return Mismatch.PORT;
        }
        if (current.tlsServerIdentity() != null && (observed.tlsServerIdentity() == null
                || !current.tlsServerIdentity().equalsIgnoreCase(observed.tlsServerIdentity()))) {
            return Mismatch.TLS_IDENTITY;
        }
        if (!observed.trustedChain()) {
            return Mismatch.TRUST_CHAIN;
        }
        if (current.hostnameVerificationRequired() && !observed.hostnameVerified()) {
            return Mismatch.HOSTNAME;
        }
        if (current.networkDomain() != null && (observed.networkDomain() == null
                || !current.networkDomain().equals(observed.networkDomain()))) {
            return Mismatch.NETWORK_PROFILE;
        }
        if (current.routeZoneId() != null && (observed.networkZone() == null
                || !current.routeZoneId().equals(observed.networkZone()))) {
            return Mismatch.NETWORK_PROFILE;
        }
        return Mismatch.NONE;
    }
}
