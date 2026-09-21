-- Phase 18: production change campaigns, progressive delivery, operational safety.
-- V1--V18 remain unchanged. Historical production changes remain STANDALONE.
-- No secret columns. No vendor command payloads. No production endpoints.

ALTER TABLE production_network_change
    ADD COLUMN execution_origin VARCHAR(32),
    ADD COLUMN campaign_handoff_id VARCHAR(64);

UPDATE production_network_change
   SET execution_origin = 'STANDALONE'
 WHERE execution_origin IS NULL;

ALTER TABLE production_network_change
    ALTER COLUMN execution_origin SET DEFAULT 'STANDALONE',
    ALTER COLUMN execution_origin SET NOT NULL;

ALTER TABLE production_network_change
    ADD CONSTRAINT production_network_change_origin_chk
        CHECK (execution_origin IN ('STANDALONE', 'PRODUCTION_CAMPAIGN')),
    ADD CONSTRAINT production_network_change_origin_handoff_chk
        CHECK (
            (execution_origin = 'STANDALONE' AND campaign_handoff_id IS NULL)
            OR (execution_origin = 'PRODUCTION_CAMPAIGN' AND campaign_handoff_id IS NOT NULL
                AND campaign_handoff_id ~ '^[0-9a-f]{64}$')
        );

CREATE UNIQUE INDEX production_network_change_campaign_handoff_uidx
    ON production_network_change (campaign_handoff_id)
 WHERE campaign_handoff_id IS NOT NULL;

