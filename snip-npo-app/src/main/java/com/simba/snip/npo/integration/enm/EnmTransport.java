package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.ericsson.enm.EnmInventoryPage;

import com.simba.snip.npo.integration.sync.VendorIncrementalBatch;

import java.time.Duration;

public interface EnmTransport extends AutoCloseable {

    void open(ImportExecutionContext context);

    EnmInventoryPage fetchFirstPage(ImportExecutionContext context, int pageSize);

    EnmInventoryPage fetchContinuation(ImportExecutionContext context, String continuationToken, int pageSize);

    default boolean supportsIncremental() {
        return false;
    }

    default VendorIncrementalBatch fetchIncremental(SynchronizationExecutionContext context) {
        throw new VendorConnectorException(
                com.simba.snip.npo.integration.ImportFailureCode.INCREMENTAL_NOT_SUPPORTED,
                "incremental synchronization is not supported by this transport"
        );
    }

    Duration lastRetryAfter();

    @Override
    void close();
}
