package com.simba.snip.npo.productionwritegateway.transport;

import com.simba.snip.npo.productionwritegateway.adapter.PostMutationObservation;
import com.simba.snip.npo.productionwritegateway.adapter.VendorMutationResult;

public interface EricssonWriteTransport {

    VendorMutationResult transmitMutation(EricssonMutationRequest request);

    PostMutationObservation observeParameter(EricssonObservationRequest request);

    default boolean supportsAtomicCompareAndSet() {
        return false;
    }
}
