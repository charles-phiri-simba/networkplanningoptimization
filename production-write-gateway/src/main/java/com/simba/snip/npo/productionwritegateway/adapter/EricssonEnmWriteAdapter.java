package com.simba.snip.npo.productionwritegateway.adapter;

import com.simba.snip.npo.productionchange.protocol.AuthorizedParameterMutation;
import com.simba.snip.npo.productionchange.protocol.ProductionExecutionContext;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.transport.EricssonMutationRequest;
import com.simba.snip.npo.productionwritegateway.transport.EricssonWriteTransport;
import org.springframework.stereotype.Component;

@Component
public class EricssonEnmWriteAdapter implements VendorNetworkWriteAdapter {

    private final EricssonWriteTransport transport;

    public EricssonEnmWriteAdapter(EricssonWriteTransport transport) {
        this.transport = transport;
    }

    @Override
    public MutationResult applyAuthorizedMutation(
            ProductionExecutionContext context,
            AuthorizedParameterMutation mutation
    ) {
        if (!"CELL".equals(mutation.objectType()) || !"txPower".equals(mutation.parameter())) {
            return new MutationResult(
                    com.simba.snip.npo.productionchange.protocol.MutationOutcome.NOT_SENT,
                    ProductionReasonCode.PRODUCTION_SCOPE_DENIED,
                    "typed CELL/txPower only"
            );
        }
        EricssonMutationRequest request = new EricssonMutationRequest(
                context.productionChangeId(),
                null,
                mutation.cellId(),
                mutation.parameter(),
                mutation.expectedValue(),
                mutation.desiredValue(),
                false
        );
        var vendor = transport.transmitMutation(request);
        return new MutationResult(vendor.outcome(), vendor.reasonCode(), null);
    }

    public EricssonWriteTransport transport() {
        return transport;
    }
}
