-- PI7: persistent what-if configuration scenarios. Cell-local synthetic evaluation only.
-- No mutation authority. No cascade delete of simulation evidence.
-- Fingerprints are VARCHAR(64) plus lowercase SHA-256 CHECKs (not CHAR/bpchar).

CREATE TABLE planning_scenario (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1024) NOT NULL DEFAULT '',
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX planning_scenario_updated_idx ON planning_scenario (updated_at DESC);

CREATE TABLE planning_scenario_cell (
    id UUID PRIMARY KEY,
    scenario_id UUID NOT NULL REFERENCES planning_scenario (id) ON DELETE RESTRICT,
    cell_id VARCHAR(64) NOT NULL,
    ordinal INTEGER NOT NULL,
    CONSTRAINT planning_scenario_cell_ordinal_chk CHECK (ordinal >= 1),
    CONSTRAINT planning_scenario_cell_unique UNIQUE (scenario_id, cell_id),
    CONSTRAINT planning_scenario_cell_ordinal_unique UNIQUE (scenario_id, ordinal)
);

CREATE TABLE planning_alternative (
    id UUID PRIMARY KEY,
    scenario_id UUID NOT NULL REFERENCES planning_scenario (id) ON DELETE RESTRICT,
    name VARCHAR(128) NOT NULL,
    ordinal INTEGER NOT NULL,
    CONSTRAINT planning_alternative_ordinal_chk CHECK (ordinal >= 1),
    CONSTRAINT planning_alternative_ordinal_unique UNIQUE (scenario_id, ordinal)
);

CREATE UNIQUE INDEX planning_alternative_name_uidx
    ON planning_alternative (scenario_id, lower(name));

CREATE TABLE planning_cell_intent (
    id UUID PRIMARY KEY,
    alternative_id UUID NOT NULL REFERENCES planning_alternative (id) ON DELETE RESTRICT,
    cell_id VARCHAR(64) NOT NULL,
    parameter_id VARCHAR(64) NOT NULL,
    intended_value NUMERIC(8,3) NOT NULL,
    CONSTRAINT planning_cell_intent_unique UNIQUE (alternative_id, cell_id),
    CONSTRAINT planning_cell_intent_parameter_chk CHECK (parameter_id = 'txPower'),
    CONSTRAINT planning_cell_intent_range_chk CHECK (intended_value >= 20 AND intended_value <= 50)
);

CREATE TABLE planning_evaluation (
    id UUID PRIMARY KEY,
    scenario_id UUID NOT NULL REFERENCES planning_scenario (id) ON DELETE RESTRICT,
    status VARCHAR(16) NOT NULL,
    intent_fingerprint VARCHAR(64) NOT NULL,
    admission_fingerprint VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    item_count INTEGER NOT NULL,
    succeeded_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT planning_evaluation_status_chk CHECK (status IN ('IN_PROGRESS', 'SUCCEEDED', 'PARTIAL', 'FAILED')),
    CONSTRAINT planning_evaluation_intent_fp_chk CHECK (intent_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT planning_evaluation_admission_fp_chk CHECK (
        admission_fingerprint IS NULL OR admission_fingerprint ~ '^[0-9a-f]{64}$'
    )
);

CREATE INDEX planning_evaluation_scenario_idx ON planning_evaluation (scenario_id, created_at DESC);

CREATE UNIQUE INDEX planning_evaluation_in_progress_uidx
    ON planning_evaluation (scenario_id)
    WHERE status = 'IN_PROGRESS';

CREATE TABLE planning_evaluation_item (
    id UUID PRIMARY KEY,
    evaluation_id UUID NOT NULL REFERENCES planning_evaluation (id) ON DELETE RESTRICT,
    alternative_id UUID NOT NULL REFERENCES planning_alternative (id) ON DELETE RESTRICT,
    cell_id VARCHAR(64) NOT NULL,
    parameter_id VARCHAR(64) NOT NULL,
    intended_value NUMERIC(8,3) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    failure_code VARCHAR(64),
    failure_message VARCHAR(512),
    twin_id UUID REFERENCES network_twin (id),
    twin_version INTEGER,
    pinned_baseline_tx_power NUMERIC(8,3),
    configuration_fingerprint VARCHAR(128),
    simulation_scenario_id UUID REFERENCES simulation_scenario (id) ON DELETE RESTRICT,
    simulation_run_id UUID REFERENCES simulation_run (id) ON DELETE RESTRICT,
    model_id VARCHAR(128),
    model_version VARCHAR(32),
    synthetic BOOLEAN,
    confidence VARCHAR(16),
    reused_existing_run BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT planning_evaluation_item_unique UNIQUE (evaluation_id, alternative_id, cell_id),
    CONSTRAINT planning_evaluation_item_outcome_chk CHECK (outcome IN ('SUCCEEDED', 'FAILED')),
    CONSTRAINT planning_evaluation_item_parameter_chk CHECK (parameter_id = 'txPower'),
    CONSTRAINT planning_evaluation_item_confidence_chk CHECK (confidence IS NULL OR confidence IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT planning_evaluation_item_success_chk CHECK (
        outcome <> 'SUCCEEDED'
        OR (
            simulation_run_id IS NOT NULL
            AND twin_id IS NOT NULL
            AND twin_version IS NOT NULL
            AND model_id IS NOT NULL
            AND synthetic = TRUE
        )
    )
);
