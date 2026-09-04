package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionwritegateway.transport.EricssonWriteTransport;
import com.simba.snip.npo.productionwritegateway.transport.UnconfiguredProductionEricssonWriteTransport;
import org.springframework.stereotype.Component;

/**
 * Resolves the production write transport. Production remains unconfigured.
 * Does not invent an Ericsson protocol.
 */
@Component
public class CertifiedTransportResolver {

    private final EricssonWriteTransport transport;

    public CertifiedTransportResolver(EricssonWriteTransport transport) {
        this.transport = transport;
    }

    public EricssonWriteTransport resolveProduction() {
        return transport;
    }

    public boolean isUnconfiguredProduction() {
        return transport instanceof UnconfiguredProductionEricssonWriteTransport;
    }
}
