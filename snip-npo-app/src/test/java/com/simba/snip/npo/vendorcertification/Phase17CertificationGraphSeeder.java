package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import com.simba.snip.npo.productionwritegateway.vendortransport.PackagedRuntimeTransportArtifactIdentityProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

final class Phase17CertificationGraphSeeder {

    static final String PACKAGED_DIGEST =
            new PackagedRuntimeTransportArtifactIdentityProvider().currentIdentity().artifactDigest();
    static final String BASELINE = "77fd24c0fd32c920c97ff5169f4bc8a93a77b208";

    private Phase17CertificationGraphSeeder() {
    }

    record Graph(
            String targetId,
            UUID interfaceId,
            UUID interfaceVersionId,
            UUID approvalId,
            UUID transportProfileVersionId,
            UUID bundleVersionId,
            UUID certificationId,
            UUID onboardingId,
            UUID targetCertificationId,
            UUID credentialProfileVersionId,
            UUID tlsProfileVersionId,
            UUID endpointProfileVersionId
    ) {
    }

    static Graph seed(JdbcTemplate jdbc, String targetId, String key) {
        Instant now = Instant.now();
        Timestamp ts = Timestamp.from(now);
        Timestamp exp = Timestamp.from(now.plusSeconds(86400L * 30));
        UUID tlsId = UUID.randomUUID();
        UUID tlsVid = UUID.randomUUID();
        UUID netId = UUID.randomUUID();
        UUID netVid = UUID.randomUUID();
        UUID credId = UUID.randomUUID();
        UUID credVid = UUID.randomUUID();
        UUID epId = UUID.randomUUID();
        UUID epVid = UUID.randomUUID();
        UUID secId = UUID.randomUUID();
        UUID secVid = UUID.randomUUID();
        UUID ifaceId = UUID.randomUUID();
        UUID ifaceVid = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID profileVid = UUID.randomUUID();
        UUID capId = UUID.randomUUID();
        UUID capVid = UUID.randomUUID();
        UUID artifactId = UUID.randomUUID();
        UUID certId = UUID.randomUUID();
        UUID certVid = UUID.randomUUID();
        UUID bundleId = UUID.randomUUID();
        UUID bundleVid = UUID.randomUUID();
        UUID onboardId = UUID.randomUUID();
        UUID onboardVid = UUID.randomUUID();
        UUID targetCertId = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();
        UUID compatId = UUID.randomUUID();
        UUID healthId = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO production_tls_profile (
                    tls_profile_version_id, tls_profile_id, version_no, content_digest, production_target_id,
                    environment, hostname_verification_required, trust_store_profile_ref, minimum_tls_policy,
                    server_identity_expectation, status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, ?, ?, 'LAB', TRUE, 'lab-trust', 'TLS_1_2', 'enm.lab.invalid', 'ACTIVE', ?, ?, 'seeder')
                """, tlsVid, tlsId, sha(key + "tls"), targetId, ts, ts);
        jdbc.update("""
                INSERT INTO production_network_policy_profile (
                    network_policy_profile_version_id, network_policy_profile_id, version_no, content_digest,
                    gateway_workload_id, production_target_id, environment, destination_identity, destination_port,
                    dns_requirement, private_route_class, allowed_egress_scope, status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, ?, 'gw-1', ?, 'LAB', 'enm.lab.invalid', 443, 'private', 'lab', '10.0.0.0/8',
                    'ACTIVE', ?, ?, 'seeder')
                """, netVid, netId, sha(key + "net"), targetId, ts, ts);
        jdbc.update("""
                INSERT INTO production_credential_profile (
                    credential_profile_version_id, credential_profile_id, version_no, content_digest,
                    production_target_id, vendor, platform, secret_reference, workload_identity_profile,
                    status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, ?, ?, 'ERICSSON', 'ENM', 'kv/ref-' || ?, 'wi-lab', 'ACTIVE', ?, ?, 'seeder')
                """, credVid, credId, sha(key + "cred"), targetId, key, ts, ts);
        jdbc.update("""
                INSERT INTO production_endpoint_profile (
                    endpoint_profile_version_id, endpoint_profile_id, version_no, content_digest, production_target_id,
                    environment, network_domain, approved_fqdn, approved_port, tls_server_identity, route_zone_id,
                    vendor, platform, status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, ?, ?, 'LAB', 'LAB', 'enm.lab.invalid', 443, 'enm.lab.invalid', 'zone-a',
                    'ERICSSON', 'ENM', 'ACTIVE', ?, ?, 'seeder')
                """, epVid, epId, sha(key + "ep"), targetId, ts, ts);
        jdbc.update("""
                INSERT INTO vendor_security_certification (
                    security_cert_version_id, security_cert_id, version_no, content_digest, tls_profile_version_id,
                    network_policy_profile_version_id, credential_profile_version_id, mtls_required, status,
                    certified_at, expires_at, created_at, created_by)
                VALUES (?, ?, 1, ?, ?, ?, ?, FALSE, 'ACTIVE', ?, ?, ?, 'seeder')
                """, secVid, secId, sha(key + "sec"), tlsVid, netVid, credVid, ts, exp, ts);
        jdbc.update("""
                INSERT INTO vendor_interface_definition (
                    interface_definition_version_id, interface_definition_id, version_no, content_digest, vendor, platform,
                    vendor_product_version_predicate, interface_type_category, documentation_reference, documentation_version,
                    documentation_status, status, effective_from, created_at, created_by, updated_at)
                VALUES (?, ?, 1, ?, 'ERICSSON', 'ENM', 'EXPLICIT:ENM-22', 'UNRESOLVED', 'doc', '1',
                    'ACTIVE', 'INTERFACE_VERIFIED', ?, ?, 'seeder', ?)
                """, ifaceVid, ifaceId, sha(key + "iface"), ts, ts, ts);
        jdbc.update("""
                INSERT INTO vendor_write_transport_profile (
                    transport_profile_version_id, transport_profile_id, version_no, content_digest, vendor, platform,
                    interface_definition_version_id, vendor_version_predicate, transport_implementation_version,
                    artifact_digest, security_cert_version_id, credential_profile_version_id, tls_profile_version_id,
                    network_policy_profile_version_id, expected_state_strategy, atomic_certified, mutation_strategy,
                    readback_strategy, rollback_strategy, timeout_policy, retry_policy, supported_object_types,
                    supported_parameters, certification_state, certification_expiry, status, effective_from,
                    created_at, created_by)
                VALUES (?, ?, 1, ?, 'ERICSSON', 'ENM', ?, 'EXPLICIT:ENM-22', 'unconfigured-0', ?, ?, ?, ?, ?,
                    'READ_THEN_WRITE', FALSE, 'single', 'readback', 'p16', '30s', 'none', 'CELL', 'txPower',
                    'DRAFT', ?, 'DRAFT', ?, ?, 'seeder')
                """, profileVid, profileId, sha(key + "profile"), ifaceVid, PACKAGED_DIGEST, secVid, credVid, tlsVid, netVid, exp, ts, ts);
        jdbc.update("""
                INSERT INTO vendor_capability_certification (
                    capability_cert_version_id, capability_cert_id, version_no, content_digest, object_type, parameter,
                    addressing_semantics, parameter_type, unit, valid_range, precision, read_semantics, write_semantics,
                    rollback_semantics, verification_semantics, propagation_delay, eventual_consistency,
                    conditional_write_supported, vendor, platform, version_predicate, transport_profile_version_id,
                    status, certified_at, expires_at, created_at, created_by)
                VALUES (?, ?, 1, ?, 'CELL', 'txPower', 'dn', 'int', 'dB', '1-46', '1', 'read', 'write', 'rb', 'verify',
                    '1s', 'none', FALSE, 'ERICSSON', 'ENM', 'EXPLICIT:ENM-22', ?, 'ACTIVE', ?, ?, ?, 'seeder')
                """, capVid, capId, sha(key + "cap"), profileVid, ts, exp, ts);
        jdbc.update("""
                UPDATE vendor_write_transport_profile
                SET capability_cert_version_id = ?, status = 'ACTIVE', certification_state = 'PRODUCTION_REGISTERED'
                WHERE transport_profile_version_id = ?
                """, capVid, profileVid);
        jdbc.update("""
                UPDATE vendor_interface_definition SET capability_cert_version_id = ? WHERE interface_definition_version_id = ?
                """, capVid, ifaceVid);
        jdbc.update("""
                INSERT INTO vendor_interface_approval (
                    approval_id, interface_definition_version_id, approver_principal_id, approval_status, approved_at,
                    content_digest)
                VALUES (?, ?, 'approver-1', 'APPROVED', ?, ?)
                """, approvalId, ifaceVid, ts, sha(key + "appr"));
        jdbc.update("""
                INSERT INTO vendor_transport_artifact (
                    artifact_id, artifact_digest, transport_implementation_version, source_baseline_sha, status)
                VALUES (?, ?, 'unconfigured-0', ?, 'CERTIFIED')
                ON CONFLICT (artifact_digest) DO NOTHING
                """, artifactId, PACKAGED_DIGEST, BASELINE);
        jdbc.update("""
                INSERT INTO transport_certification (
                    transport_certification_id, vendor, platform, transport_profile_version_id, state, environment_class,
                    created_by, updated_at)
                VALUES (?, 'ERICSSON', 'ENM', ?, 'PRODUCTION_REGISTERED', 'LAB', 'seeder', ?)
                """, certId, profileVid, ts);
        jdbc.update("""
                INSERT INTO transport_certification_bundle (
                    bundle_version_id, bundle_id, version_no, content_digest, vendor, platform,
                    interface_definition_version_id, interface_approval_id, transport_profile_version_id, artifact_digest,
                    transport_implementation_version, source_baseline_sha, vendor_version_predicate,
                    capability_cert_version_id, security_cert_version_id, credential_profile_version_id,
                    tls_profile_version_id, network_policy_profile_version_id, endpoint_profile_version_id, target_class,
                    active_evidence_set_digest, certifier_principal_id, certified_at, expires_at, status)
                VALUES (?, ?, 1, ?, 'ERICSSON', 'ENM', ?, ?, ?, ?, 'unconfigured-0', ?, 'EXPLICIT:ENM-22',
                    ?, ?, ?, ?, ?, ?, 'LAB', ?, 'certifier-1', ?, ?, 'ACTIVE')
                """, bundleVid, bundleId, sha(key + "bundle"), ifaceVid, approvalId, profileVid, PACKAGED_DIGEST,
                BASELINE, capVid, secVid, credVid, tlsVid, netVid, epVid, sha(key + "evset"), ts, exp);
        jdbc.update("""
                INSERT INTO transport_certification_version (
                    transport_certification_version_id, transport_certification_id, version_no, content_digest, state,
                    interface_definition_version_id, bundle_version_id, artifact_digest, source_baseline_sha,
                    actor_principal_id, created_at)
                VALUES (?, ?, 1, ?, 'PRODUCTION_REGISTERED', ?, ?, ?, ?, 'seeder', ?)
                """, certVid, certId, sha(key + "cv"), ifaceVid, bundleVid, PACKAGED_DIGEST, BASELINE, ts);
        jdbc.update("UPDATE transport_certification SET current_version_id = ? WHERE transport_certification_id = ?",
                certVid, certId);
        jdbc.update("""
                INSERT INTO production_target_onboarding (
                    onboarding_id, onboarding_version_id, production_target_id, status, certification_level,
                    created_by, reviewed_by, approved_by, created_at, updated_at)
                VALUES (?, NULL, ?, 'DRAFT', 'L0', 'creator-1', NULL, NULL, ?, ?)
                """, onboardId, targetId, ts, ts);
        jdbc.update("""
                INSERT INTO production_target_onboarding_version (
                    onboarding_version_id, onboarding_id, version_no, content_digest, production_target_id, vendor, platform,
                    vendor_software_version, interface_definition_version_id, transport_profile_version_id, artifact_digest,
                    capability_cert_version_id, security_cert_version_id, credential_profile_version_id, tls_profile_version_id,
                    network_policy_profile_version_id, endpoint_profile_version_id, bundle_version_id, change_control_policy,
                    verification_policy, rollback_policy, monitoring_profile, support_owner, environment, region,
                    network_domain, expires_at, created_at, created_by)
                VALUES (?, ?, 1, ?, ?, 'ERICSSON', 'ENM', 'ENM-22', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'p16', 'p16', 'p16', 'lab',
                    'owner', 'LAB', 'test', 'RAN', ?, ?, 'creator-1')
                """, onboardVid, onboardId, sha(key + "onb"), targetId, ifaceVid, profileVid, PACKAGED_DIGEST,
                capVid, secVid, credVid, tlsVid, netVid, epVid, bundleVid, exp, ts);
        jdbc.update("""
                UPDATE production_target_onboarding
                SET onboarding_version_id = ?, status = 'APPROVED', reviewed_by = 'reviewer-1', approved_by = 'approver-1'
                WHERE onboarding_id = ?
                """, onboardVid, onboardId);
        jdbc.update("""
                INSERT INTO production_target_certification (
                    target_certification_id, production_target_id, onboarding_version_id, bundle_version_id, status,
                    certified_at, expires_at, content_digest)
                VALUES (?, ?, ?, ?, 'CURRENT', ?, ?, ?)
                """, targetCertId, targetId, onboardVid, bundleVid, ts, exp, sha(key + "tcert"));
        jdbc.update("""
                INSERT INTO transport_certification_evidence (
                    evidence_id, evidence_version, certification_subject_type, certification_subject_id,
                    certification_subject_version_id, evidence_type, environment_level, issuer_principal_id,
                    certifier_permission, result, reference, evidence_hash, created_at, effective_at, status)
                VALUES (?, 1, 'BUNDLE', ?, ?, 'LAB_PROOF', 'L0', 'issuer-1', 'TRANSPORT_CERTIFY', 'PASS', 'ref',
                    ?, ?, ?, 'ACTIVE')
                """, evidenceId, bundleId, bundleVid, sha(key + "evid"), ts, ts);
        jdbc.update("""
                INSERT INTO vendor_version_compatibility (
                    compatibility_id, vendor, platform, transport_profile_version_id, version_predicate, evidence_id,
                    certified_at, expires_at, status)
                VALUES (?, 'ERICSSON', 'ENM', ?, 'EXPLICIT:ENM-22', ?, ?, ?, 'ACTIVE')
                """, compatId, profileVid, evidenceId, ts, exp);
        jdbc.update("""
                INSERT INTO vendor_transport_health (
                    health_id, production_target_id, transport_profile_version_id, health_state, source, detail_code,
                    observed_at, requires_human_reactivation)
                VALUES (?, ?, ?, 'HEALTHY', 'POLICY', 'ok', ?, FALSE)
                """, healthId, targetId, profileVid, ts);
        return new Graph(targetId, ifaceId, ifaceVid, approvalId, profileVid, bundleVid, certId, onboardId,
                targetCertId, credVid, tlsVid, epVid);
    }

    static Graph seedSecondProfile(JdbcTemplate jdbc, Graph primary, String key) {
        Instant now = Instant.now();
        Timestamp ts = Timestamp.from(now);
        Timestamp exp = Timestamp.from(now.plusSeconds(86400L * 30));
        UUID ifaceId = UUID.randomUUID();
        UUID ifaceVid = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID profileVid = UUID.randomUUID();
        UUID capId = UUID.randomUUID();
        UUID capVid = UUID.randomUUID();
        UUID certId = UUID.randomUUID();
        UUID bundleId = UUID.randomUUID();
        UUID bundleVid = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO vendor_interface_definition (
                    interface_definition_version_id, interface_definition_id, version_no, content_digest, vendor, platform,
                    vendor_product_version_predicate, interface_type_category, documentation_reference, documentation_version,
                    documentation_status, status, effective_from, created_at, created_by, updated_at)
                VALUES (?, ?, 1, ?, 'ERICSSON', 'ENM', 'EXPLICIT:ENM-22', 'UNRESOLVED', 'doc2', '1',
                    'ACTIVE', 'INTERFACE_VERIFIED', ?, ?, 'seeder', ?)
                """, ifaceVid, ifaceId, sha(key + "iface2"), ts, ts, ts);
        jdbc.update("""
                INSERT INTO vendor_write_transport_profile (
                    transport_profile_version_id, transport_profile_id, version_no, content_digest, vendor, platform,
                    interface_definition_version_id, vendor_version_predicate, transport_implementation_version,
                    artifact_digest, security_cert_version_id, credential_profile_version_id, tls_profile_version_id,
                    network_policy_profile_version_id, expected_state_strategy, atomic_certified, mutation_strategy,
                    readback_strategy, rollback_strategy, timeout_policy, retry_policy, supported_object_types,
                    supported_parameters, certification_state, certification_expiry, status, effective_from,
                    created_at, created_by)
                VALUES (?, ?, 1, ?, 'ERICSSON', 'ENM', ?, 'EXPLICIT:ENM-22', 'unconfigured-0', ?, ?, ?, ?, ?,
                    'READ_THEN_WRITE', FALSE, 'single', 'readback', 'p16', '30s', 'none', 'CELL', 'txPower',
                    'DRAFT', ?, 'DRAFT', ?, ?, 'seeder')
                """, profileVid, profileId, sha(key + "profile2"), ifaceVid, PACKAGED_DIGEST,
                jdbc.queryForObject("SELECT security_cert_version_id FROM vendor_write_transport_profile WHERE transport_profile_version_id = ?",
                        UUID.class, primary.transportProfileVersionId()),
                primary.credentialProfileVersionId(), primary.tlsProfileVersionId(),
                jdbc.queryForObject("SELECT network_policy_profile_version_id FROM vendor_write_transport_profile WHERE transport_profile_version_id = ?",
                        UUID.class, primary.transportProfileVersionId()),
                exp, ts, ts);
        jdbc.update("""
                INSERT INTO vendor_capability_certification (
                    capability_cert_version_id, capability_cert_id, version_no, content_digest, object_type, parameter,
                    addressing_semantics, parameter_type, unit, valid_range, precision, read_semantics, write_semantics,
                    rollback_semantics, verification_semantics, propagation_delay, eventual_consistency,
                    conditional_write_supported, vendor, platform, version_predicate, transport_profile_version_id,
                    status, certified_at, expires_at, created_at, created_by)
                VALUES (?, ?, 1, ?, 'CELL', 'txPower', 'dn', 'int', 'dB', '1-46', '1', 'read', 'write', 'rb', 'verify',
                    '1s', 'none', FALSE, 'ERICSSON', 'ENM', 'EXPLICIT:ENM-22', ?, 'ACTIVE', ?, ?, ?, 'seeder')
                """, capVid, capId, sha(key + "cap2"), profileVid, ts, exp, ts);
        jdbc.update("""
                UPDATE vendor_write_transport_profile
                SET capability_cert_version_id = ?, status = 'ACTIVE', certification_state = 'PRODUCTION_REGISTERED'
                WHERE transport_profile_version_id = ?
                """, capVid, profileVid);
        jdbc.update("""
                INSERT INTO vendor_interface_approval (
                    approval_id, interface_definition_version_id, approver_principal_id, approval_status, approved_at,
                    content_digest)
                VALUES (?, ?, 'approver-1', 'APPROVED', ?, ?)
                """, approvalId, ifaceVid, ts, sha(key + "appr2"));
        jdbc.update("""
                INSERT INTO transport_certification (
                    transport_certification_id, vendor, platform, transport_profile_version_id, state, environment_class,
                    created_by, updated_at)
                VALUES (?, 'ERICSSON', 'ENM', ?, 'PRODUCTION_REGISTERED', 'LAB', 'seeder', ?)
                """, certId, profileVid, ts);
        jdbc.update("""
                INSERT INTO transport_certification_bundle (
                    bundle_version_id, bundle_id, version_no, content_digest, vendor, platform,
                    interface_definition_version_id, interface_approval_id, transport_profile_version_id, artifact_digest,
                    transport_implementation_version, source_baseline_sha, vendor_version_predicate,
                    capability_cert_version_id, security_cert_version_id, credential_profile_version_id,
                    tls_profile_version_id, network_policy_profile_version_id, endpoint_profile_version_id, target_class,
                    active_evidence_set_digest, certifier_principal_id, certified_at, expires_at, status)
                VALUES (?, ?, 1, ?, 'ERICSSON', 'ENM', ?, ?, ?, ?, 'unconfigured-0', ?, 'EXPLICIT:ENM-22',
                    ?, ?, ?, ?, ?, ?, 'LAB', ?, 'certifier-1', ?, ?, 'ACTIVE')
                """, bundleVid, bundleId, sha(key + "bundle2"), ifaceVid, approvalId, profileVid, PACKAGED_DIGEST,
                BASELINE, capVid, secVidFrom(jdbc, primary), primary.credentialProfileVersionId(),
                primary.tlsProfileVersionId(), netVidFrom(jdbc, primary), primary.endpointProfileVersionId(),
                sha(key + "evset2"), ts, exp);
        UUID secondTargetCert = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO production_target_certification (
                    target_certification_id, production_target_id, onboarding_version_id, bundle_version_id, status,
                    certified_at, expires_at, content_digest)
                VALUES (?, ?, ?, ?, 'STALE', ?, ?, ?)
                """, secondTargetCert, primary.targetId(),
                jdbc.queryForObject("SELECT onboarding_version_id FROM production_target_onboarding WHERE onboarding_id = ?",
                        UUID.class, primary.onboardingId()),
                bundleVid, ts, exp, sha(key + "tcert2"));
        return new Graph(primary.targetId(), ifaceId, ifaceVid, approvalId, profileVid, bundleVid, certId,
                primary.onboardingId(), secondTargetCert, primary.credentialProfileVersionId(),
                primary.tlsProfileVersionId(), primary.endpointProfileVersionId());
    }

    private static UUID secVidFrom(JdbcTemplate jdbc, Graph primary) {
        return jdbc.queryForObject(
                "SELECT security_cert_version_id FROM vendor_write_transport_profile WHERE transport_profile_version_id = ?",
                UUID.class, primary.transportProfileVersionId());
    }

    private static UUID netVidFrom(JdbcTemplate jdbc, Graph primary) {
        return jdbc.queryForObject(
                "SELECT network_policy_profile_version_id FROM vendor_write_transport_profile WHERE transport_profile_version_id = ?",
                UUID.class, primary.transportProfileVersionId());
    }

    private static String sha(String seed) {
        return Sha256Hex.hash(seed);
    }
}
