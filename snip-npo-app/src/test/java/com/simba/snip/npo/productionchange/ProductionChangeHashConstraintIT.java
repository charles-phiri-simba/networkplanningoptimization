package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeHashConstraintIT extends ProductionChangeITSupport {

    private static final String VALID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    void sha256HexInvariantEnforced() {
        String targetId = "hash-ok-" + UUID.randomUUID();
        insertTarget(targetId, VALID);

        assertRejected("63", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertRejected("65", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertRejected("non-hex", "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
        assertRejected("uppercase", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        assertTrue(jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_network_target WHERE target_id = ?",
                Integer.class,
                targetId) >= 1);
    }

    private void assertRejected(String label, String fingerprint) {
        DataIntegrityViolationException ex = assertThrows(
                DataIntegrityViolationException.class,
                () -> insertTarget("hash-bad-" + label + "-" + UUID.randomUUID(), fingerprint),
                label);
        String cause = String.valueOf(ex.getMostSpecificCause()).toLowerCase();
        assertTrue(
                cause.contains("chk_sha256")
                        || cause.contains("violates check")
                        || cause.contains("too long")
                        || cause.contains("character varying"),
                label + " rejected for unexpected reason: " + cause);
    }

    private void insertTarget(String targetId, String fingerprint) {
        java.sql.Timestamp now = java.sql.Timestamp.from(java.time.Instant.now());
        jdbc.update("""
                INSERT INTO production_network_target (
                    target_id, vendor, platform, environment, adapter_profile_id, capability_profile_version,
                    security_profile_id, credential_profile_id, allowed_object_types, allowed_parameters,
                    change_window_policy, verification_policy, certification_level, enabled, target_state,
                    target_fingerprint, expected_state_guard_strength, created_at, updated_at, version)
                VALUES (?, 'ERICSSON', 'ENM', 'LAB', 'ERICSSON_ENM_LAB_ADAPTER', '1',
                    'STRICT_TLS', 'ericsson-enm-lab-write', 'CELL', 'txPower',
                    'ALWAYS_OPEN', 'ALLOW_READ_THEN_WRITE', 'L0', TRUE, 'ACTIVE',
                    ?, 'READ_THEN_WRITE', ?, ?, 0)
                """, targetId, fingerprint, now, now);
    }
}
