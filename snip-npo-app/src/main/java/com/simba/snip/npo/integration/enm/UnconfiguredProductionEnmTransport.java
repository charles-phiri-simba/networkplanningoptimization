package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.ImportFailureCode;

public final class UnconfiguredProductionEnmTransport implements EnmTransport {

    @Override
    public void open(ImportExecutionContext context) {
        throw new VendorConnectorException(
                ImportFailureCode.PRODUCTION_TRANSPORT_NOT_CONFIGURED,
                "production ENM transport is not configured"
        );
    }

    @Override
    public com.simba.snip.npo.integration.ericsson.enm.EnmInventoryPage fetchFirstPage(
            ImportExecutionContext context, int pageSize
    ) {
        throw new VendorConnectorException(
                ImportFailureCode.PRODUCTION_TRANSPORT_NOT_CONFIGURED,
                "production ENM transport is not configured"
        );
    }

    @Override
    public com.simba.snip.npo.integration.ericsson.enm.EnmInventoryPage fetchContinuation(
            ImportExecutionContext context, String continuationToken, int pageSize
    ) {
        throw new VendorConnectorException(
                ImportFailureCode.PRODUCTION_TRANSPORT_NOT_CONFIGURED,
                "production ENM transport is not configured"
        );
    }

    @Override
    public java.time.Duration lastRetryAfter() {
        return java.time.Duration.ZERO;
    }

    @Override
    public void close() {
        // no session
    }
}
