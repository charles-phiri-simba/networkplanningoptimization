-- Phase 1B structured network domain. Persistence IDs are UUIDs; domain IDs are stable strings.

CREATE TABLE site (
    id UUID PRIMARY KEY,
    site_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT site_status_chk CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE gnb (
    id UUID PRIMARY KEY,
    gnb_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    site_id UUID NOT NULL REFERENCES site (id),
    vendor VARCHAR(128),
    model VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    CONSTRAINT gnb_status_chk CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX gnb_site_id_idx ON gnb (site_id);

CREATE TABLE cell (
    id UUID PRIMARY KEY,
    cell_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    gnb_id UUID NOT NULL REFERENCES gnb (id),
    technology VARCHAR(32) NOT NULL,
    band VARCHAR(32) NOT NULL,
    arfcn INTEGER,
    pci INTEGER,
    bandwidth_mhz INTEGER,
    duplex_mode VARCHAR(16),
    status VARCHAR(32) NOT NULL,
    CONSTRAINT cell_status_chk CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX cell_gnb_id_idx ON cell (gnb_id);

CREATE TABLE radio_configuration (
    id UUID PRIMARY KEY,
    cell_id UUID NOT NULL REFERENCES cell (id),
    parameter_name VARCHAR(128) NOT NULL,
    parameter_value VARCHAR(128) NOT NULL,
    unit VARCHAR(32),
    effective_from TIMESTAMPTZ NOT NULL
);

CREATE INDEX radio_configuration_cell_id_idx ON radio_configuration (cell_id);

CREATE TABLE kpi_observation (
    id UUID PRIMARY KEY,
    cell_id UUID NOT NULL REFERENCES cell (id),
    metric VARCHAR(64) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(32),
    observed_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(64) NOT NULL,
    synthetic BOOLEAN NOT NULL
);

CREATE INDEX kpi_observation_cell_observed_idx ON kpi_observation (cell_id, observed_at DESC);

CREATE TABLE neighbour_relationship (
    id UUID PRIMARY KEY,
    source_cell_id UUID NOT NULL REFERENCES cell (id),
    target_cell_id UUID NOT NULL REFERENCES cell (id),
    relation_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT neighbour_status_chk CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT neighbour_not_self_chk CHECK (source_cell_id <> target_cell_id),
    CONSTRAINT neighbour_unique UNIQUE (source_cell_id, target_cell_id)
);

CREATE INDEX neighbour_source_idx ON neighbour_relationship (source_cell_id);
CREATE INDEX neighbour_target_idx ON neighbour_relationship (target_cell_id);
