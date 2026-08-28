package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportLease;
import com.simba.snip.npo.integration.ImportLeaseService;
import com.simba.snip.npo.integration.ImportRuntimeException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class ImportExecutionContext {

    private final UUID executionId;
    private final Instant deadline;
    private final ImportLease lease;
    private final ImportLeaseService leaseService;
    private final ConnectorCancellationToken cancellationToken;
    private final Duration requestTimeout;

    public ImportExecutionContext(
            UUID executionId,
            Instant deadline,
            ImportLease lease,
            ImportLeaseService leaseService,
            ConnectorCancellationToken cancellationToken,
            Duration requestTimeout
    ) {
        this.executionId = executionId;
        this.deadline = deadline;
        this.lease = lease;
        this.leaseService = leaseService;
        this.cancellationToken = cancellationToken;
        this.requestTimeout = requestTimeout;
    }

    public UUID executionId() {
        return executionId;
    }

    public Instant deadline() {
        return deadline;
    }

    public ImportLease lease() {
        return lease;
    }

    public ConnectorCancellationToken cancellationToken() {
        return cancellationToken;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public Duration remaining() {
        Duration left = Duration.between(Instant.now(), deadline);
        return left.isNegative() ? Duration.ZERO : left;
    }

    public void assertContinuing() {
        cancellationToken.throwIfCancelled();
        if (remaining().isZero() || remaining().isNegative()) {
            throw new ImportRuntimeException(ImportFailureCode.VENDOR_TIMEOUT, "overall execution deadline exceeded");
        }
        leaseService.assertOwnership(lease);
    }

    public void assertBeforeRetry(Duration nextBackoff) {
        assertContinuing();
        Duration left = remaining();
        if (nextBackoff != null && left.compareTo(nextBackoff) < 0) {
            throw new ImportRuntimeException(ImportFailureCode.VENDOR_TIMEOUT, "retry cannot fit remaining execution budget");
        }
    }
}
