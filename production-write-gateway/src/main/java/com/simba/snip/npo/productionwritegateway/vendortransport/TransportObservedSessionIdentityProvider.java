package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionwritegateway.transport.EricssonWriteTransport;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TransportObservedSessionIdentityProvider implements ObservedVendorSessionIdentityProvider {

    private final EricssonWriteTransport transport;

    public TransportObservedSessionIdentityProvider(EricssonWriteTransport transport) {
        this.transport = transport;
    }

    @Override
    public Optional<DestinationTrustValidator.ObservedDestination> currentObserved() {
        if (transport instanceof ObservedVendorSessionIdentityProvider provider) {
            return provider.currentObserved();
        }
        return Optional.empty();
    }
}
