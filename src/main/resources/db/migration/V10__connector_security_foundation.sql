-- Phase 9: safe connector session metadata and append-only security audit.
-- Never persist passwords, tokens, private keys, or raw secrets.

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
        'CONNECTOR_DISABLED'
    )
);

CREATE TABLE connector_session (
    session_id UUID PRIMARY KEY,
    execution_id UUID REFERENCES network_import_batch (id),
    connector_id VARCHAR(128) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    credential_ref VARCHAR(128) NOT NULL,
    credential_version VARCHAR(64),
    trust_profile_id VARCHAR(64) NOT NULL,
    endpoint_ref VARCHAR(128) NOT NULL,
    server_certificate_fingerprint VARCHAR(128),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT connector_session_status_chk CHECK (status IN (
        'OPEN',
        'COMPLETED',
        'FAILED'
    ))
);

CREATE INDEX connector_session_execution_idx ON connector_session (execution_id);
CREATE INDEX connector_session_connector_idx ON connector_session (connector_id, started_at);

CREATE TABLE connector_security_audit_event (
    event_id UUID PRIMARY KEY,
    session_id UUID REFERENCES connector_session (session_id),
    execution_id UUID REFERENCES network_import_batch (id),
    connector_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    credential_ref VARCHAR(128),
    credential_version VARCHAR(64),
    endpoint_ref VARCHAR(128),
    trust_profile_id VARCHAR(64),
    server_certificate_fingerprint VARCHAR(128),
    failure_code VARCHAR(64),
    details TEXT NOT NULL,
    CONSTRAINT connector_security_audit_event_type_chk CHECK (event_type IN (
        'SESSION_REQUESTED',
        'CREDENTIAL_RESOLVED',
        'NETWORK_POLICY_VALIDATED',
        'TLS_VALIDATED',
        'AUTHENTICATION_SUCCEEDED',
        'AUTHENTICATION_FAILED',
        'AUTHORIZATION_DENIED',
        'SESSION_COMPLETED',
        'SESSION_FAILED'
    ))
);

CREATE INDEX connector_security_audit_session_idx
    ON connector_security_audit_event (session_id, occurred_at);
CREATE INDEX connector_security_audit_execution_idx
    ON connector_security_audit_event (execution_id, occurred_at);
