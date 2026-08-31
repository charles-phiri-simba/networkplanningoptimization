-- Phase 15: governed network change execution, verification and recovery. No secret or vendor-command columns.

CREATE TABLE network_change_execution (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES network_change_plan (id),
    plan_version INTEGER NOT NULL,
    plan_fingerprint VARCHAR(64) NOT NULL,
    execution_target_id VARCHAR(128) NOT NULL,
    execution_target_type VARCHAR(32) NOT NULL,
    execution_target_environment VARCHAR(32) NOT NULL,
    adapter_profile_id VARCHAR(64) NOT NULL,
    capability_profile_version VARCHAR(32) NOT NULL,
    cell_id VARCHAR(128) NOT NULL,
    parameter_name VARCHAR(64) NOT NULL,
    execution_fingerprint VARCHAR(64) NOT NULL,
    authorized_execution_fingerprint VARCHAR(64),
    status VARCHAR(48) NOT NULL,
    requested_by VARCHAR(128) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    reviewed_by VARCHAR(128),
    reviewed_at TIMESTAMPTZ,
    authorized_by VARCHAR(128),
    authorized_at TIMESTAMPTZ,
    admitted_at TIMESTAMPTZ,
    execution_window_opens_at TIMESTAMPTZ,
    execution_window_closes_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    lease_key VARCHAR(256),
    fencing_token BIGINT,
    failure_code VARCHAR(64),
    failure_detail_safe VARCHAR(1024),
    verification_status VARCHAR(32),
    verification_completed_at TIMESTAMPTZ,
    recovery_status VARCHAR(32),
    rollback_status VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX network_change_execution_plan_idx ON network_change_execution (plan_id);
CREATE INDEX network_change_execution_status_idx ON network_change_execution (status);
CREATE INDEX network_change_execution_target_scope_idx
    ON network_change_execution (execution_target_id, cell_id, parameter_name);

CREATE UNIQUE INDEX network_change_execution_active_plan_idx ON network_change_execution (plan_id)
    WHERE status IN (
        'REQUESTED', 'PRELIMINARY_ADMISSION_CHECKING', 'READY_FOR_REVIEW', 'REVIEWED',
        'READY_FOR_EXECUTION_AUTHORIZATION', 'AUTHORIZED', 'FINAL_PREFLIGHT_CHECKING',
        'EXECUTING', 'APPLIED', 'EXECUTION_OUTCOME_UNKNOWN', 'VERIFYING',
        'RECOVERY_REQUIRED', 'ROLLBACK_REQUESTED', 'ROLLBACK_REVIEWED', 'ROLLBACK_AUTHORIZED',
        'ROLLING_BACK', 'ROLLBACK_APPLIED', 'ROLLBACK_OUTCOME_UNKNOWN'
    );

CREATE UNIQUE INDEX network_change_execution_active_scope_idx
    ON network_change_execution (execution_target_id, cell_id, parameter_name)
    WHERE status IN (
        'REQUESTED', 'PRELIMINARY_ADMISSION_CHECKING', 'READY_FOR_REVIEW', 'REVIEWED',
        'READY_FOR_EXECUTION_AUTHORIZATION', 'AUTHORIZED', 'FINAL_PREFLIGHT_CHECKING',
        'EXECUTING', 'APPLIED', 'EXECUTION_OUTCOME_UNKNOWN', 'VERIFYING',
        'RECOVERY_REQUIRED', 'ROLLBACK_REQUESTED', 'ROLLBACK_REVIEWED', 'ROLLBACK_AUTHORIZED',
        'ROLLING_BACK', 'ROLLBACK_APPLIED', 'ROLLBACK_OUTCOME_UNKNOWN'
    );

CREATE TABLE network_change_execution_operation (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES network_change_execution (id),
    sequence_number INTEGER NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    target_entity_type VARCHAR(32) NOT NULL,
    target_entity_id VARCHAR(128) NOT NULL,
    parameter_name VARCHAR(64) NOT NULL,
    expected_current_value VARCHAR(32) NOT NULL,
    desired_value VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX network_change_execution_operation_execution_idx
    ON network_change_execution_operation (execution_id);

CREATE TABLE network_change_execution_attempt (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES network_change_execution (id),
    attempt_number INTEGER NOT NULL,
    direction VARCHAR(16) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failure_code VARCHAR(64),
    failure_detail_safe VARCHAR(1024),
    target_revision_before BIGINT,
    target_revision_after BIGINT
);

CREATE INDEX network_change_execution_attempt_execution_idx
    ON network_change_execution_attempt (execution_id);

CREATE TABLE network_change_execution_authorization (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES network_change_execution (id),
    authorization_type VARCHAR(32) NOT NULL,
    authorizer VARCHAR(128) NOT NULL,
    authorized_fingerprint VARCHAR(64) NOT NULL,
    authorized_at TIMESTAMPTZ NOT NULL,
    execution_version BIGINT NOT NULL
);

CREATE INDEX network_change_execution_authorization_execution_idx
    ON network_change_execution_authorization (execution_id);

CREATE TABLE network_change_execution_verification (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES network_change_execution (id),
    attempt_id UUID REFERENCES network_change_execution_attempt (id),
    direction VARCHAR(16) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    observed_value VARCHAR(32),
    expected_value VARCHAR(32),
    target_revision BIGINT,
    observed_at TIMESTAMPTZ NOT NULL,
    reason_code VARCHAR(64),
    evidence_summary VARCHAR(1024)
);

CREATE INDEX network_change_execution_verification_execution_idx
    ON network_change_execution_verification (execution_id);

CREATE TABLE network_change_execution_recovery (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES network_change_execution (id),
    evaluated_at TIMESTAMPTZ NOT NULL,
    recovery_status VARCHAR(32) NOT NULL,
    rollback_eligible BOOLEAN NOT NULL,
    reason_codes VARCHAR(512),
    evidence_summary VARCHAR(1024)
);

CREATE INDEX network_change_execution_recovery_execution_idx
    ON network_change_execution_recovery (execution_id);

CREATE TABLE network_change_execution_rollback (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES network_change_execution (id),
    status VARCHAR(32) NOT NULL,
    requested_by VARCHAR(128),
    requested_at TIMESTAMPTZ,
    reviewed_by VARCHAR(128),
    reviewed_at TIMESTAMPTZ,
    authorized_by VARCHAR(128),
    authorized_at TIMESTAMPTZ,
    authorized_rollback_fingerprint VARCHAR(64),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failure_code VARCHAR(64),
    failure_detail_safe VARCHAR(1024)
);

CREATE UNIQUE INDEX network_change_execution_rollback_execution_idx
    ON network_change_execution_rollback (execution_id);

CREATE TABLE network_change_execution_audit_event (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    details VARCHAR(1024),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX network_change_execution_audit_execution_idx
    ON network_change_execution_audit_event (execution_id);

CREATE TABLE network_change_execution_lease (
    lease_key VARCHAR(256) PRIMARY KEY,
    target_id VARCHAR(128) NOT NULL,
    cell_id VARCHAR(128) NOT NULL,
    parameter_name VARCHAR(64) NOT NULL,
    owner_execution_id UUID NOT NULL REFERENCES network_change_execution (id),
    owner_instance_id VARCHAR(64) NOT NULL,
    fencing_token BIGINT NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL,
    heartbeat_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT network_change_execution_lease_scope_unique UNIQUE (target_id, cell_id, parameter_name)
);

CREATE INDEX network_change_execution_lease_expiry_idx ON network_change_execution_lease (expires_at);

CREATE TABLE simulator_execution_cell_state (
    id UUID PRIMARY KEY,
    target_id VARCHAR(128) NOT NULL,
    cell_id VARCHAR(128) NOT NULL,
    parameter_name VARCHAR(64) NOT NULL,
    parameter_value VARCHAR(32) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT simulator_execution_cell_state_scope_unique UNIQUE (target_id, cell_id, parameter_name)
);

CREATE INDEX simulator_execution_cell_state_target_idx ON simulator_execution_cell_state (target_id);
