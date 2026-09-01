package com.simba.snip.npo.productionwritegateway.adapter;

import com.simba.snip.npo.productionchange.protocol.AuthorizedParameterMutation;
import com.simba.snip.npo.productionchange.protocol.ProductionExecutionContext;

public interface VendorNetworkWriteAdapter {

    MutationResult applyAuthorizedMutation(
            ProductionExecutionContext context,
            AuthorizedParameterMutation mutation
    );
}
