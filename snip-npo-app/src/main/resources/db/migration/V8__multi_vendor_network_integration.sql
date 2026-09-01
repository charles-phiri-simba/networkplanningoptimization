-- Phase 7: fixture-first read-only multi-vendor integration metadata.
-- Canonical Site/gNB/Cell/configuration/neighbour remain in existing operational tables.
-- No Ericsson/Nokia operational tables and no raw vendor payload archive.

CREATE TABLE network_source (
    id UUID PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL UNIQUE,
    vendor VARCHAR(32) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    read_only BOOLEAN NOT NULL,
    enabled BOOLEAN NOT NULL,
    schema_version VARCHAR(64) NOT NULL,
    CONSTRAINT network_source_vendor_chk CHECK (vendor IN ('ERICSSON', 'NOKIA')),
    CONSTRAINT network_source_mode_chk CHECK (mode IN ('FIXTURE'))
);

INSERT INTO network_source (id, source_system, vendor, mode, read_only, enabled, schema_version) VALUES
    ('00000000-0000-4000-a000-000000000071', 'ERICSSON_FIXTURE', 'ERICSSON', 'FIXTURE', TRUE, TRUE, 'ERICSSON_FIXTURE_V1'),
    ('00000000-0000-4000-a000-000000000072', 'NOKIA_FIXTURE', 'NOKIA', 'FIXTURE', TRUE, TRUE, 'NOKIA_FIXTURE_V1');

CREATE TABLE network_import_batch (
    id UUID PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL,
    vendor VARCHAR(32) NOT NULL,
    source_snapshot_id VARCHAR(128) NOT NULL,
    vendor_schema_version VARCHAR(64) NOT NULL,
    fixture_kind VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    entities_read INTEGER NOT NULL DEFAULT 0,
    entities_created INTEGER NOT NULL DEFAULT 0,
    entities_updated INTEGER NOT NULL DEFAULT 0,
    entities_unchanged INTEGER NOT NULL DEFAULT 0,
    entities_rejected INTEGER NOT NULL DEFAULT 0,
    conflicts_detected INTEGER NOT NULL DEFAULT 0,
    missing_entities_detected INTEGER NOT NULL DEFAULT 0,
    error TEXT,
    CONSTRAINT network_import_batch_status_chk CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED')),
    CONSTRAINT network_import_batch_vendor_chk CHECK (vendor IN ('ERICSSON', 'NOKIA'))
);

CREATE INDEX network_import_batch_started_idx ON network_import_batch (started_at DESC);

CREATE TABLE network_source_reference (
    id UUID PRIMARY KEY,
    canonical_entity_type VARCHAR(32) NOT NULL,
    canonical_entity_id VARCHAR(64) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    vendor VARCHAR(32) NOT NULL,
    source_entity_type VARCHAR(32) NOT NULL,
    source_entity_id VARCHAR(128) NOT NULL,
    source_dn VARCHAR(256),
    authoritative BOOLEAN NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    last_source_snapshot_id VARCHAR(128) NOT NULL,
    vendor_schema_version VARCHAR(64) NOT NULL,
    source_observed_at TIMESTAMPTZ NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL,
    import_batch_id UUID NOT NULL REFERENCES network_import_batch (id),
    source_status VARCHAR(32) NOT NULL,
    CONSTRAINT network_source_reference_status_chk CHECK (source_status IN ('ACTIVE', 'MISSING')),
    CONSTRAINT network_source_reference_type_chk CHECK (
        canonical_entity_type IN ('SITE', 'GNB', 'CELL', 'CELL_CONFIGURATION', 'NEIGHBOUR')
    ),
    CONSTRAINT network_source_reference_unique UNIQUE (
        canonical_entity_type, canonical_entity_id, source_system, source_entity_id
    )
);

CREATE UNIQUE INDEX network_source_reference_auth_unique
    ON network_source_reference (canonical_entity_type, canonical_entity_id)
    WHERE authoritative;

CREATE INDEX network_source_reference_source_idx
    ON network_source_reference (source_system, source_status);

CREATE TABLE network_import_rejection (
    id UUID PRIMARY KEY,
    import_id UUID NOT NULL REFERENCES network_import_batch (id),
    source_entity_id VARCHAR(128),
    entity_type VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    details VARCHAR(1024) NOT NULL,
    rejected_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT network_import_rejection_reason_chk CHECK (reason_code IN (
        'MISSING_SOURCE_ID',
        'MISSING_CANONICAL_ID',
        'DUPLICATE_SOURCE_IDENTITY',
        'UNSUPPORTED_TECHNOLOGY',
        'UNSUPPORTED_DUPLEX',
        'INVALID_UNIT',
        'INVALID_TX_POWER',
        'MISSING_PARENT',
        'INVALID_NEIGHBOUR',
        'MALFORMED_RELATIONSHIP'
    ))
);

CREATE INDEX network_import_rejection_import_idx ON network_import_rejection (import_id);

CREATE TABLE network_integration_conflict (
    id UUID PRIMARY KEY,
    import_id UUID NOT NULL REFERENCES network_import_batch (id),
    entity_type VARCHAR(32) NOT NULL,
    canonical_entity_id VARCHAR(64) NOT NULL,
    conflict_scope VARCHAR(64) NOT NULL,
    current_value VARCHAR(256) NOT NULL,
    incoming_value VARCHAR(256) NOT NULL,
    authoritative_source VARCHAR(64) NOT NULL,
    incoming_source VARCHAR(64) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT network_integration_conflict_status_chk CHECK (status IN ('OPEN')),
    CONSTRAINT network_integration_conflict_reason_chk CHECK (reason_code IN (
        'SECOND_SOURCE_VALUE_MISMATCH',
        'SECOND_SOURCE_CLAIM'
    ))
);

CREATE INDEX network_integration_conflict_import_idx ON network_integration_conflict (import_id);
CREATE INDEX network_integration_conflict_entity_idx ON network_integration_conflict (canonical_entity_id);

CREATE TABLE network_import_audit_event (
    id UUID PRIMARY KEY,
    import_id UUID NOT NULL REFERENCES network_import_batch (id),
    event_type VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    details TEXT NOT NULL,
    CONSTRAINT network_import_audit_event_type_chk CHECK (event_type IN (
        'IMPORT_STARTED',
        'SNAPSHOT_READ',
        'VALIDATION_COMPLETED',
        'RECONCILIATION_COMPLETED',
        'IMPORT_COMPLETED',
        'IMPORT_FAILED'
    ))
);

CREATE INDEX network_import_audit_event_import_idx ON network_import_audit_event (import_id, occurred_at);
