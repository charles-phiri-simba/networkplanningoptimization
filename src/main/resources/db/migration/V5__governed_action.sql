-- Phase 4: governed ProposedAction workflow. Audit is append-only. No live-network state.

CREATE TABLE proposed_action (
    id UUID PRIMARY KEY,
    assurance_case_id UUID NOT NULL REFERENCES assurance_case (id),
    action_type VARCHAR(64) NOT NULL,
    capability_id VARCHAR(128),
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    parameters TEXT NOT NULL,
    rationale VARCHAR(1024) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    policy_decision VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    proposed_at TIMESTAMPTZ NOT NULL,
    proposed_by VARCHAR(64) NOT NULL,
    executed_by VARCHAR(64),
    synthetic BOOLEAN NOT NULL,
    CONSTRAINT proposed_action_type_chk CHECK (action_type IN (
        'GENERATE_REMEDIATION_PLAN',
        'SIMULATE_CELL_PARAMETER_CHANGE',
        'APPLY_CELL_PARAMETER_CHANGE'
    )),
    CONSTRAINT proposed_action_risk_chk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT proposed_action_policy_chk CHECK (policy_decision IN ('ALLOW', 'DENY', 'REQUIRE_APPROVAL')),
    CONSTRAINT proposed_action_status_chk CHECK (status IN (
        'PROPOSED',
        'POLICY_EVALUATED',
        'APPROVAL_REQUIRED',
        'APPROVED',
        'REJECTED',
        'DENIED',
        'EXECUTING',
        'SUCCEEDED',
        'FAILED'
    ))
);

CREATE INDEX proposed_action_case_idx ON proposed_action (assurance_case_id, proposed_at DESC);
CREATE INDEX proposed_action_status_idx ON proposed_action (status);

CREATE TABLE policy_decision (
    id UUID PRIMARY KEY,
    action_id UUID NOT NULL UNIQUE REFERENCES proposed_action (id),
    decision VARCHAR(32) NOT NULL,
    policy_id VARCHAR(64) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    evaluated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT policy_decision_chk CHECK (decision IN ('ALLOW', 'DENY', 'REQUIRE_APPROVAL'))
);

CREATE TABLE action_approval (
    id UUID PRIMARY KEY,
    action_id UUID NOT NULL UNIQUE REFERENCES proposed_action (id),
    decision VARCHAR(16) NOT NULL,
    decided_by VARCHAR(64) NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL,
    comment VARCHAR(512),
    CONSTRAINT action_approval_decision_chk CHECK (decision IN ('APPROVED', 'REJECTED'))
);

CREATE TABLE action_result (
    id UUID PRIMARY KEY,
    action_id UUID NOT NULL UNIQUE REFERENCES proposed_action (id),
    capability_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    output TEXT,
    error TEXT,
    synthetic BOOLEAN NOT NULL,
    CONSTRAINT action_result_status_chk CHECK (status IN ('SUCCEEDED', 'FAILED', 'REJECTED'))
);

CREATE TABLE action_audit_event (
    id UUID PRIMARY KEY,
    action_id UUID NOT NULL REFERENCES proposed_action (id),
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    details TEXT NOT NULL,
    CONSTRAINT action_audit_event_type_chk CHECK (event_type IN (
        'ACTION_PROPOSED',
        'POLICY_EVALUATED',
        'APPROVAL_REQUESTED',
        'ACTION_APPROVED',
        'ACTION_REJECTED',
        'ACTION_DENIED',
        'MCP_INVOCATION_STARTED',
        'MCP_INVOCATION_SUCCEEDED',
        'MCP_INVOCATION_FAILED'
    ))
);

CREATE INDEX action_audit_event_action_idx ON action_audit_event (action_id, occurred_at ASC);
