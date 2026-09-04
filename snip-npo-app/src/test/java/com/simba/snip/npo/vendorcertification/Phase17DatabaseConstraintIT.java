package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.ProductionChangeITSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17DatabaseConstraintIT extends ProductionChangeITSupport {

    @AfterEach
    void cleanup() {
        Phase17GraphCleanup.deleteAll(jdbc);
    }

    @Test
    void t17Db001To031RuntimeConstraints() {
        Phase17CertificationGraphSeeder.Graph graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_ID, "db");
        Timestamp now = Timestamp.from(Instant.now());

        assertTrue(count("SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE constraint_type = 'FOREIGN KEY' AND table_name = 'production_target_certification'") >= 1);

        jdbc.update("""
                INSERT INTO vendor_abstract_protocol_placeholder (
                    placeholder_version_id, placeholder_id, version_no, content_digest, interface_type_category,
                    status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'UNRESOLVED', 'DRAFT', ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now);
        String ifaceDigest = jdbc.queryForObject(
                "SELECT content_digest FROM vendor_interface_definition WHERE interface_definition_id = ?",
                String.class, graph.interfaceId());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_interface_definition (
                    interface_definition_version_id, interface_definition_id, version_no, content_digest, vendor, platform,
                    vendor_product_version_predicate, interface_type_category, documentation_reference, documentation_version,
                    documentation_status, status, effective_from, created_at, created_by, updated_at)
                VALUES (?, ?, 2, ?, 'ERICSSON', 'ENM', 'EXPLICIT:ENM-22', 'UNRESOLVED', 'doc', '1',
                    'ACTIVE', 'DRAFT', ?, ?, 'tester', ?)
                """, UUID.randomUUID(), graph.interfaceId(), ifaceDigest, now, now, now));

        String digest = jdbc.queryForObject(
                "SELECT content_digest FROM production_tls_profile WHERE tls_profile_version_id = ?",
                String.class, graph.tlsProfileVersionId());
        assertThrows(Exception.class, () -> jdbc.update(
                "UPDATE production_tls_profile SET content_digest = ? WHERE tls_profile_version_id = ?",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                graph.tlsProfileVersionId()));
        assertEquals(digest, jdbc.queryForObject(
                "SELECT content_digest FROM production_tls_profile WHERE tls_profile_version_id = ?",
                String.class, graph.tlsProfileVersionId()));

        assertTrue(count("SELECT COUNT(*) FROM information_schema.check_constraints "
                + "WHERE check_clause LIKE '%DRAFT%' AND constraint_schema = 'public'") >= 1);
        assertTrue(count("SELECT COUNT(*) FROM information_schema.check_constraints "
                + "WHERE check_clause LIKE '%0-9a-f%' AND constraint_schema = 'public'") >= 20);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_abstract_protocol_placeholder (
                    placeholder_version_id, placeholder_id, version_no, content_digest, interface_type_category,
                    status, effective_from, effective_until, created_at, created_by)
                VALUES (?, ?, 1, 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',
                    'UNRESOLVED', 'DRAFT', ?, ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now, now));

        assertEquals(1, count("SELECT COUNT(*) FROM production_target_certification WHERE production_target_id = ? AND status = 'CURRENT'",
                TARGET_ID));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO production_target_certification (
                    target_certification_id, production_target_id, onboarding_version_id, bundle_version_id, status,
                    certified_at, expires_at, content_digest)
                VALUES (?, ?, ?, ?, 'CURRENT', ?, ?, ?)
                """, UUID.randomUUID(), TARGET_ID, graph.targetCertificationId() == null ? UUID.randomUUID() : jdbc.queryForObject(
                        "SELECT onboarding_version_id FROM production_target_certification WHERE target_certification_id = ?",
                        UUID.class, graph.targetCertificationId()), graph.bundleVersionId(), now, now,
                "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"));

        assertTrue(count("SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'transport_certification_evidence_active_pass_uidx'") == 1);
        assertTrue(count("SELECT COUNT(*) FROM transport_certification_bundle WHERE tls_profile_version_id IS NOT NULL "
                + "AND network_policy_profile_version_id IS NOT NULL AND bundle_version_id = ?", graph.bundleVersionId()) == 1);
        assertTrue(count("SELECT COUNT(*) FROM production_credential_profile WHERE credential_profile_version_id = ? "
                + "AND production_target_id IS NOT NULL", graph.credentialProfileVersionId()) == 1);
        assertTrue(count("SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'production_endpoint_profile_active_target_uidx'") == 1);
        assertTrue(count("SELECT COUNT(*) FROM vendor_write_transport_profile WHERE certification_expiry IS NOT NULL "
                + "AND transport_profile_version_id = ?", graph.transportProfileVersionId()) == 1);
        assertTrue(count("SELECT COUNT(*) FROM transport_certification WHERE state = 'PRODUCTION_REGISTERED' "
                + "AND transport_certification_id = ?", graph.certificationId()) == 1);
        jdbc.update(
                "INSERT INTO phase17_invalidation_event (invalidation_event_id, idempotency_key, trigger_type, "
                        + "source_table, source_logical_id, new_status, effective_at, processed_at, actor_principal_id) "
                        + "VALUES (?, 'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee', "
                        + "'CERTIFICATION_REVOKED', 't', 'x', 'REVOKED', ?, ?, 'a')",
                UUID.randomUUID(), now, now);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO phase17_invalidation_event (invalidation_event_id, idempotency_key, trigger_type, "
                        + "source_table, source_logical_id, new_status, effective_at, processed_at, actor_principal_id) "
                        + "VALUES (?, 'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee', "
                        + "'CERTIFICATION_REVOKED', 't', 'x', 'REVOKED', ?, ?, 'a')",
                UUID.randomUUID(), now, now));

        Integer repos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.routines WHERE routine_name LIKE '%phase17%delete%'",
                Integer.class);
        assertTrue(repos == null || repos == 0);
        assertTrue(count("SELECT COUNT(*) FROM vendor_interface_approval WHERE approval_id = ?", graph.approvalId()) == 1);
        assertTrue(count("SELECT COUNT(*) FROM transport_certification_bundle WHERE active_evidence_set_digest ~ '^[0-9a-f]{64}$' "
                + "AND bundle_version_id = ?", graph.bundleVersionId()) == 1);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE production_target_onboarding SET created_by = reviewed_by, status = 'APPROVED' WHERE onboarding_id = ?",
                graph.onboardingId()));
        assertEquals(Boolean.FALSE, jdbc.queryForObject(
                "SELECT atomic_certified FROM vendor_write_transport_profile WHERE transport_profile_version_id = ?",
                Boolean.class, graph.transportProfileVersionId()));
        assertTrue(count("SELECT COUNT(*) FROM production_network_policy_profile WHERE allowed_egress_scope <> '0.0.0.0/0'") >= 1);
        assertEquals(Boolean.TRUE, jdbc.queryForObject(
                "SELECT hostname_verification_required FROM production_tls_profile WHERE tls_profile_version_id = ?",
                Boolean.class, graph.tlsProfileVersionId()));
        assertTrue(count("SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'vendor_write_transport_profile' "
                + "AND column_name = 'atomic_certified'") == 1);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_transport_artifact (
                    artifact_id, artifact_digest, transport_implementation_version, source_baseline_sha, status)
                VALUES (?, 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', 'x', '77FD24C0FD32C920C97FF5169F4BC8A93A77B208', 'REGISTERED')
                """, UUID.randomUUID()));
        assertTrue(count("SELECT COUNT(*) FROM information_schema.referential_constraints "
                + "WHERE delete_rule IN ('RESTRICT','NO ACTION') AND constraint_schema = 'public'") >= 10);
        assertTrue(count("SELECT COUNT(*) FROM pg_indexes WHERE indexname LIKE '%active%' AND tablename LIKE '%transport%'") >= 1);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO production_tls_profile (
                    tls_profile_version_id, tls_profile_id, version_no, content_digest, production_target_id,
                    environment, hostname_verification_required, trust_store_profile_ref, minimum_tls_policy,
                    server_identity_expectation, status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab',
                    NULL, 'PROD', TRUE, 't', 'TLS_1_2', 'x', 'ACTIVE', ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now));
        assertTrue(count("SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'phase17_invalidation_event' "
                + "AND column_name = 'idempotency_key'") == 1);
        assertTrue(count("SELECT COUNT(*) FROM transport_certification_bundle WHERE bundle_version_id = ?",
                graph.bundleVersionId()) == 1);
        Integer issuedPredicate = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'production_execution_grant' "
                        + "AND column_name = 'status'",
                Integer.class);
        assertTrue(issuedPredicate != null && issuedPredicate == 1);
    }

    private int count(String sql, Object... args) {
        Integer value = args.length == 0
                ? jdbc.queryForObject(sql, Integer.class)
                : jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
