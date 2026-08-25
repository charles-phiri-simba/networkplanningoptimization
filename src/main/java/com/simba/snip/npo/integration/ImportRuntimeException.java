package com.simba.snip.npo.integration;

public class ImportRuntimeException extends RuntimeException {

    private final ImportFailureCode failureCode;
    private final boolean retryable;

    public ImportRuntimeException(ImportFailureCode failureCode, String message) {
        this(failureCode, message, retryableDefault(failureCode), null);
    }

    public ImportRuntimeException(ImportFailureCode failureCode, String message, Throwable cause) {
        this(failureCode, message, retryableDefault(failureCode), cause);
    }

    public ImportRuntimeException(ImportFailureCode failureCode, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.failureCode = failureCode;
        this.retryable = retryable;
    }

    public ImportFailureCode failureCode() {
        return failureCode;
    }

    public boolean retryable() {
        return retryable;
    }

    public static boolean retryableDefault(ImportFailureCode code) {
        return switch (code) {
            case SCHEMA_UNSUPPORTED, VALIDATION_FATAL, SNAPSHOT_ID_CONTENT_MISMATCH -> false;
            case ADAPTER_ERROR, SNAPSHOT_READ_FAILED, LEASE_UNAVAILABLE, LEASE_LOST, LEASE_EXPIRED,
                    EXECUTION_TIMEOUT, RECONCILIATION_FAILED, DATABASE_COMMIT_FAILED -> true;
        };
    }
}
