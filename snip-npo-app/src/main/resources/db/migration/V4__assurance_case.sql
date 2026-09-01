-- Phase 3: Assurance Case + operational evidence. No incident platform.

CREATE TABLE assurance_case (
    id UUID PRIMARY KEY,
    case_type VARCHAR(64) NOT NULL,
    affected_entity_type VARCHAR(32) NOT NULL,
    affected_entity_id VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    confidence VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    first_observed_at TIMESTAMPTZ NOT NULL,
    last_observed_at TIMESTAMPTZ NOT NULL,
    rule_id VARCHAR(64) NOT NULL,
    synthetic BOOLEAN NOT NULL,
    CONSTRAINT assurance_case_type_chk CHECK (case_type IN ('DEGRADING_RADIO_QUALITY')),
    CONSTRAINT assurance_entity_type_chk CHECK (affected_entity_type IN ('CELL')),
    CONSTRAINT assurance_severity_chk CHECK (severity IN ('INFO', 'WARNING', 'MAJOR', 'CRITICAL')),
    CONSTRAINT assurance_confidence_chk CHECK (confidence IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT assurance_status_chk CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED'))
);

CREATE UNIQUE INDEX assurance_case_active_uk
    ON assurance_case (affected_entity_id, case_type)
    WHERE status IN ('OPEN', 'ACKNOWLEDGED');

CREATE INDEX assurance_case_entity_idx
    ON assurance_case (affected_entity_id, detected_at DESC);

CREATE TABLE assurance_evidence (
    id UUID PRIMARY KEY,
    assurance_case_id UUID NOT NULL REFERENCES assurance_case (id) ON DELETE CASCADE,
    evidence_type VARCHAR(64) NOT NULL,
    metric VARCHAR(64),
    value DOUBLE PRECISION,
    unit VARCHAR(32),
    trend VARCHAR(32),
    observed_at TIMESTAMPTZ,
    source VARCHAR(64) NOT NULL,
    synthetic BOOLEAN NOT NULL,
    description VARCHAR(512) NOT NULL
);

CREATE INDEX assurance_evidence_case_idx ON assurance_evidence (assurance_case_id);
