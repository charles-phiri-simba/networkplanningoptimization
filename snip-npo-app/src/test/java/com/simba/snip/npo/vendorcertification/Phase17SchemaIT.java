package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.ProductionChangeITSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17SchemaIT extends ProductionChangeITSupport {

    @AfterEach
    void cleanupPhase17() {
        Phase17GraphCleanup.deleteAll(jdbc);
    }

    @Test
    void hashAndL4Checks() {
        Integer sha = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.check_constraints
                WHERE check_clause LIKE '%0-9a-f%' AND constraint_schema = 'public'
                """,
                Integer.class);
        assertTrue(sha != null && sha >= 8);

        Timestamp now = Timestamp.from(Instant.now());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_abstract_protocol_placeholder (
                    placeholder_version_id, placeholder_id, version_no, content_digest, interface_type_category,
                    status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, 'ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789',
                    'UNRESOLVED', 'DRAFT', ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now));

        jdbc.update("""
                INSERT INTO vendor_abstract_protocol_placeholder (
                    placeholder_version_id, placeholder_id, version_no, content_digest, interface_type_category,
                    status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'UNRESOLVED', 'DRAFT', ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now);

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_abstract_protocol_placeholder (
                    placeholder_version_id, placeholder_id, version_no, content_digest, interface_type_category,
                    status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'UNRESOLVED', 'DRAFT', ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_abstract_protocol_placeholder (
                    placeholder_version_id, placeholder_id, version_no, content_digest, interface_type_category,
                    status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'UNRESOLVED', 'DRAFT', ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_abstract_protocol_placeholder (
                    placeholder_version_id, placeholder_id, version_no, content_digest, interface_type_category,
                    status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, 'zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz',
                    'UNRESOLVED', 'DRAFT', ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_abstract_protocol_placeholder (
                    placeholder_version_id, placeholder_id, version_no, content_digest, interface_type_category,
                    status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, '',
                    'UNRESOLVED', 'DRAFT', ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_transport_artifact (
                    artifact_id, artifact_digest, transport_implementation_version, source_baseline_sha, status)
                VALUES (?, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'unconfigured-0', '77fd24c0fd32c920c97ff5169f4bc8a93a77b20', 'REGISTERED')
                """, UUID.randomUUID()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_transport_artifact (
                    artifact_id, artifact_digest, transport_implementation_version, source_baseline_sha, status)
                VALUES (?, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'unconfigured-0', '77fd24c0fd32c920c97ff5169f4bc8a93a77b2081', 'REGISTERED')
                """, UUID.randomUUID()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_transport_artifact (
                    artifact_id, artifact_digest, transport_implementation_version, source_baseline_sha, status)
                VALUES (?, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'unconfigured-0', '77FD24C0FD32C920C97FF5169F4BC8A93A77B208', 'REGISTERED')
                """, UUID.randomUUID()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_transport_artifact (
                    artifact_id, artifact_digest, transport_implementation_version, source_baseline_sha, status)
                VALUES (?, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'unconfigured-0', '', 'REGISTERED')
                """, UUID.randomUUID()));
        jdbc.update("""
                INSERT INTO vendor_transport_artifact (
                    artifact_id, artifact_digest, transport_implementation_version, source_baseline_sha, status)
                VALUES (?, 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
                    'unconfigured-0', '77fd24c0fd32c920c97ff5169f4bc8a93a77b208', 'REGISTERED')
                """, UUID.randomUUID());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_abstract_protocol_placeholder (
                    placeholder_version_id, placeholder_id, version_no, content_digest, interface_type_category,
                    status, effective_from, created_at, created_by)
                VALUES (?, ?, 1, '                                                                ',
                    'UNRESOLVED', 'DRAFT', ?, ?, 'tester')
                """, UUID.randomUUID(), UUID.randomUUID(), now, now));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO vendor_transport_artifact (
                    artifact_id, artifact_digest, transport_implementation_version, source_baseline_sha, status)
                VALUES (?, 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',
                    'unconfigured-0', '                                        ', 'REGISTERED')
                """, UUID.randomUUID()));

        Integer l4 = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.check_constraints
                WHERE constraint_name LIKE '%onboarding%' AND check_clause LIKE '%L4%'
                """,
                Integer.class);
        assertTrue(l4 == null || l4 == 0);
    }
}
