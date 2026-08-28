-- Phase 11: vendor snapshot metadata, provenance, and vendor failure codes. No secret or raw payload columns.

CREATE TABLE vendor_snapshot (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    snapshot_id VARCHAR(128) NOT NULL,
    connector_id VARCHAR(128) NOT NULL,
    source_vendor VARCHAR(32) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    completeness VARCHAR(16) NOT NULL,
    pages_received INTEGER NOT NULL,
    entities_read INTEGER NOT NULL,
    source_version VARCHAR(64),
    warnings VARCHAR(256)
);

CREATE INDEX vendor_snapshot_execution_idx ON vendor_snapshot (execution_id);

CREATE TABLE source_provenance (
    id UUID PRIMARY KEY,
    canonical_entity_type VARCHAR(32) NOT NULL,
    canonical_entity_id VARCHAR(64) NOT NULL,
    source_vendor VARCHAR(32) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    source_object_type VARCHAR(32) NOT NULL,
    source_object_id VARCHAR(128) NOT NULL,
    source_snapshot_id VARCHAR(128) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    import_execution_id UUID NOT NULL
);

CREATE INDEX source_provenance_execution_idx ON source_provenance (import_execution_id);
CREATE INDEX source_provenance_canonical_idx ON source_provenance (canonical_entity_id);

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
        'TRUST_MATERIAL_RESOLUTION_FAILED',
        'VENDOR_UNAVAILABLE',
        'VENDOR_AUTHENTICATION_FAILED',
        'VENDOR_AUTHORIZATION_DENIED',
        'VENDOR_RATE_LIMITED',
        'VENDOR_TIMEOUT',
        'VENDOR_PROTOCOL_ERROR',
        'VENDOR_RESPONSE_INVALID',
        'VENDOR_PAGINATION_INVALID',
        'SNAPSHOT_LIMIT_EXCEEDED',
        'SNAPSHOT_PARTIAL',
        'CONNECTOR_CANCELLED',
        'PRODUCTION_TRANSPORT_NOT_CONFIGURED'
    )
);

ALTER TABLE network_import_audit_event DROP CONSTRAINT network_import_audit_event_type_chk;
ALTER TABLE network_import_audit_event ADD CONSTRAINT network_import_audit_event_type_chk CHECK (event_type IN (
    'IMPORT_STARTED',
    'SNAPSHOT_READ',
    'VALIDATION_COMPLETED',
    'RECONCILIATION_COMPLETED',
    'IMPORT_COMPLETED',
    'IMPORT_FAILED',
    'IMPORT_REJECTED',
    'IMPORT_REPLAYED',
    'IMPORT_TIMED_OUT',
    'LEASE_ACQUIRED',
    'LEASE_RELEASED',
    'IMPORT_REQUESTED',
    'CREDENTIAL_RESOLVED',
    'VENDOR_SESSION_ESTABLISHED',
    'SNAPSHOT_STARTED',
    'PAGE_RECEIVED',
    'SNAPSHOT_COMPLETED',
    'SNAPSHOT_PARTIAL',
    'VENDOR_RATE_LIMITED',
    'VENDOR_TIMEOUT',
    'VENDOR_AUTHENTICATION_FAILED',
    'VENDOR_AUTHORIZATION_DENIED',
    'VENDOR_PROTOCOL_ERROR',
    'LEASE_LOST',
    'CONNECTOR_CANCELLED',
    'SESSION_COMPLETED'
));
