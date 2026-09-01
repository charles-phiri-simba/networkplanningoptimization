-- Phase 6: cell Digital Twin projection and immutable synthetic simulation. No live-network state.

CREATE TABLE network_twin (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    scope_type VARCHAR(16) NOT NULL,
    scope_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    latest_version INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    synchronized_at TIMESTAMPTZ,
    synthetic BOOLEAN NOT NULL,
    CONSTRAINT network_twin_scope_type_chk CHECK (scope_type IN ('CELL')),
    CONSTRAINT network_twin_status_chk CHECK (status IN ('ACTIVE')),
    CONSTRAINT network_twin_scope_unique UNIQUE (scope_type, scope_id)
);

CREATE INDEX network_twin_scope_idx ON network_twin (scope_type, scope_id);

CREATE TABLE network_twin_version (
    id UUID PRIMARY KEY,
    twin_id UUID NOT NULL REFERENCES network_twin (id),
    version INTEGER NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    synchronized_at TIMESTAMPTZ NOT NULL,
    source_event_time TIMESTAMPTZ,
    source_context_version TEXT NOT NULL,
    provenance TEXT NOT NULL,
    cell_state TEXT NOT NULL,
    configuration TEXT NOT NULL,
    current_metrics TEXT NOT NULL,
    temporal_summary TEXT NOT NULL,
    neighbour_summary TEXT NOT NULL,
    CONSTRAINT network_twin_version_unique UNIQUE (twin_id, version)
);

CREATE INDEX network_twin_version_twin_idx ON network_twin_version (twin_id, version DESC);

CREATE TABLE simulation_scenario (
    id UUID PRIMARY KEY,
    twin_id UUID NOT NULL REFERENCES network_twin (id),
    baseline_twin_version INTEGER NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1024) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    synthetic BOOLEAN NOT NULL,
    CONSTRAINT simulation_scenario_status_chk CHECK (status IN ('ACTIVE'))
);

CREATE INDEX simulation_scenario_twin_idx ON simulation_scenario (twin_id, created_at DESC);

CREATE TABLE simulation_scenario_change (
    id UUID PRIMARY KEY,
    scenario_id UUID NOT NULL UNIQUE REFERENCES simulation_scenario (id),
    parameter_id VARCHAR(64) NOT NULL,
    current_value VARCHAR(64) NOT NULL,
    proposed_value VARCHAR(64) NOT NULL,
    unit VARCHAR(32) NOT NULL
);

CREATE TABLE simulation_run (
    id UUID PRIMARY KEY,
    scenario_id UUID NOT NULL REFERENCES simulation_scenario (id),
    twin_id UUID NOT NULL REFERENCES network_twin (id),
    baseline_twin_version INTEGER NOT NULL,
    model_id VARCHAR(128) NOT NULL,
    model_version VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    synthetic BOOLEAN NOT NULL,
    confidence VARCHAR(16),
    assumptions TEXT,
    provenance TEXT,
    error TEXT,
    action_id UUID,
    CONSTRAINT simulation_run_status_chk CHECK (status IN ('STARTED', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT simulation_run_confidence_chk CHECK (confidence IS NULL OR confidence IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX simulation_run_scenario_idx ON simulation_run (scenario_id, started_at ASC);
CREATE INDEX simulation_run_twin_idx ON simulation_run (twin_id, baseline_twin_version);

CREATE TABLE simulation_result_metric (
    id UUID PRIMARY KEY,
    simulation_id UUID NOT NULL REFERENCES simulation_run (id),
    metric VARCHAR(64) NOT NULL,
    baseline_value DOUBLE PRECISION NOT NULL,
    candidate_value DOUBLE PRECISION NOT NULL,
    delta DOUBLE PRECISION NOT NULL,
    unit VARCHAR(32) NOT NULL
);

CREATE INDEX simulation_result_metric_sim_idx ON simulation_result_metric (simulation_id);

CREATE TABLE simulation_limitation (
    id UUID PRIMARY KEY,
    simulation_id UUID NOT NULL REFERENCES simulation_run (id),
    code VARCHAR(64) NOT NULL,
    CONSTRAINT simulation_limitation_code_chk CHECK (code IN (
        'NO_RF_PROPAGATION_MODEL',
        'NO_VENDOR_CALIBRATION',
        'NO_MOBILITY_MODEL',
        'NO_TRAFFIC_FORECAST',
        'SYNTHETIC_KPI_MODEL'
    ))
);

CREATE INDEX simulation_limitation_sim_idx ON simulation_limitation (simulation_id);
