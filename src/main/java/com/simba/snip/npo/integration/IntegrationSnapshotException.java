package com.simba.snip.npo.integration;

public class IntegrationSnapshotException extends RuntimeException {

    public IntegrationSnapshotException(String message) {
        super(message);
    }

    public IntegrationSnapshotException(String message, Throwable cause) {
        super(message, cause);
    }
}
