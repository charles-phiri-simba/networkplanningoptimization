-- Phase 5: bounded AgentRun orchestration. Audit is append-only. No live-network state.

CREATE TABLE agent_run (
    id UUID PRIMARY KEY,
    objective VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    assurance_case_id UUID NOT NULL REFERENCES assurance_case (id),
    initiated_by VARCHAR(64) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    max_steps INTEGER NOT NULL,
    current_step INTEGER NOT NULL,
    max_agent_calls INTEGER NOT NULL,
    max_retries INTEGER NOT NULL,
    timeout_ms BIGINT NOT NULL,
    CONSTRAINT agent_run_status_chk CHECK (status IN (
        'CREATED',
        'RUNNING',
        'WAITING_FOR_HUMAN',
        'COMPLETED',
        'FAILED',
        'CANCELLED'
    ))
);

CREATE INDEX agent_run_case_idx ON agent_run (assurance_case_id, started_at DESC);
CREATE INDEX agent_run_status_idx ON agent_run (status);

CREATE TABLE agent_plan (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL UNIQUE REFERENCES agent_run (id),
    objective VARCHAR(1024) NOT NULL
);

CREATE TABLE agent_plan_step (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES agent_plan (id),
    step_number INTEGER NOT NULL,
    agent_role VARCHAR(32) NOT NULL,
    task VARCHAR(512) NOT NULL,
    required_inputs TEXT NOT NULL,
    expected_output VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    output_summary TEXT,
    CONSTRAINT agent_plan_step_role_chk CHECK (agent_role IN (
        'CHIEF_ORCHESTRATOR',
        'KNOWLEDGE',
        'CONTEXT',
        'ASSURANCE',
        'DECISION'
    )),
    CONSTRAINT agent_plan_step_status_chk CHECK (status IN (
        'PENDING',
        'RUNNING',
        'COMPLETED',
        'FAILED',
        'SKIPPED'
    )),
    CONSTRAINT agent_plan_step_unique UNIQUE (plan_id, step_number)
);

CREATE INDEX agent_plan_step_plan_idx ON agent_plan_step (plan_id, step_number);

CREATE TABLE agent_case_memory (
    id UUID PRIMARY KEY,
    assurance_case_id UUID NOT NULL REFERENCES assurance_case (id),
    run_id UUID NOT NULL UNIQUE REFERENCES agent_run (id),
    summary VARCHAR(1024) NOT NULL,
    findings TEXT NOT NULL,
    proposed_action_ids TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX agent_case_memory_case_idx ON agent_case_memory (assurance_case_id, created_at DESC);

CREATE TABLE agent_run_audit_event (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES agent_run (id),
    event_type VARCHAR(64) NOT NULL,
    agent_id VARCHAR(64),
    occurred_at TIMESTAMPTZ NOT NULL,
    summary VARCHAR(512) NOT NULL,
    CONSTRAINT agent_run_audit_event_type_chk CHECK (event_type IN (
        'RUN_STARTED',
        'PLAN_CREATED',
        'STEP_STARTED',
        'STEP_COMPLETED',
        'STEP_FAILED',
        'ACTION_PROPOSED',
        'RUN_COMPLETED',
        'RUN_FAILED',
        'RUN_CANCELLED',
        'LIMIT_REACHED'
    ))
);

CREATE INDEX agent_run_audit_event_run_idx ON agent_run_audit_event (run_id, occurred_at ASC);

ALTER TABLE proposed_action ADD COLUMN agent_run_id UUID REFERENCES agent_run (id);
ALTER TABLE proposed_action ADD COLUMN agent_id VARCHAR(64);

CREATE INDEX proposed_action_agent_run_idx ON proposed_action (agent_run_id);
