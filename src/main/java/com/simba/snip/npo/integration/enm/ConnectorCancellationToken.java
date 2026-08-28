package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportRuntimeException;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ConnectorCancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void throwIfCancelled() {
        if (cancelled.get()) {
            throw new ImportRuntimeException(
                    ImportFailureCode.CONNECTOR_CANCELLED,
                    "connector import was cancelled",
                    false,
                    null
            );
        }
    }
}
