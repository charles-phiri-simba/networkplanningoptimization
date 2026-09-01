-- Phase 10: vault/identity failure codes. No secret-value columns.

ALTER TABLE network_import_batch DROP CONSTRAINT network_import_batch_failure_code_chk;
ALTER TABLE network_import_batch ADD CONSTRAINT network_import_batch_failure_code_chk CHECK (
    failure_code IS NULL OR failure_code IN (
        'ADAPTER_ERROR',
        'SNAPSHOT_READ_FAILED',
        'SCHEMA_UNSUPPORTED',
        'VALIDATION_FATAL',
        'LEASE_UNAVAILABLE',
        'LEASE_LOST',
        'LEASE_EXPIRED',
        'EXECUTION_TIMEOUT',
        'RECONCILIATION_FAILED',
        'DATABASE_COMMIT_FAILED',
        'SNAPSHOT_ID_CONTENT_MISMATCH',
        'CREDENTIAL_RESOLUTION_FAILED',
        'CONNECTOR_AUTHENTICATION_FAILED',
        'TLS_TRUST_FAILED',
        'CONNECTOR_AUTHORIZATION_DENIED',
        'NETWORK_POLICY_DENIED',
        'CONNECTOR_DISABLED',
        'VAULT_AUTHENTICATION_FAILED',
        'VAULT_ACCESS_DENIED',
        'VAULT_SECRET_NOT_FOUND',
        'VAULT_SECRET_DISABLED',
        'VAULT_UNAVAILABLE',
        'TRUST_MATERIAL_RESOLUTION_FAILED'
    )
);
