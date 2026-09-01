package com.simba.snip.npo.domain;

import java.util.UUID;

public class ImportBusyException extends DomainConflictException {

    private final UUID activeExecutionId;
    private final String failureCode;

    public ImportBusyException(String message, UUID activeExecutionId, String failureCode) {
        super(message);
        this.activeExecutionId = activeExecutionId;
        this.failureCode = failureCode;
    }

    public UUID getActiveExecutionId() {
        return activeExecutionId;
    }

    public String getFailureCode() {
        return failureCode;
    }
}
