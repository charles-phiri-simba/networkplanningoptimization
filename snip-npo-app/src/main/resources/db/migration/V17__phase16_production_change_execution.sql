-- Phase 16: production change governance, grants, gateway attempts, audit chain.
-- No secret columns. No vendor command payloads. No production endpoints.

CREATE TABLE production_network_target (
    target_id VARCHAR(128) PRIMARY KEY,
    vendor VARCHAR(32) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    environment VARCHAR(32) NOT NULL,
    region VARCHAR(64),
    network_domain VARCHAR(64),
    adapter_profile_id VARCHAR(64) NOT NULL,
    capability_profile_version VARCHAR(32) NOT NULL,
    security_profile_id VARCHAR(64) NOT NULL,
    credential_profile_id VARCHAR(128) NOT NULL,
    allowed_object_types VARCHAR(256) NOT NULL,
    allowed_parameters VARCHAR(256) NOT NULL,
    change_window_policy VARCHAR(1024),
    rollback_policy VARCHAR(1024),
    verification_policy VARCHAR(1024),
    certification_level VARCHAR(8) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    target_state VARCHAR(16) NOT NULL,
    target_fingerprint VARCHAR(64) NOT NULL,
    expected_state_guard_strength VARCHAR(32) NOT NULL DEFAULT 'READ_THEN_WRITE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE production_network_change (
    production_change_id UUID PRIMARY KEY,
    phase15_execution_id UUID NOT NULL,
    production_target_id VARCHAR(128) NOT NULL REFERENCES production_network_target (target_id),
    change_control_reference VARCHAR(256) NOT NULL,
    status VARCHAR(64) NOT NULL,
    production_fingerprint VARCHAR(64),
    authorization_generation INTEGER NOT NULL DEFAULT 0,
    phase14_plan_id UUID,
    phase14_plan_fingerprint VARCHAR(64),
    phase15_execution_fingerprint VARCHAR(64),
    cell_id VARCHAR(128) NOT NULL,
    parameter VARCHAR(64) NOT NULL,
    expected_value NUMERIC NOT NULL,
    desired_value NUMERIC NOT NULL,
    rollback_expected_value NUMERIC,
    rollback_desired_value NUMERIC,
    requester_principal_id VARCHAR(128) NOT NULL,
    reviewer_principal_id VARCHAR(128),
    authorizer_principal_id VARCHAR(128),
    executor_principal_id VARCHAR(128),
    reason_code VARCHAR(128),
    audit_chain_integrity VARCHAR(16) NOT NULL DEFAULT 'VALID',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX production_network_change_execution_idx ON production_network_change (phase15_execution_id);
CREATE INDEX production_network_change_target_idx ON production_network_change (production_target_id);
CREATE INDEX production_network_change_status_idx ON production_network_change (status);

CREATE TABLE production_change_review (
    review_id UUID PRIMARY KEY,
    production_change_id UUID NOT NULL REFERENCES production_network_change (production_change_id),
    reviewer_principal_id VARCHAR(128) NOT NULL,
    decision VARCHAR(16) NOT NULL,
    reason_codes VARCHAR(1024),
    reviewed_at TIMESTAMPTZ NOT NULL,
    production_fingerprint_at_review VARCHAR(64)
);

CREATE INDEX production_change_review_change_idx ON production_change_review (production_change_id);

CREATE TABLE production_change_authorization (
    authorization_id UUID PRIMARY KEY,
    production_change_id UUID NOT NULL REFERENCES production_network_change (production_change_id),
    authorizer_principal_id VARCHAR(128) NOT NULL,
    authorization_generation INTEGER NOT NULL,
    production_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    authorized_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ
);

CREATE INDEX production_change_authorization_change_idx ON production_change_authorization (production_change_id);

CREATE TABLE production_change_control (
    control_id UUID PRIMARY KEY,
    production_change_id UUID NOT NULL REFERENCES production_network_change (production_change_id),
    system VARCHAR(32) NOT NULL,
    reference VARCHAR(256) NOT NULL,
    status VARCHAR(16) NOT NULL,
    validated_by_principal_id VARCHAR(128) NOT NULL,
    validated_at TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ
);

CREATE INDEX production_change_control_change_idx ON production_change_control (production_change_id);

CREATE TABLE production_execution_grant (
    grant_id UUID PRIMARY KEY,
    production_change_id UUID NOT NULL REFERENCES production_network_change (production_change_id),
    phase15_execution_id UUID NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    grant_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    production_fingerprint VARCHAR(64) NOT NULL,
    authorization_generation INTEGER NOT NULL,
    fencing_token BIGINT NOT NULL,
    operation_binding_hash VARCHAR(64) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX production_execution_grant_change_idx ON production_execution_grant (production_change_id);
CREATE INDEX production_execution_grant_execution_idx ON production_execution_grant (phase15_execution_id);
CREATE INDEX production_execution_grant_expiry_idx ON production_execution_grant (expires_at) WHERE status = 'ISSUED';

CREATE UNIQUE INDEX production_execution_grant_one_active_forward_idx
    ON production_execution_grant (production_change_id, grant_type, operation_binding_hash)
    WHERE status = 'ISSUED' AND grant_type = 'FORWARD';

CREATE UNIQUE INDEX production_execution_grant_one_active_rollback_idx
    ON production_execution_grant (production_change_id, grant_type, operation_binding_hash)
    WHERE status = 'ISSUED' AND grant_type = 'ROLLBACK';

CREATE TABLE production_gateway_attempt (
    attempt_id UUID PRIMARY KEY,
    grant_id UUID NOT NULL REFERENCES production_execution_grant (grant_id),
    production_change_id UUID NOT NULL REFERENCES production_network_change (production_change_id),
    production_target_id VARCHAR(128) NOT NULL,
    status VARCHAR(48) NOT NULL,
    send_phase VARCHAR(32) NOT NULL,
    mutation_outcome VARCHAR(32),
    operation_binding_hash VARCHAR(64) NOT NULL,
    fencing_token BIGINT NOT NULL,
    production_fingerprint VARCHAR(64) NOT NULL,
    gateway_instance_id VARCHAR(64),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX production_gateway_attempt_grant_idx ON production_gateway_attempt (grant_id);
CREATE INDEX production_gateway_attempt_change_idx ON production_gateway_attempt (production_change_id);

CREATE TABLE production_gateway_evidence (
    evidence_id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES production_gateway_attempt (attempt_id),
    evidence_type VARCHAR(32) NOT NULL,
    evidence_version INTEGER NOT NULL DEFAULT 1,
    payload_json JSONB NOT NULL,
    produced_at TIMESTAMPTZ NOT NULL,
    producer VARCHAR(32) NOT NULL
);

CREATE INDEX production_gateway_evidence_attempt_idx ON production_gateway_evidence (attempt_id);

CREATE TABLE production_execution_verification (
    verification_id UUID PRIMARY KEY,
    production_change_id UUID NOT NULL REFERENCES production_network_change (production_change_id),
    attempt_id UUID REFERENCES production_gateway_attempt (attempt_id),
    result VARCHAR(32) NOT NULL,
    observed_value NUMERIC,
    desired_value NUMERIC,
    verified_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX production_execution_verification_change_idx ON production_execution_verification (production_change_id);

CREATE TABLE production_execution_recovery (
    recovery_id UUID PRIMARY KEY,
    production_change_id UUID NOT NULL REFERENCES production_network_change (production_change_id),
    status VARCHAR(64) NOT NULL,
    reason_codes VARCHAR(1024),
    signaled_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX production_execution_recovery_change_idx ON production_execution_recovery (production_change_id);

CREATE TABLE production_execution_rollback (
    rollback_id UUID PRIMARY KEY,
    production_change_id UUID NOT NULL REFERENCES production_network_change (production_change_id),
    status VARCHAR(48) NOT NULL,
    rollback_fingerprint VARCHAR(64),
    authorization_generation INTEGER,
    requester_principal_id VARCHAR(128),
    reviewer_principal_id VARCHAR(128),
    authorizer_principal_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX production_execution_rollback_change_idx ON production_execution_rollback (production_change_id);

CREATE TABLE production_execution_lease (
    lease_id UUID PRIMARY KEY,
    production_target_id VARCHAR(128) NOT NULL,
    cell_id VARCHAR(128) NOT NULL,
    parameter VARCHAR(64) NOT NULL,
    holder_id VARCHAR(128) NOT NULL,
    fencing_token BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX production_execution_lease_active_scope_idx
    ON production_execution_lease (production_target_id, cell_id, parameter)
    WHERE status = 'ACTIVE';

CREATE TABLE production_target_health (
    health_id UUID PRIMARY KEY,
    production_target_id VARCHAR(128) NOT NULL REFERENCES production_network_target (target_id),
    health_state VARCHAR(16) NOT NULL,
    outcome_unknown_count INTEGER NOT NULL DEFAULT 0,
    verification_failure_count INTEGER NOT NULL DEFAULT 0,
    last_checked_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX production_target_health_target_idx ON production_target_health (production_target_id);

CREATE TABLE production_change_audit_event (
    audit_event_id UUID PRIMARY KEY,
    production_change_id UUID NOT NULL REFERENCES production_network_change (production_change_id),
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    previous_event_hash VARCHAR(64) NOT NULL,
    event_hash VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_principal_id VARCHAR(128) NOT NULL,
    reason_codes VARCHAR(1024),
    safe_payload_json JSONB NOT NULL,
    chain_integrity VARCHAR(16) NOT NULL
);

CREATE UNIQUE INDEX production_change_audit_event_seq_idx
    ON production_change_audit_event (production_change_id, sequence_number);

CREATE TABLE production_rate_limit_state (
    counter_id VARCHAR(256) PRIMARY KEY,
    scope_type VARCHAR(32) NOT NULL,
    scope_key VARCHAR(256) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    count INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- SHA-256 hexadecimal digest invariant (exactly 64 lowercase hex). VARCHAR(64)
-- is retained; fixed-width character padding is undesirable. Opaque identifiers
-- are not constrained.
ALTER TABLE production_network_target
    ADD CONSTRAINT chk_sha256_production_network_target_target_fingerprint
    CHECK (target_fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE production_network_change
    ADD CONSTRAINT chk_sha256_production_network_change_production_fingerprint
    CHECK (production_fingerprint IS NULL OR production_fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE production_network_change
    ADD CONSTRAINT chk_sha256_production_network_change_phase14_plan_fingerprint
    CHECK (phase14_plan_fingerprint IS NULL OR phase14_plan_fingerprint ~ '^[0-9a-fA-F]{64}$');

ALTER TABLE production_network_change
    ADD CONSTRAINT chk_sha256_production_network_change_phase15_execution_fingerprint
    CHECK (phase15_execution_fingerprint IS NULL OR phase15_execution_fingerprint ~ '^[0-9a-fA-F]{64}$');

ALTER TABLE production_change_review
    ADD CONSTRAINT chk_sha256_production_change_review_production_fingerprint_at_review
    CHECK (production_fingerprint_at_review IS NULL OR production_fingerprint_at_review ~ '^[0-9a-f]{64}$');

ALTER TABLE production_change_authorization
    ADD CONSTRAINT chk_sha256_production_change_authorization_production_fingerprint
    CHECK (production_fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE production_execution_grant
    ADD CONSTRAINT chk_sha256_production_execution_grant_production_fingerprint
    CHECK (production_fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE production_execution_grant
    ADD CONSTRAINT chk_sha256_production_execution_grant_operation_binding_hash
    CHECK (operation_binding_hash ~ '^[0-9a-f]{64}$');

ALTER TABLE production_gateway_attempt
    ADD CONSTRAINT chk_sha256_production_gateway_attempt_operation_binding_hash
    CHECK (operation_binding_hash ~ '^[0-9a-f]{64}$');

ALTER TABLE production_gateway_attempt
    ADD CONSTRAINT chk_sha256_production_gateway_attempt_production_fingerprint
    CHECK (production_fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE production_execution_rollback
    ADD CONSTRAINT chk_sha256_production_execution_rollback_rollback_fingerprint
    CHECK (rollback_fingerprint IS NULL OR rollback_fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE production_change_audit_event
    ADD CONSTRAINT chk_sha256_production_change_audit_event_previous_event_hash
    CHECK (previous_event_hash ~ '^[0-9a-f]{64}$');

ALTER TABLE production_change_audit_event
    ADD CONSTRAINT chk_sha256_production_change_audit_event_event_hash
    CHECK (event_hash ~ '^[0-9a-f]{64}$');
