package com.simba.snip.npo.productionwritegateway.transport;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.adapter.ObservationStatus;
import com.simba.snip.npo.productionwritegateway.adapter.PostMutationObservation;
import com.simba.snip.npo.productionwritegateway.adapter.VendorMutationResult;

import java.time.Instant;

/**
 * Production Ericsson write transport is NOT CONFIGURED. Fail closed.
 * Does not guess REST/CLI/Bulk CM endpoints, URLs, or payloads.
 */
public class UnconfiguredProductionEricssonWriteTransport implements EricssonWriteTransport {

    @Override
    public VendorMutationResult transmitMutation(EricssonMutationRequest request) {
        return VendorMutationResult.notSent(ProductionReasonCode.PRODUCTION_WRITE_TRANSPORT_NOT_CONFIGURED);
    }

    @Override
    public PostMutationObservation observeParameter(EricssonObservationRequest request) {
        return new PostMutationObservation(ObservationStatus.SOURCE_UNAVAILABLE, null, Instant.now());
    }

    @Override
    public boolean supportsAtomicCompareAndSet() {
        return false;
    }
}
