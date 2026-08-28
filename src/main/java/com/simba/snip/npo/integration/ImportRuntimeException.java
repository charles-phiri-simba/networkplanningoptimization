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
            case SCHEMA_UNSUPPORTED, VALIDATION_FATAL, SNAPSHOT_ID_CONTENT_MISMATCH,
                    CONNECTOR_AUTHENTICATION_FAILED, TLS_TRUST_FAILED, CONNECTOR_AUTHORIZATION_DENIED,
                    NETWORK_POLICY_DENIED, CONNECTOR_DISABLED, VAULT_AUTHENTICATION_FAILED,
                    VAULT_ACCESS_DENIED, VAULT_SECRET_NOT_FOUND, VAULT_SECRET_DISABLED,
                    TRUST_MATERIAL_RESOLUTION_FAILED, VENDOR_AUTHENTICATION_FAILED,
                    VENDOR_AUTHORIZATION_DENIED, VENDOR_RESPONSE_INVALID, VENDOR_PAGINATION_INVALID,
                    VENDOR_PROTOCOL_ERROR, SNAPSHOT_LIMIT_EXCEEDED, SNAPSHOT_PARTIAL,
                    CONNECTOR_CANCELLED, PRODUCTION_TRANSPORT_NOT_CONFIGURED -> false;
            case ADAPTER_ERROR, SNAPSHOT_READ_FAILED, LEASE_UNAVAILABLE, LEASE_LOST, LEASE_EXPIRED,
                    EXECUTION_TIMEOUT, RECONCILIATION_FAILED, DATABASE_COMMIT_FAILED,
                    CREDENTIAL_RESOLUTION_FAILED, VAULT_UNAVAILABLE, VENDOR_UNAVAILABLE,
                    VENDOR_RATE_LIMITED, VENDOR_TIMEOUT -> true;
        };
    }
}
