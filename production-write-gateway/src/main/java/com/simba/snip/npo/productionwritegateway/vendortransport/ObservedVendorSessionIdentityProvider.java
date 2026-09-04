package com.simba.snip.npo.productionwritegateway.vendortransport;

import java.util.Optional;

/**
 * Observed vendor session identity from the transport/session boundary.
 * Missing observation is fail-closed on the certified send path.
 */
public interface ObservedVendorSessionIdentityProvider {

    Optional<DestinationTrustValidator.ObservedDestination> currentObserved();
}
