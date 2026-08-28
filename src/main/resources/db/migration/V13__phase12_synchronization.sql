-- Phase 12: synchronization checkpoints, source state, drift, knowledge confidence. No secret columns.

CREATE TABLE synchronization_checkpoint (
    id UUID PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL,
    connector_id VARCHAR(128) NOT NULL,
    synchronization_scope VARCHAR(64) NOT NULL,
    checkpoint_type VARCHAR(32) NOT NULL,
    checkpoint_value VARCHAR(256) NOT NULL,
    source_version VARCHAR(64),
    last_successful_execution_id UUID,
    last_successful_snapshot_id VARCHAR(128),
    last_successful_started_at TIMESTAMPTZ,
    last_successful_completed_at TIMESTAMPTZ,
    last_observed_at TIMESTAMPTZ,
    synchronization_mode VARCHAR(32),
    completeness VARCHAR(16),
    fencing_token BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT synchronization_checkpoint_scope_uq UNIQUE (source_system, synchronization_scope)
);

CREATE INDEX synchronization_checkpoint_connector_idx ON synchronization_checkpoint (connector_id);

CREATE TABLE synchronization_source_state (
    id UUID PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL,
    connector_id VARCHAR(128) NOT NULL,
    synchronization_scope VARCHAR(64) NOT NULL,
    freshness VARCHAR(16) NOT NULL,
    source_health VARCHAR(32) NOT NULL,
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    last_started_at TIMESTAMPTZ,
    last_completed_at TIMESTAMPTZ,
    latest_completed_execution_id UUID,
    latest_fencing_token BIGINT NOT NULL DEFAULT 0,
    recovery_required BOOLEAN NOT NULL DEFAULT FALSE,
    overlap_skipped_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT synchronization_source_state_scope_uq UNIQUE (source_system, synchronization_scope)
);

CREATE TABLE network_drift_observation (
    id UUID PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL,
    connector_id VARCHAR(128) NOT NULL,
    synchronization_scope VARCHAR(64) NOT NULL,
    drift_type VARCHAR(32) NOT NULL,
    drift_status VARCHAR(16) NOT NULL,
    entity_type VARCHAR(32),
    entity_id VARCHAR(128),
    execution_id UUID,
    fencing_token BIGINT,
    summary VARCHAR(512) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolution_execution_id UUID
);

CREATE INDEX network_drift_observation_scope_idx ON network_drift_observation (source_system, synchronization_scope);
CREATE INDEX network_drift_observation_status_idx ON network_drift_observation (drift_status);

CREATE TABLE network_knowledge_status (
    id UUID PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL,
    connector_id VARCHAR(128) NOT NULL,
    synchronization_scope VARCHAR(64) NOT NULL,
    confidence VARCHAR(16) NOT NULL,
    reason_codes VARCHAR(512) NOT NULL,
    freshness VARCHAR(16) NOT NULL,
    source_health VARCHAR(32) NOT NULL,
    last_trusted_snapshot_id VARCHAR(128),
    last_trusted_synchronization_at TIMESTAMPTZ,
    fencing_token BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT network_knowledge_status_scope_uq UNIQUE (source_system, synchronization_scope)
);

ALTER TABLE network_import_batch ADD COLUMN IF NOT EXISTS synchronization_mode VARCHAR(32);
ALTER TABLE network_import_batch ADD COLUMN IF NOT EXISTS synchronization_initiator VARCHAR(16);

ALTER TABLE network_import_audit_event DROP CONSTRAINT IF EXISTS network_import_audit_event_type_chk;
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
    'SESSION_COMPLETED',
    'SYNCHRONIZATION_DUE',
    'SYNCHRONIZATION_STARTED',
    'OVERLAP_SKIPPED',
    'CHECKPOINT_LOADED',
    'MODE_SELECTED',
    'CHECKPOINT_ADVANCED',
    'CHECKPOINT_UNCERTAIN',
    'SYNCHRONIZATION_COMPLETED',
    'SYNCHRONIZATION_FAILED',
    'RECOVERY_REQUIRED',
    'DRIFT_DETECTED',
    'DRIFT_RESOLVED'
));

ALTER TABLE network_import_batch DROP CONSTRAINT IF EXISTS network_import_batch_failure_code_chk;
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
        'PRODUCTION_TRANSPORT_NOT_CONFIGURED',
        'SYNCHRONIZATION_DISABLED',
        'INCREMENTAL_NOT_SUPPORTED',
        'CHECKPOINT_REJECTED',
        'CHECKPOINT_EXPIRED',
        'SEQUENCE_GAP',
        'RECOVERY_REQUIRED',
        'CHECKPOINT_UNCERTAIN'
    )
);
