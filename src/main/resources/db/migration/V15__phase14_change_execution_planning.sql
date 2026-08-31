-- Phase 14: governed change planning and execution readiness. No secret or vendor-command columns.

CREATE TABLE network_change_plan (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES network_change_proposal (id),
    resolved_candidate_id UUID,
    status VARCHAR(32) NOT NULL,
    plan_version INTEGER NOT NULL DEFAULT 1,
    target_entity_type VARCHAR(32) NOT NULL,
    target_entity_id VARCHAR(128) NOT NULL,
    parameter_name VARCHAR(64) NOT NULL,
    expected_current_value VARCHAR(32) NOT NULL,
    desired_value VARCHAR(32) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    authorized_fingerprint VARCHAR(64),
    source_system VARCHAR(64) NOT NULL,
    source_snapshot_id VARCHAR(128),
    source_synchronization_execution_id UUID,
    knowledge_confidence_at_creation VARCHAR(16) NOT NULL,
    knowledge_reason_codes VARCHAR(512) NOT NULL,
    impact_level VARCHAR(16) NOT NULL,
    risk_level VARCHAR(16),
    risk_reason_codes VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ,
    reviewed_by VARCHAR(128),
    authorized_at TIMESTAMPTZ,
    authorized_by VARCHAR(128),
    cancelled_at TIMESTAMPTZ,
    cancelled_by VARCHAR(128),
    expires_at TIMESTAMPTZ NOT NULL,
    invalidated_at TIMESTAMPTZ,
    invalidation_reason VARCHAR(64),
    predecessor_id UUID,
    superseded_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(128) NOT NULL
);

CREATE INDEX network_change_plan_proposal_idx ON network_change_plan (proposal_id);
CREATE INDEX network_change_plan_status_idx ON network_change_plan (status);
CREATE UNIQUE INDEX network_change_plan_active_proposal_idx ON network_change_plan (proposal_id)
    WHERE status IN ('DRAFT', 'VALIDATING', 'PLANNED', 'SAFETY_EVALUATING', 'READY_FOR_REVIEW', 'AUTHORIZED', 'READY_FOR_EXECUTION');

CREATE TABLE network_change_plan_operation (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES network_change_plan (id),
    sequence_number INTEGER NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    target_entity_type VARCHAR(32) NOT NULL,
    target_entity_id VARCHAR(128) NOT NULL,
    parameter_name VARCHAR(64) NOT NULL,
    expected_current_value VARCHAR(32) NOT NULL,
    desired_value VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX network_change_plan_operation_plan_idx ON network_change_plan_operation (plan_id);

CREATE TABLE network_change_plan_precondition (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES network_change_plan (id),
    precondition_type VARCHAR(64) NOT NULL,
    expected_condition VARCHAR(512) NOT NULL,
    observed_value VARCHAR(512),
    result VARCHAR(16) NOT NULL,
    reason_code VARCHAR(64),
    checked_at TIMESTAMPTZ,
    evidence_reference VARCHAR(256),
    sequence_number INTEGER NOT NULL
);

CREATE INDEX network_change_plan_precondition_plan_idx ON network_change_plan_precondition (plan_id);

CREATE TABLE network_change_plan_rollback_operation (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES network_change_plan (id),
    sequence_number INTEGER NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    target_entity_type VARCHAR(32) NOT NULL,
    target_entity_id VARCHAR(128) NOT NULL,
    parameter_name VARCHAR(64) NOT NULL,
    expected_current_value VARCHAR(32) NOT NULL,
    desired_value VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX network_change_plan_rollback_plan_idx ON network_change_plan_rollback_operation (plan_id);

CREATE TABLE network_change_plan_operation_dependency (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES network_change_plan (id),
    operation_id UUID NOT NULL,
    depends_on_operation_id UUID NOT NULL
);

CREATE INDEX network_change_plan_operation_dependency_plan_idx ON network_change_plan_operation_dependency (plan_id);

CREATE TABLE network_change_plan_review (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES network_change_plan (id),
    reviewer VARCHAR(128) NOT NULL,
    comment VARCHAR(512),
    plan_version BIGINT NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX network_change_plan_review_plan_idx ON network_change_plan_review (plan_id);

CREATE TABLE network_change_plan_readiness_assessment (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES network_change_plan (id),
    assessed_at TIMESTAMPTZ NOT NULL,
    result VARCHAR(16) NOT NULL,
    assessed_fingerprint VARCHAR(64) NOT NULL,
    reason_codes VARCHAR(512),
    evidence_summary VARCHAR(1024)
);

CREATE INDEX network_change_plan_readiness_plan_idx ON network_change_plan_readiness_assessment (plan_id);

CREATE TABLE network_change_plan_audit_event (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    details VARCHAR(1024),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX network_change_plan_audit_plan_idx ON network_change_plan_audit_event (plan_id);
