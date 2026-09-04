-- Phase 17: certified vendor write transport certification/onboarding control plane.
-- Additive. No secret columns. No production endpoints. No vendor protocol fields.
-- Existing production_network_target rows remain uncertified until explicit onboarding.


CREATE TABLE vendor_abstract_protocol_placeholder (
    placeholder_version_id UUID PRIMARY KEY,
    placeholder_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    interface_type_category VARCHAR(64) NOT NULL CHECK (interface_type_category IN ('UNRESOLVED','ABSTRACT_ALTERNATIVE')),
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED')),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (placeholder_id, version_no),
    CHECK (effective_until IS NULL OR effective_until > effective_from)
);
CREATE UNIQUE INDEX vendor_abstract_protocol_placeholder_active_uidx
    ON vendor_abstract_protocol_placeholder (placeholder_id) WHERE status = 'ACTIVE';

CREATE TABLE production_tls_profile (
    tls_profile_version_id UUID PRIMARY KEY,
    tls_profile_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    production_target_id VARCHAR(128) NULL REFERENCES production_network_target (target_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    environment VARCHAR(32) NOT NULL DEFAULT 'LAB' CHECK (environment IN ('LAB','PREPROD','PROD')),
    hostname_verification_required BOOLEAN NOT NULL DEFAULT TRUE,
    trust_store_profile_ref VARCHAR(256) NOT NULL,
    minimum_tls_policy VARCHAR(32) NOT NULL CHECK (minimum_tls_policy IN ('TLS_1_2','TLS_1_3')),
    cipher_policy_ref VARCHAR(128) NULL,
    mtls_required BOOLEAN NOT NULL DEFAULT FALSE,
    client_certificate_profile_ref VARCHAR(256) NULL,
    server_identity_expectation VARCHAR(255) NOT NULL,
    rotation_policy VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (tls_profile_id, version_no),
    CHECK (effective_until IS NULL OR effective_until > effective_from),
    CHECK (status = 'DRAFT' OR hostname_verification_required = TRUE),
    CHECK (NOT (status = 'ACTIVE' AND environment IN ('PREPROD','PROD') AND production_target_id IS NULL))
);
CREATE UNIQUE INDEX production_tls_profile_active_uidx
    ON production_tls_profile (tls_profile_id) WHERE status = 'ACTIVE';

CREATE TABLE production_network_policy_profile (
    network_policy_profile_version_id UUID PRIMARY KEY,
    network_policy_profile_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    gateway_workload_id VARCHAR(128) NOT NULL,
    production_target_id VARCHAR(128) NULL REFERENCES production_network_target (target_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    environment VARCHAR(32) NOT NULL DEFAULT 'LAB' CHECK (environment IN ('LAB','PREPROD','PROD')),
    destination_identity VARCHAR(255) NOT NULL,
    destination_port INTEGER NOT NULL CHECK (destination_port BETWEEN 1 AND 65535),
    dns_requirement VARCHAR(128) NOT NULL,
    private_route_class VARCHAR(128) NOT NULL,
    allowed_egress_scope VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED')),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (network_policy_profile_id, version_no),
    CHECK (effective_until IS NULL OR effective_until > effective_from),
    CHECK (NOT (status = 'ACTIVE' AND environment IN ('PREPROD','PROD') AND allowed_egress_scope = '0.0.0.0/0')),
    CHECK (NOT (status = 'ACTIVE' AND environment IN ('PREPROD','PROD') AND production_target_id IS NULL))
);
CREATE UNIQUE INDEX production_network_policy_profile_active_uidx
    ON production_network_policy_profile (network_policy_profile_id) WHERE status = 'ACTIVE';

CREATE TABLE production_credential_profile (
    credential_profile_version_id UUID PRIMARY KEY,
    credential_profile_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    production_target_id VARCHAR(128) NULL REFERENCES production_network_target (target_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    vendor VARCHAR(32) NOT NULL CHECK (vendor IN ('ERICSSON')),
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('ENM')),
    secret_reference VARCHAR(256) NOT NULL,
    workload_identity_profile VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (credential_profile_id, version_no),
    CHECK (effective_until IS NULL OR effective_until > effective_from),
    CHECK (status = 'DRAFT' OR production_target_id IS NOT NULL)
);
CREATE UNIQUE INDEX production_credential_profile_active_uidx
    ON production_credential_profile (credential_profile_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX production_credential_profile_active_target_uidx
    ON production_credential_profile (production_target_id) WHERE status = 'ACTIVE' AND production_target_id IS NOT NULL;

CREATE TABLE production_endpoint_profile (
    endpoint_profile_version_id UUID PRIMARY KEY,
    endpoint_profile_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    production_target_id VARCHAR(128) NULL REFERENCES production_network_target (target_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    environment VARCHAR(32) NOT NULL CHECK (environment IN ('LAB','PREPROD','PROD')),
    network_domain VARCHAR(64) NOT NULL,
    approved_fqdn VARCHAR(255) NOT NULL,
    approved_port INTEGER NOT NULL CHECK (approved_port BETWEEN 1 AND 65535),
    tls_server_identity VARCHAR(255) NOT NULL,
    route_zone_id VARCHAR(128) NOT NULL,
    vendor VARCHAR(32) NOT NULL CHECK (vendor IN ('ERICSSON')),
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('ENM')),
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED')),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (endpoint_profile_id, version_no),
    CHECK (effective_until IS NULL OR effective_until > effective_from),
    CHECK (NOT (status = 'ACTIVE' AND environment IN ('PREPROD','PROD') AND production_target_id IS NULL))
);
CREATE UNIQUE INDEX production_endpoint_profile_active_target_uidx
    ON production_endpoint_profile (production_target_id) WHERE status = 'ACTIVE' AND production_target_id IS NOT NULL;

CREATE TABLE vendor_security_certification (
    security_cert_version_id UUID PRIMARY KEY,
    security_cert_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    tls_profile_version_id UUID NOT NULL REFERENCES production_tls_profile (tls_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    network_policy_profile_version_id UUID NOT NULL REFERENCES production_network_policy_profile (network_policy_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    credential_profile_version_id UUID NOT NULL REFERENCES production_credential_profile (credential_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    mtls_required BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')),
    certified_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (security_cert_id, version_no)
);
CREATE UNIQUE INDEX vendor_security_certification_active_uidx
    ON vendor_security_certification (security_cert_id) WHERE status = 'ACTIVE';

CREATE TABLE vendor_interface_definition (
    interface_definition_version_id UUID PRIMARY KEY,
    interface_definition_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    vendor VARCHAR(32) NOT NULL CHECK (vendor IN ('ERICSSON')),
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('ENM')),
    vendor_product_version_predicate VARCHAR(512) NOT NULL,
    interface_type_category VARCHAR(64) NOT NULL CHECK (interface_type_category IN ('UNRESOLVED','ABSTRACT_ALTERNATIVE')),
    documentation_reference VARCHAR(512) NOT NULL,
    documentation_version VARCHAR(128) NOT NULL,
    documentation_status VARCHAR(32) NOT NULL CHECK (documentation_status IN ('ACTIVE','WITHDRAWN','SUPERSEDED')),
    abstract_protocol_placeholder_version_id UUID NULL REFERENCES vendor_abstract_protocol_placeholder (placeholder_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    security_cert_version_id UUID NULL REFERENCES vendor_security_certification (security_cert_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    capability_cert_version_id UUID NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','INTERFACE_VERIFIED','SUPERSEDED','REVOKED','EXPIRED')),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    supersedes_version_id UUID NULL REFERENCES vendor_interface_definition (interface_definition_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (interface_definition_id, version_no),
    UNIQUE (interface_definition_id, content_digest),
    CHECK (effective_until IS NULL OR effective_until > effective_from)
);
CREATE INDEX vendor_interface_definition_status_idx ON vendor_interface_definition (interface_definition_id, status);
CREATE INDEX vendor_interface_definition_doc_idx ON vendor_interface_definition (documentation_status);

CREATE TABLE vendor_interface_approval (
    approval_id UUID PRIMARY KEY,
    interface_definition_version_id UUID NOT NULL REFERENCES vendor_interface_definition (interface_definition_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    approver_principal_id VARCHAR(128) NOT NULL,
    approval_status VARCHAR(32) NOT NULL CHECK (approval_status IN ('APPROVED','REVOKED','WITHDRAWN')),
    approved_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    revoked_by VARCHAR(128) NULL,
    reason_code VARCHAR(128) NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$')
);
CREATE UNIQUE INDEX vendor_interface_approval_approved_uidx
    ON vendor_interface_approval (interface_definition_version_id) WHERE approval_status = 'APPROVED';

CREATE TABLE vendor_write_transport_profile (
    transport_profile_version_id UUID PRIMARY KEY,
    transport_profile_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    vendor VARCHAR(32) NOT NULL CHECK (vendor IN ('ERICSSON')),
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('ENM')),
    interface_definition_version_id UUID NOT NULL REFERENCES vendor_interface_definition (interface_definition_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    vendor_version_predicate VARCHAR(512) NOT NULL,
    transport_implementation_version VARCHAR(64) NOT NULL,
    artifact_digest CHAR(64) NOT NULL CHECK (artifact_digest ~ '^[0-9a-f]{64}$'),
    security_cert_version_id UUID NOT NULL REFERENCES vendor_security_certification (security_cert_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    credential_profile_version_id UUID NOT NULL REFERENCES production_credential_profile (credential_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    capability_cert_version_id UUID NULL,
    tls_profile_version_id UUID NOT NULL REFERENCES production_tls_profile (tls_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    network_policy_profile_version_id UUID NOT NULL REFERENCES production_network_policy_profile (network_policy_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    expected_state_strategy VARCHAR(32) NOT NULL CHECK (expected_state_strategy IN ('READ_THEN_WRITE','ATOMIC')),
    atomic_certified BOOLEAN NOT NULL DEFAULT FALSE,
    mutation_strategy VARCHAR(64) NOT NULL,
    readback_strategy VARCHAR(64) NOT NULL,
    rollback_strategy VARCHAR(64) NOT NULL,
    timeout_policy VARCHAR(256) NOT NULL,
    retry_policy VARCHAR(256) NOT NULL,
    supported_object_types VARCHAR(64) NOT NULL CHECK (supported_object_types = 'CELL'),
    supported_parameters VARCHAR(64) NOT NULL CHECK (supported_parameters = 'txPower'),
    certification_state VARCHAR(32) NOT NULL CHECK (certification_state IN ('DRAFT','INTERFACE_VERIFIED','LAB_CERTIFICATION_PENDING','LAB_CERTIFIED','PREPROD_CERTIFICATION_PENDING','PREPROD_CERTIFIED','PRODUCTION_REGISTRATION_PENDING','PRODUCTION_REGISTERED','SUSPENDED','EXPIRED','REVOKED')),
    certification_expiry TIMESTAMPTZ NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (transport_profile_id, version_no),
    CHECK (effective_until IS NULL OR effective_until > effective_from),
    CHECK (expected_state_strategy <> 'ATOMIC' OR atomic_certified = TRUE),
    CHECK (certification_state = 'DRAFT' OR capability_cert_version_id IS NOT NULL)
);
CREATE UNIQUE INDEX vendor_write_transport_profile_active_uidx
    ON vendor_write_transport_profile (transport_profile_id) WHERE status = 'ACTIVE';

CREATE TABLE vendor_capability_certification (
    capability_cert_version_id UUID PRIMARY KEY,
    capability_cert_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    object_type VARCHAR(32) NOT NULL CHECK (object_type = 'CELL'),
    parameter VARCHAR(32) NOT NULL CHECK (parameter = 'txPower'),
    addressing_semantics VARCHAR(256) NOT NULL,
    parameter_type VARCHAR(64) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    valid_range VARCHAR(128) NOT NULL,
    precision VARCHAR(64) NOT NULL,
    read_semantics VARCHAR(256) NOT NULL,
    write_semantics VARCHAR(256) NOT NULL,
    rollback_semantics VARCHAR(256) NOT NULL,
    verification_semantics VARCHAR(256) NOT NULL,
    propagation_delay VARCHAR(64) NOT NULL,
    eventual_consistency VARCHAR(128) NOT NULL,
    conditional_write_supported BOOLEAN NOT NULL DEFAULT FALSE,
    vendor VARCHAR(32) NOT NULL CHECK (vendor IN ('ERICSSON')),
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('ENM')),
    version_predicate VARCHAR(512) NOT NULL,
    transport_profile_version_id UUID NOT NULL REFERENCES vendor_write_transport_profile (transport_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')),
    certified_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (capability_cert_id, version_no)
);
CREATE UNIQUE INDEX vendor_capability_certification_active_uidx
    ON vendor_capability_certification (capability_cert_id) WHERE status = 'ACTIVE';

ALTER TABLE vendor_write_transport_profile
    ADD CONSTRAINT vendor_write_transport_profile_capability_fk
    FOREIGN KEY (capability_cert_version_id) REFERENCES vendor_capability_certification (capability_cert_version_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE vendor_interface_definition
    ADD CONSTRAINT vendor_interface_definition_capability_fk
    FOREIGN KEY (capability_cert_version_id) REFERENCES vendor_capability_certification (capability_cert_version_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE vendor_transport_artifact (
    artifact_id UUID PRIMARY KEY,
    artifact_digest CHAR(64) NOT NULL UNIQUE CHECK (artifact_digest ~ '^[0-9a-f]{64}$'),
    transport_implementation_version VARCHAR(64) NOT NULL,
    source_baseline_sha CHAR(40) NOT NULL CHECK (source_baseline_sha ~ '^[0-9a-f]{40}$'),
    certification_bundle_version VARCHAR(64) NULL,
    container_image_digest VARCHAR(128) NULL,
    jar_digest CHAR(64) NULL,
    sbom_reference VARCHAR(256) NULL,
    ci_run_id VARCHAR(64) NULL,
    build_provenance_reference VARCHAR(256) NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('REGISTERED','CERTIFIED','SUPERSEDED','REVOKED')),
    CHECK (jar_digest IS NULL OR jar_digest ~ '^[0-9a-f]{64}$')
);

CREATE TABLE transport_certification (
    transport_certification_id UUID PRIMARY KEY,
    current_version_id UUID NULL,
    vendor VARCHAR(32) NOT NULL CHECK (vendor IN ('ERICSSON')),
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('ENM')),
    transport_profile_version_id UUID NOT NULL REFERENCES vendor_write_transport_profile (transport_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    state VARCHAR(32) NOT NULL CHECK (state IN ('DRAFT','INTERFACE_VERIFIED','LAB_CERTIFICATION_PENDING','LAB_CERTIFIED','PREPROD_CERTIFICATION_PENDING','PREPROD_CERTIFIED','PRODUCTION_REGISTRATION_PENDING','PRODUCTION_REGISTERED','SUSPENDED','EXPIRED','REVOKED')),
    environment_class VARCHAR(32) NOT NULL CHECK (environment_class IN ('LOCAL','LAB','PREPROD','PROD')),
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE transport_certification_version (
    transport_certification_version_id UUID PRIMARY KEY,
    transport_certification_id UUID NOT NULL REFERENCES transport_certification (transport_certification_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    state VARCHAR(32) NOT NULL CHECK (state IN ('DRAFT','INTERFACE_VERIFIED','LAB_CERTIFICATION_PENDING','LAB_CERTIFIED','PREPROD_CERTIFICATION_PENDING','PREPROD_CERTIFIED','PRODUCTION_REGISTRATION_PENDING','PRODUCTION_REGISTERED','SUSPENDED','EXPIRED','REVOKED')),
    interface_definition_version_id UUID NOT NULL REFERENCES vendor_interface_definition (interface_definition_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    bundle_version_id UUID NULL,
    artifact_digest CHAR(64) NOT NULL CHECK (artifact_digest ~ '^[0-9a-f]{64}$'),
    source_baseline_sha CHAR(40) NOT NULL CHECK (source_baseline_sha ~ '^[0-9a-f]{40}$'),
    actor_principal_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (transport_certification_id, version_no)
);

ALTER TABLE transport_certification
    ADD CONSTRAINT transport_certification_current_version_fk
    FOREIGN KEY (current_version_id) REFERENCES transport_certification_version (transport_certification_version_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE transport_certification_evidence (
    evidence_id UUID PRIMARY KEY,
    evidence_version INTEGER NOT NULL,
    certification_subject_type VARCHAR(64) NOT NULL,
    certification_subject_id UUID NOT NULL,
    certification_subject_version_id UUID NOT NULL,
    evidence_type VARCHAR(64) NOT NULL,
    environment_level VARCHAR(8) NOT NULL CHECK (environment_level IN ('L0','L1','L2','L3')),
    issuer_principal_id VARCHAR(128) NOT NULL,
    certifier_permission VARCHAR(64) NOT NULL,
    result VARCHAR(16) NOT NULL CHECK (result IN ('PASS','FAIL','NOT_EXECUTED','NOT_SATISFIED')),
    reference VARCHAR(512) NOT NULL,
    evidence_hash CHAR(64) NOT NULL CHECK (evidence_hash ~ '^[0-9a-f]{64}$'),
    artifact_binding VARCHAR(128) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    superseded_by UUID NULL REFERENCES transport_certification_evidence (evidence_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    status VARCHAR(32) NOT NULL CHECK (status IN ('ACTIVE','SUPERSEDED','WITHDRAWN','REVOKED')),
    UNIQUE (certification_subject_version_id, evidence_type, evidence_version),
    CHECK (result <> 'PASS' OR (issuer_principal_id IS NOT NULL AND certifier_permission IS NOT NULL AND issuer_principal_id <> ''))
);
CREATE UNIQUE INDEX transport_certification_evidence_active_pass_uidx
    ON transport_certification_evidence (certification_subject_version_id, evidence_type)
    WHERE status = 'ACTIVE' AND result = 'PASS';

CREATE TABLE transport_certification_bundle (
    bundle_version_id UUID PRIMARY KEY,
    bundle_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    vendor VARCHAR(32) NOT NULL CHECK (vendor IN ('ERICSSON')),
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('ENM')),
    interface_definition_version_id UUID NOT NULL REFERENCES vendor_interface_definition (interface_definition_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    interface_approval_id UUID NOT NULL REFERENCES vendor_interface_approval (approval_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    transport_profile_version_id UUID NOT NULL REFERENCES vendor_write_transport_profile (transport_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    artifact_digest CHAR(64) NOT NULL CHECK (artifact_digest ~ '^[0-9a-f]{64}$'),
    transport_implementation_version VARCHAR(64) NOT NULL,
    source_baseline_sha CHAR(40) NOT NULL CHECK (source_baseline_sha ~ '^[0-9a-f]{40}$'),
    vendor_version_predicate VARCHAR(512) NOT NULL,
    capability_cert_version_id UUID NOT NULL REFERENCES vendor_capability_certification (capability_cert_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    security_cert_version_id UUID NOT NULL REFERENCES vendor_security_certification (security_cert_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    credential_profile_version_id UUID NOT NULL REFERENCES production_credential_profile (credential_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    tls_profile_version_id UUID NOT NULL REFERENCES production_tls_profile (tls_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    network_policy_profile_version_id UUID NOT NULL REFERENCES production_network_policy_profile (network_policy_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    endpoint_profile_version_id UUID NULL REFERENCES production_endpoint_profile (endpoint_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    target_class VARCHAR(32) NOT NULL CHECK (target_class IN ('LAB','PREPROD','PROD')),
    active_evidence_set_digest CHAR(64) NOT NULL CHECK (active_evidence_set_digest ~ '^[0-9a-f]{64}$'),
    certifier_principal_id VARCHAR(128) NOT NULL,
    certified_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('ACTIVE','INVALID','EXPIRED','REVOKED')),
    UNIQUE (bundle_id, version_no),
    CHECK (target_class = 'LAB' OR endpoint_profile_version_id IS NOT NULL)
);
CREATE UNIQUE INDEX transport_certification_bundle_active_uidx
    ON transport_certification_bundle (bundle_id) WHERE status = 'ACTIVE';

ALTER TABLE transport_certification_version
    ADD CONSTRAINT transport_certification_version_bundle_fk
    FOREIGN KEY (bundle_version_id) REFERENCES transport_certification_bundle (bundle_version_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE production_target_onboarding (
    onboarding_id UUID PRIMARY KEY,
    onboarding_version_id UUID NULL,
    production_target_id VARCHAR(128) NOT NULL REFERENCES production_network_target (target_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','IN_REVIEW','APPROVED','INVALID','SUSPENDED','REVOKED')),
    certification_level VARCHAR(8) NOT NULL CHECK (certification_level IN ('L0','L1','L2','L3')),
    created_by VARCHAR(128) NOT NULL,
    reviewed_by VARCHAR(128) NULL,
    approved_by VARCHAR(128) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (status <> 'APPROVED' OR (
        created_by IS DISTINCT FROM reviewed_by
        AND created_by IS DISTINCT FROM approved_by
        AND reviewed_by IS DISTINCT FROM approved_by
        AND reviewed_by IS NOT NULL
        AND approved_by IS NOT NULL
    ))
);

CREATE TABLE production_target_onboarding_version (
    onboarding_version_id UUID PRIMARY KEY,
    onboarding_id UUID NOT NULL REFERENCES production_target_onboarding (onboarding_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    version_no INTEGER NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    production_target_id VARCHAR(128) NOT NULL,
    vendor VARCHAR(32) NOT NULL CHECK (vendor IN ('ERICSSON')),
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('ENM')),
    vendor_software_version VARCHAR(128) NOT NULL,
    interface_definition_version_id UUID NOT NULL REFERENCES vendor_interface_definition (interface_definition_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    transport_profile_version_id UUID NOT NULL REFERENCES vendor_write_transport_profile (transport_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    artifact_digest CHAR(64) NOT NULL CHECK (artifact_digest ~ '^[0-9a-f]{64}$'),
    capability_cert_version_id UUID NOT NULL REFERENCES vendor_capability_certification (capability_cert_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    security_cert_version_id UUID NOT NULL REFERENCES vendor_security_certification (security_cert_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    credential_profile_version_id UUID NOT NULL REFERENCES production_credential_profile (credential_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    tls_profile_version_id UUID NOT NULL REFERENCES production_tls_profile (tls_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    network_policy_profile_version_id UUID NOT NULL REFERENCES production_network_policy_profile (network_policy_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    endpoint_profile_version_id UUID NOT NULL REFERENCES production_endpoint_profile (endpoint_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    bundle_version_id UUID NOT NULL REFERENCES transport_certification_bundle (bundle_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    change_control_policy VARCHAR(256) NOT NULL,
    verification_policy VARCHAR(256) NOT NULL,
    rollback_policy VARCHAR(256) NOT NULL,
    monitoring_profile VARCHAR(256) NOT NULL,
    support_owner VARCHAR(128) NOT NULL,
    environment VARCHAR(32) NOT NULL,
    region VARCHAR(64) NOT NULL,
    network_domain VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (onboarding_id, version_no)
);

ALTER TABLE production_target_onboarding
    ADD CONSTRAINT production_target_onboarding_current_version_fk
    FOREIGN KEY (onboarding_version_id) REFERENCES production_target_onboarding_version (onboarding_version_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE production_target_certification (
    target_certification_id UUID PRIMARY KEY,
    production_target_id VARCHAR(128) NOT NULL REFERENCES production_network_target (target_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    onboarding_version_id UUID NOT NULL REFERENCES production_target_onboarding_version (onboarding_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    bundle_version_id UUID NOT NULL REFERENCES transport_certification_bundle (bundle_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    status VARCHAR(32) NOT NULL CHECK (status IN ('CURRENT','STALE','INVALID','SUSPENDED','EXPIRED','REVOKED')),
    certified_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    content_digest CHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$')
);
CREATE UNIQUE INDEX production_target_certification_current_uidx
    ON production_target_certification (production_target_id) WHERE status = 'CURRENT';

CREATE TABLE vendor_version_compatibility (
    compatibility_id UUID PRIMARY KEY,
    vendor VARCHAR(32) NOT NULL CHECK (vendor IN ('ERICSSON')),
    platform VARCHAR(32) NOT NULL CHECK (platform IN ('ENM')),
    transport_profile_version_id UUID NOT NULL REFERENCES vendor_write_transport_profile (transport_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    version_predicate VARCHAR(512) NOT NULL,
    evidence_id UUID NOT NULL REFERENCES transport_certification_evidence (evidence_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    certified_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('ACTIVE','SUSPENDED','EXPIRED','REVOKED'))
);

CREATE TABLE vendor_transport_health (
    health_id UUID PRIMARY KEY,
    production_target_id VARCHAR(128) NOT NULL REFERENCES production_network_target (target_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    transport_profile_version_id UUID NOT NULL REFERENCES vendor_write_transport_profile (transport_profile_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    health_state VARCHAR(32) NOT NULL CHECK (health_state IN ('HEALTHY','DEGRADED','UNAVAILABLE','SECURITY_FAILURE','CAPABILITY_MISMATCH','VERSION_MISMATCH','SUSPENDED')),
    source VARCHAR(32) NOT NULL CHECK (source IN ('OBSERVATION','POLICY','HUMAN')),
    detail_code VARCHAR(128) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    requires_human_reactivation BOOLEAN NOT NULL,
    UNIQUE (production_target_id, transport_profile_version_id)
);

CREATE TABLE vendor_transport_health_event (
    health_event_id UUID PRIMARY KEY,
    production_target_id VARCHAR(128) NOT NULL,
    transport_profile_version_id UUID NOT NULL,
    health_state VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    detail_code VARCHAR(128) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE phase17_certification_audit_event (
    event_id UUID PRIMARY KEY,
    subject_type VARCHAR(64) NOT NULL,
    subject_id VARCHAR(128) NOT NULL,
    subject_version_id VARCHAR(128) NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_principal_id VARCHAR(128) NOT NULL,
    sequence_number BIGINT NOT NULL,
    previous_event_hash CHAR(64) NOT NULL CHECK (previous_event_hash ~ '^[0-9a-f]{64}$'),
    event_hash CHAR(64) NOT NULL CHECK (event_hash ~ '^[0-9a-f]{64}$'),
    payload_canonical TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (subject_type, subject_id, sequence_number)
);

CREATE TABLE phase17_invalidation_event (
    invalidation_event_id UUID PRIMARY KEY,
    idempotency_key CHAR(64) NOT NULL UNIQUE CHECK (idempotency_key ~ '^[0-9a-f]{64}$'),
    trigger_type VARCHAR(64) NOT NULL CHECK (trigger_type IN ('INTERFACE_REVOKED','INTERFACE_SUPERSEDED','DOCUMENTATION_WITHDRAWN','DOCUMENTATION_SUPERSEDED','APPROVAL_REVOKED','TRANSPORT_IMPLEMENTATION_CHANGED','ARTIFACT_DIGEST_CHANGED','ENDPOINT_PROFILE_CHANGED','NETWORK_PROFILE_CHANGED','TLS_PROFILE_CHANGED','SECURITY_PROFILE_CHANGED','CREDENTIAL_PROFILE_CHANGED','CAPABILITY_PROFILE_CHANGED','VENDOR_VERSION_MISMATCH','TARGET_ONBOARDING_CHANGED','TARGET_SUSPENDED','CERTIFICATION_EXPIRED','CERTIFICATION_REVOKED','PHASE16_L4_AUTHORIZATION_REVOKED','KILL_SWITCH_DISABLED')),
    source_table VARCHAR(128) NOT NULL,
    source_logical_id VARCHAR(128) NOT NULL,
    source_version_id UUID NULL,
    new_status VARCHAR(32) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    actor_principal_id VARCHAR(128) NOT NULL
);

CREATE TABLE phase17_invalidation_outbox (
    outbox_id UUID PRIMARY KEY,
    invalidation_event_id UUID NOT NULL REFERENCES phase17_invalidation_event (invalidation_event_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    payload_canonical TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ NULL
);

-- Certified snapshot content is versioned, never silently rewritten in place.
CREATE OR REPLACE FUNCTION phase17_reject_certified_content_digest_change()
RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'UPDATE'
       AND OLD.content_digest IS DISTINCT FROM NEW.content_digest
       AND COALESCE(OLD.status, '') <> 'DRAFT' THEN
        RAISE EXCEPTION 'phase17 certified content_digest is immutable'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER vendor_write_transport_profile_content_immutable
    BEFORE UPDATE ON vendor_write_transport_profile
    FOR EACH ROW EXECUTE PROCEDURE phase17_reject_certified_content_digest_change();
CREATE TRIGGER transport_certification_bundle_content_immutable
    BEFORE UPDATE ON transport_certification_bundle
    FOR EACH ROW EXECUTE PROCEDURE phase17_reject_certified_content_digest_change();
CREATE TRIGGER production_endpoint_profile_content_immutable
    BEFORE UPDATE ON production_endpoint_profile
    FOR EACH ROW EXECUTE PROCEDURE phase17_reject_certified_content_digest_change();
CREATE TRIGGER production_tls_profile_content_immutable
    BEFORE UPDATE ON production_tls_profile
    FOR EACH ROW EXECUTE PROCEDURE phase17_reject_certified_content_digest_change();
CREATE TRIGGER production_credential_profile_content_immutable
    BEFORE UPDATE ON production_credential_profile
    FOR EACH ROW EXECUTE PROCEDURE phase17_reject_certified_content_digest_change();
