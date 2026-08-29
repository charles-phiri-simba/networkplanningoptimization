-- Phase 13: network change intelligence proposals. No secret columns.

CREATE TABLE network_change_proposal (
    id UUID PRIMARY KEY,
    proposal_type VARCHAR(64) NOT NULL,
    target_entity_type VARCHAR(32) NOT NULL,
    target_entity_id VARCHAR(128) NOT NULL,
    parameter_name VARCHAR(64) NOT NULL,
    current_value VARCHAR(32) NOT NULL,
    proposed_value VARCHAR(32),
    unit VARCHAR(16) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    source_snapshot_id VARCHAR(128),
    source_synchronization_execution_id UUID,
    network_knowledge_confidence VARCHAR(16) NOT NULL,
    knowledge_reason_codes VARCHAR(512) NOT NULL,
    assurance_confidence VARCHAR(16),
    simulation_confidence VARCHAR(16),
    assurance_case_id UUID,
    decision_reference VARCHAR(256),
    benefit_summary VARCHAR(512),
    benefit_score NUMERIC(12, 6),
    risk_level VARCHAR(16),
    risk_reason_codes VARCHAR(512),
    proposal_score NUMERIC(12, 6),
    status VARCHAR(32) NOT NULL,
    generation_initiator VARCHAR(32) NOT NULL,
    failure_code VARCHAR(64),
    failure_reason VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    evaluated_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    invalidated_at TIMESTAMPTZ,
    invalidation_reason VARCHAR(64),
    superseded_by UUID,
    predecessor_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(128) NOT NULL
);

CREATE INDEX network_change_proposal_target_idx ON network_change_proposal (target_entity_type, target_entity_id, parameter_name);
CREATE INDEX network_change_proposal_status_idx ON network_change_proposal (status);

CREATE TABLE network_change_candidate (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES network_change_proposal (id),
    candidate_value VARCHAR(32) NOT NULL,
    baseline_candidate BOOLEAN NOT NULL DEFAULT FALSE,
    validation_outcome VARCHAR(32) NOT NULL,
    validation_reason VARCHAR(64),
    simulation_run_id UUID,
    simulation_confidence VARCHAR(16),
    benefit_score NUMERIC(12, 6),
    benefit_reason_codes VARCHAR(512),
    risk_level VARCHAR(16),
    risk_reason_codes VARCHAR(512),
    proposal_score NUMERIC(12, 6),
    rank_order INTEGER,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX network_change_candidate_proposal_idx ON network_change_candidate (proposal_id);

CREATE TABLE change_proposal_review (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES network_change_proposal (id),
    decision VARCHAR(16) NOT NULL,
    reviewer VARCHAR(128) NOT NULL,
    reason_code VARCHAR(64),
    comment VARCHAR(512),
    proposal_version BIGINT NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX change_proposal_review_proposal_idx ON change_proposal_review (proposal_id);

CREATE TABLE change_proposal_audit_event (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    details VARCHAR(1024),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX change_proposal_audit_proposal_idx ON change_proposal_audit_event (proposal_id);