CREATE TABLE production_change_campaign (
    campaign_id UUID PRIMARY KEY,
    production_target_id VARCHAR(128) NOT NULL REFERENCES production_network_target (target_id),
    vendor VARCHAR(32) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    environment VARCHAR(32) NOT NULL,
    network_domain VARCHAR(64),
    objective VARCHAR(1024) NOT NULL,
    change_control_reference VARCHAR(256) NOT NULL,
    state VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    creator_principal_id VARCHAR(128) NOT NULL,
    authorizer_principal_id VARCHAR(128),
    current_revision_number INTEGER NOT NULL DEFAULT 1,
    fingerprint VARCHAR(64),
    abort_state VARCHAR(32) NOT NULL DEFAULT 'NOT_ABORTED',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX production_change_campaign_target_idx ON production_change_campaign (production_target_id);
CREATE INDEX production_change_campaign_state_idx ON production_change_campaign (state);

CREATE TABLE campaign_revision (
    revision_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_number INTEGER NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    authorization_generation INTEGER NOT NULL DEFAULT 0,
    state VARCHAR(64) NOT NULL,
    policy_versions VARCHAR(1024) NOT NULL DEFAULT '{}',
    windows VARCHAR(1024),
    external_ticket_currentness_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (campaign_id, revision_number)
);

CREATE TABLE campaign_cohort (
    cohort_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    cohort_sequence INTEGER NOT NULL,
    cohort_type VARCHAR(16) NOT NULL,
    state VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (revision_id, cohort_sequence),
    CONSTRAINT campaign_cohort_type_chk CHECK (cohort_type IN ('CANARY', 'STANDARD'))
);

CREATE TABLE campaign_execution_item (
    item_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    cohort_id UUID NOT NULL REFERENCES campaign_cohort (cohort_id),
    item_sequence INTEGER NOT NULL,
    phase14_plan_id UUID,
    phase14_plan_fingerprint VARCHAR(64),
    phase15_execution_id UUID,
    phase15_execution_fingerprint VARCHAR(64),
    production_change_id UUID,
    production_fingerprint VARCHAR(64),
    production_target_id VARCHAR(128) NOT NULL,
    object_type VARCHAR(16) NOT NULL DEFAULT 'CELL',
    cell_id VARCHAR(128) NOT NULL,
    parameter VARCHAR(64) NOT NULL,
    expected_value NUMERIC NOT NULL,
    desired_value NUMERIC NOT NULL,
    rollback_value NUMERIC NOT NULL,
    unit VARCHAR(16) NOT NULL,
    item_fingerprint VARCHAR(64) NOT NULL,
    state VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (revision_id, item_sequence),
    UNIQUE (revision_id, production_target_id, cell_id, parameter),
    CONSTRAINT campaign_item_object_chk CHECK (object_type = 'CELL'),
    CONSTRAINT campaign_item_parameter_chk CHECK (parameter = 'txPower'),
    CONSTRAINT campaign_item_unit_chk CHECK (unit = 'dBm')
);

CREATE TABLE campaign_lease (
    campaign_id UUID PRIMARY KEY REFERENCES production_change_campaign (campaign_id),
    holder_id VARCHAR(128),
    fencing_token BIGINT NOT NULL DEFAULT 0,
    acquired_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    status VARCHAR(16) NOT NULL DEFAULT 'NONE',
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE campaign_control_generation (
    campaign_id UUID PRIMARY KEY REFERENCES production_change_campaign (campaign_id),
    current_generation BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE campaign_release_generation (
    campaign_id UUID PRIMARY KEY REFERENCES production_change_campaign (campaign_id),
    current_generation BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE campaign_cohort_release (
    release_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    cohort_id UUID NOT NULL REFERENCES campaign_cohort (cohort_id),
    release_generation BIGINT NOT NULL,
    control_generation BIGINT NOT NULL,
    campaign_fence BIGINT NOT NULL,
    release_fingerprint VARCHAR(64) NOT NULL,
    state VARCHAR(16) NOT NULL,
    releaser_principal_id VARCHAR(128) NOT NULL,
    released_at TIMESTAMPTZ NOT NULL,
    UNIQUE (campaign_id, release_generation)
);

CREATE TABLE campaign_forward_progression_closure (
    campaign_id UUID PRIMARY KEY REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    state VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT campaign_closure_state_chk CHECK (state IN ('OPEN', 'CLOSED'))
);

CREATE TABLE campaign_mutation_slot (
    campaign_id UUID PRIMARY KEY REFERENCES production_change_campaign (campaign_id),
    holder_type VARCHAR(16) NOT NULL DEFAULT 'NONE',
    holder_item_id UUID,
    reservation_id UUID,
    campaign_fencing_token BIGINT,
    campaign_control_generation BIGINT,
    state VARCHAR(32) NOT NULL DEFAULT 'EMPTY',
    acquired_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT campaign_slot_state_chk CHECK (state IN ('EMPTY', 'ACQUIRED', 'HELD_MAY_HAVE_SENT')),
    CONSTRAINT campaign_slot_holder_chk CHECK (holder_type IN ('NONE', 'FORWARD', 'RECOVERY'))
);

CREATE TABLE campaign_safety_exposure (
    campaign_id UUID PRIMARY KEY REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    forward_mutation_exposure INTEGER NOT NULL DEFAULT 0,
    recovery_mutation_exposure INTEGER NOT NULL DEFAULT 0,
    distinct_cells_exposed INTEGER NOT NULL DEFAULT 0,
    unresolved_forward_exposure INTEGER NOT NULL DEFAULT 0,
    unresolved_recovery_exposure INTEGER NOT NULL DEFAULT 0,
    verification_failure_count INTEGER NOT NULL DEFAULT 0,
    consecutive_failure_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE campaign_safety_budget_reservation (
    reservation_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    item_id UUID REFERENCES campaign_execution_item (item_id),
    budget_type VARCHAR(16) NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT campaign_reservation_budget_chk CHECK (budget_type IN ('FORWARD', 'RECOVERY')),
    CONSTRAINT campaign_reservation_state_chk CHECK (state IN ('RESERVED', 'CONSUMED', 'RELEASED', 'EXPIRED', 'STALE'))
);

CREATE UNIQUE INDEX campaign_active_forward_lineage_uidx
    ON campaign_safety_budget_reservation (item_id)
 WHERE budget_type = 'FORWARD' AND state IN ('RESERVED', 'CONSUMED');

CREATE TABLE campaign_exposed_cell (
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_number INTEGER NOT NULL,
    cell_id VARCHAR(128) NOT NULL,
    first_accounted_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (campaign_id, revision_number, cell_id)
);

CREATE TABLE campaign_execution_binding (
    binding_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    revision_number INTEGER NOT NULL,
    campaign_fingerprint VARCHAR(64) NOT NULL,
    control_generation BIGINT NOT NULL,
    cohort_id UUID NOT NULL REFERENCES campaign_cohort (cohort_id),
    release_id UUID REFERENCES campaign_cohort_release (release_id),
    release_fingerprint VARCHAR(64) NOT NULL,
    release_generation BIGINT NOT NULL,
    item_id UUID NOT NULL REFERENCES campaign_execution_item (item_id),
    item_sequence INTEGER NOT NULL,
    item_fingerprint VARCHAR(64) NOT NULL,
    campaign_fence BIGINT NOT NULL,
    production_target_id VARCHAR(128) NOT NULL,
    phase14_plan_id UUID,
    phase14_plan_fingerprint VARCHAR(64),
    phase15_execution_id UUID,
    phase15_execution_fingerprint VARCHAR(64),
    production_change_id UUID,
    production_fingerprint VARCHAR(64),
    cell_id VARCHAR(128) NOT NULL,
    parameter VARCHAR(64) NOT NULL,
    expected_value NUMERIC NOT NULL,
    desired_value NUMERIC NOT NULL,
    rollback_value NUMERIC NOT NULL,
    unit VARCHAR(16) NOT NULL,
    binding_digest VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (item_id),
    UNIQUE (production_change_id)
);

CREATE TABLE campaign_execution_handoff (
    campaign_handoff_id VARCHAR(64) PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    item_id UUID NOT NULL REFERENCES campaign_execution_item (item_id),
    binding_id UUID NOT NULL REFERENCES campaign_execution_binding (binding_id),
    binding_digest VARCHAR(64) NOT NULL,
    production_change_id UUID,
    lineage_type VARCHAR(16) NOT NULL DEFAULT 'FORWARD',
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (item_id, lineage_type)
);

CREATE TABLE campaign_observation_boundary (
    boundary_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    cohort_id UUID NOT NULL REFERENCES campaign_cohort (cohort_id),
    item_id UUID NOT NULL REFERENCES campaign_execution_item (item_id),
    production_change_id UUID,
    cell_id VARCHAR(128) NOT NULL,
    parameter VARCHAR(64) NOT NULL,
    verified_value NUMERIC,
    vendor_verified_at TIMESTAMPTZ,
    observation_start TIMESTAMPTZ NOT NULL,
    observation_end TIMESTAMPTZ,
    source VARCHAR(128) NOT NULL,
    minimum_measurement_time TIMESTAMPTZ,
    watermark_type VARCHAR(64),
    watermark_value VARCHAR(256),
    max_ingestion_lag_ms BIGINT,
    required_canonical_checkpoint VARCHAR(128),
    policy_versions VARCHAR(1024),
    network_observation_health VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    operational_safety_health VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    boundary_digest VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE campaign_recovery (
    campaign_id UUID PRIMARY KEY REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    state VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED',
    requester_principal_id VARCHAR(128),
    reviewer_principal_id VARCHAR(128),
    authorizer_principal_id VARCHAR(128),
    requested_at TIMESTAMPTZ,
    authorized_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE campaign_suspension (
    suspension_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    suspension_type VARCHAR(32) NOT NULL,
    state VARCHAR(16) NOT NULL,
    reason_code VARCHAR(128) NOT NULL,
    pre_state VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT campaign_suspension_type_chk CHECK (suspension_type IN ('OPERATOR_PAUSE', 'SAFETY_SUSPENSION'))
);

CREATE TABLE campaign_resumption (
    resumption_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    state VARCHAR(16) NOT NULL,
    requester_principal_id VARCHAR(128),
    reviewer_principal_id VARCHAR(128),
    authorizer_principal_id VARCHAR(128),
    destination_state VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE campaign_governance_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    command VARCHAR(64) NOT NULL,
    request_digest VARCHAR(64) NOT NULL,
    response_body TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX campaign_governance_idempotency_cmd_uidx
    ON campaign_governance_idempotency (campaign_id, command, request_digest);

CREATE TABLE campaign_audit_event (
    event_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    sequence INTEGER NOT NULL,
    previous_event_hash VARCHAR(64) NOT NULL,
    event_hash VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_principal_id VARCHAR(128) NOT NULL,
    event_at TIMESTAMPTZ NOT NULL,
    payload_digest VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    UNIQUE (campaign_id, sequence)
);

CREATE OR REPLACE FUNCTION campaign_forbid_origin_mutation() RETURNS trigger AS $$
BEGIN
    IF NEW.execution_origin IS DISTINCT FROM OLD.execution_origin THEN
        RAISE EXCEPTION 'execution_origin is immutable';
    END IF;
    IF NEW.campaign_handoff_id IS DISTINCT FROM OLD.campaign_handoff_id
       AND OLD.campaign_handoff_id IS NOT NULL THEN
        RAISE EXCEPTION 'campaign_handoff_id is immutable once written';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER production_network_change_origin_immutable
    BEFORE UPDATE ON production_network_change
    FOR EACH ROW EXECUTE FUNCTION campaign_forbid_origin_mutation();

CREATE OR REPLACE FUNCTION campaign_forbid_closure_reopen() RETURNS trigger AS $$
BEGIN
    IF OLD.state = 'CLOSED' AND NEW.state IS DISTINCT FROM 'CLOSED' THEN
        RAISE EXCEPTION 'ForwardProgressionClosure cannot reopen';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER campaign_closure_no_reopen
    BEFORE UPDATE ON campaign_forward_progression_closure
    FOR EACH ROW EXECUTE FUNCTION campaign_forbid_closure_reopen();

CREATE TABLE campaign_external_interference (
    interference_id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES production_change_campaign (campaign_id),
    revision_id UUID NOT NULL REFERENCES campaign_revision (revision_id),
    item_id UUID REFERENCES campaign_execution_item (item_id),
    cell_id VARCHAR(128) NOT NULL,
    parameter VARCHAR(64) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    evidence_digest VARCHAR(64) NOT NULL
);

-- SHA-256 lowercase hex format constraints (Sha256Hex). Spec §4.2.
ALTER TABLE production_change_campaign
    ADD CONSTRAINT production_change_campaign_fp_sha256_chk
        CHECK (fingerprint IS NULL OR fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE campaign_revision
    ADD CONSTRAINT campaign_revision_fp_sha256_chk
        CHECK (fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE campaign_execution_item
    ADD CONSTRAINT campaign_item_fp_sha256_chk
        CHECK (item_fingerprint ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT campaign_item_p14_fp_sha256_chk
        CHECK (phase14_plan_fingerprint IS NULL OR phase14_plan_fingerprint ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT campaign_item_p15_fp_sha256_chk
        CHECK (phase15_execution_fingerprint IS NULL OR phase15_execution_fingerprint ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT campaign_item_p16_fp_sha256_chk
        CHECK (production_fingerprint IS NULL OR production_fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE campaign_cohort_release
    ADD CONSTRAINT campaign_release_fp_sha256_chk
        CHECK (release_fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE campaign_execution_binding
    ADD CONSTRAINT campaign_binding_digest_sha256_chk
        CHECK (binding_digest ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT campaign_binding_campaign_fp_sha256_chk
        CHECK (campaign_fingerprint ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT campaign_binding_item_fp_sha256_chk
        CHECK (item_fingerprint ~ '^[0-9a-f]{64}$');

ALTER TABLE campaign_execution_handoff
    ADD CONSTRAINT campaign_handoff_id_sha256_chk
        CHECK (campaign_handoff_id ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT campaign_handoff_digest_sha256_chk
        CHECK (binding_digest ~ '^[0-9a-f]{64}$');

ALTER TABLE campaign_observation_boundary
    ADD CONSTRAINT campaign_observation_digest_sha256_chk
        CHECK (boundary_digest ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT campaign_observation_start_after_vendor_chk
        CHECK (vendor_verified_at IS NULL OR observation_start >= vendor_verified_at);

ALTER TABLE campaign_audit_event
    ADD CONSTRAINT campaign_audit_event_hash_sha256_chk
        CHECK (event_hash ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT campaign_audit_previous_hash_sha256_chk
        CHECK (previous_event_hash ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT campaign_audit_payload_digest_sha256_chk
        CHECK (payload_digest ~ '^[0-9a-f]{64}$');

ALTER TABLE campaign_external_interference
    ADD CONSTRAINT campaign_interference_digest_sha256_chk
        CHECK (evidence_digest ~ '^[0-9a-f]{64}$');

ALTER TABLE campaign_governance_idempotency
    ADD CONSTRAINT campaign_idempotency_digest_sha256_chk
        CHECK (request_digest ~ '^[0-9a-f]{64}$');

ALTER TABLE campaign_lease
    ADD CONSTRAINT campaign_lease_status_chk
        CHECK (status IN ('NONE', 'HELD', 'ACTIVE', 'EXPIRED'));

CREATE INDEX campaign_execution_item_campaign_state_idx
    ON campaign_execution_item (campaign_id, state);

ALTER TABLE campaign_resumption
    ADD CONSTRAINT campaign_resumption_state_chk
        CHECK (state IN ('REQUESTED', 'UNDER_REVIEW', 'REVIEWED', 'AUTHORIZED', 'EFFECTIVE', 'REJECTED', 'STALE'));

CREATE UNIQUE INDEX campaign_resumption_active_uidx
    ON campaign_resumption (campaign_id)
 WHERE state IN ('REQUESTED', 'UNDER_REVIEW', 'REVIEWED', 'AUTHORIZED');

-- Released membership identity is frozen. Lifecycle state may still advance.
CREATE OR REPLACE FUNCTION campaign_forbid_released_membership_mutation() RETURNS trigger AS $$
DECLARE
    cohort_state VARCHAR(64);
    cid UUID;
BEGIN
    cid := COALESCE(NEW.cohort_id, OLD.cohort_id);
    SELECT state INTO cohort_state FROM campaign_cohort WHERE cohort_id = cid;
    IF cohort_state IS DISTINCT FROM 'RELEASED' THEN
        RETURN COALESCE(NEW, OLD);
    END IF;
    IF TG_OP = 'INSERT' OR TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'released cohort membership is immutable';
    END IF;
    IF NEW.cohort_id IS DISTINCT FROM OLD.cohort_id
       OR NEW.item_sequence IS DISTINCT FROM OLD.item_sequence
       OR NEW.item_fingerprint IS DISTINCT FROM OLD.item_fingerprint
       OR NEW.cell_id IS DISTINCT FROM OLD.cell_id
       OR NEW.parameter IS DISTINCT FROM OLD.parameter
       OR NEW.expected_value IS DISTINCT FROM OLD.expected_value
       OR NEW.desired_value IS DISTINCT FROM OLD.desired_value
       OR NEW.rollback_value IS DISTINCT FROM OLD.rollback_value
       OR NEW.unit IS DISTINCT FROM OLD.unit
       OR NEW.object_type IS DISTINCT FROM OLD.object_type
       OR NEW.phase14_plan_id IS DISTINCT FROM OLD.phase14_plan_id
       OR NEW.phase15_execution_id IS DISTINCT FROM OLD.phase15_execution_id THEN
        RAISE EXCEPTION 'released cohort membership is immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER campaign_item_released_membership_immutable
    BEFORE INSERT OR UPDATE OR DELETE ON campaign_execution_item
    FOR EACH ROW EXECUTE FUNCTION campaign_forbid_released_membership_mutation();

