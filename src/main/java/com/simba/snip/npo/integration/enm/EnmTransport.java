package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.ericsson.enm.EnmInventoryPage;

import java.time.Duration;

public interface EnmTransport extends AutoCloseable {

    void open(ImportExecutionContext context);

    EnmInventoryPage fetchFirstPage(ImportExecutionContext context, int pageSize);

    EnmInventoryPage fetchContinuation(ImportExecutionContext context, String continuationToken, int pageSize);

    Duration lastRetryAfter();

    @Override
    void close();
}
