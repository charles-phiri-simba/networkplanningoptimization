-- Phase 8: durable import execution, source-scope lease, and phase checkpoints.
-- Phase 7 network_import_batch rows remain; runtime columns are additive.

ALTER TABLE network_import_batch
    ADD COLUMN execution_type VARCHAR(32) NOT NULL DEFAULT 'NEW',
    ADD COLUMN attempt_number INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN previous_execution_id UUID REFERENCES network_import_batch (id),
    ADD COLUMN original_successful_execution_id UUID REFERENCES network_import_batch (id),
    ADD COLUMN source_scope VARCHAR(64) NOT NULL DEFAULT 'DEFAULT',
    ADD COLUMN canonical_snapshot_hash VARCHAR(64),
    ADD COLUMN failure_code VARCHAR(64),
    ADD COLUMN retryable BOOLEAN,
    ADD COLUMN owner_instance_id VARCHAR(64),
    ADD COLUMN lease_fencing_token BIGINT,
    ADD COLUMN requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE network_import_batch SET requested_at = started_at WHERE requested_at IS NULL;

ALTER TABLE network_import_batch DROP CONSTRAINT network_import_batch_status_chk;
ALTER TABLE network_import_batch ADD CONSTRAINT network_import_batch_status_chk CHECK (status IN (
    'REQUESTED',
    'RUNNING',
    'COMPLETED',
    'FAILED',
    'TIMED_OUT',
    'REJECTED'
));

ALTER TABLE network_import_batch ADD CONSTRAINT network_import_batch_execution_type_chk CHECK (execution_type IN (
    'NEW',
    'RETRY',
    'REPLAY'
));

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
        'SNAPSHOT_ID_CONTENT_MISMATCH'
    )
);

CREATE INDEX network_import_batch_identity_idx
    ON network_import_batch (source_system, source_scope, source_snapshot_id, attempt_number);

CREATE INDEX network_import_batch_scope_status_idx
    ON network_import_batch (source_system, source_scope, status);

CREATE TABLE network_import_lease (
    lease_key VARCHAR(160) PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL,
    source_scope VARCHAR(64) NOT NULL,
    owner_execution_id UUID NOT NULL REFERENCES network_import_batch (id),
    owner_instance_id VARCHAR(64) NOT NULL,
    fencing_token BIGINT NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL,
    heartbeat_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT network_import_lease_scope_unique UNIQUE (source_system, source_scope)
);

CREATE INDEX network_import_lease_expiry_idx ON network_import_lease (expires_at);

CREATE TABLE network_import_checkpoint (
    checkpoint_id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES network_import_batch (id),
    checkpoint_type VARCHAR(64) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    details TEXT NOT NULL,
    CONSTRAINT network_import_checkpoint_type_chk CHECK (checkpoint_type IN (
        'SNAPSHOT_READ',
        'NORMALIZATION_COMPLETED',
        'VALIDATION_COMPLETED',
        'RECONCILIATION_COMPLETED',
        'CANONICAL_COMMIT_COMPLETED'
    ))
);

CREATE INDEX network_import_checkpoint_execution_idx
    ON network_import_checkpoint (execution_id, recorded_at);

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
    'LEASE_RELEASED'
));
