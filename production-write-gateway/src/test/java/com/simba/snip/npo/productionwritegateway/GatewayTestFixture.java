package com.simba.snip.npo.productionwritegateway;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.simba.snip.npo.productionchange.protocol.ProductionRateLimitCounters;

public final class GatewayTestFixture {

    public static final String HASH_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    public static final String HASH_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private GatewayTestFixture() {
    }

    public static SeededGrant seedIssuedForwardGrant(JdbcTemplate jdbc) {
        return seedGrant(jdbc, "ISSUED", "FORWARD", Instant.now().plus(10, ChronoUnit.MINUTES), HASH_A, 1, 7L);
    }

    public static SeededGrant seedGrant(
            JdbcTemplate jdbc,
            String grantStatus,
            String grantType,
            Instant expiresAt,
            String fingerprint,
            int authGeneration,
            long fencingToken
    ) {
        String targetId = "target-" + UUID.randomUUID();
        UUID changeId = UUID.randomUUID();
        UUID grantId = UUID.randomUUID();
        UUID phase15 = UUID.randomUUID();
        Instant now = Instant.now();
        String cellId = "cell-" + UUID.randomUUID();
        String binding = HASH_A;

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
                """, targetId, HASH_A, Timestamp.from(now), Timestamp.from(now));

        jdbc.update("""
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, rollback_expected_value, rollback_desired_value,
                    requester_principal_id, audit_chain_integrity, created_at, updated_at, version)
                VALUES (?, ?, ?, 'CC-1', 'AUTHORIZED', ?, ?, ?, 'txPower',
                    10, 12, 12, 10, 'requester-1', 'VALID', ?, ?, 0)
                """, changeId, phase15, targetId, fingerprint, authGeneration, cellId,
                Timestamp.from(now), Timestamp.from(now));

        jdbc.update("""
                INSERT INTO production_change_control (
                    control_id, production_change_id, system, reference, status,
                    validated_by_principal_id, validated_at, valid_until)
                VALUES (?, ?, 'MANUAL', 'CC-1', 'VALID', 'validator-1', ?, ?)
                """, UUID.randomUUID(), changeId, Timestamp.from(now), Timestamp.from(now.plus(1, ChronoUnit.HOURS)));

        jdbc.update("""
                INSERT INTO production_execution_lease (
                    lease_id, production_target_id, cell_id, parameter, holder_id, fencing_token,
                    status, acquired_at, expires_at)
                VALUES (?, ?, ?, 'txPower', 'gateway-test', ?, 'ACTIVE', ?, ?)
                """, UUID.randomUUID(), targetId, cellId, fencingToken,
                Timestamp.from(now), Timestamp.from(now.plus(30, ChronoUnit.MINUTES)));

        Instant hourStart = ProductionRateLimitCounters.align(now, Duration.ofHours(1));
        Instant dayStart = ProductionRateLimitCounters.align(now, Duration.ofDays(1));
        jdbc.update("""
                INSERT INTO production_rate_limit_state (counter_id, scope_type, scope_key, window_start, count, updated_at)
                VALUES (?, ?, ?, ?, 0, ?)
                """, ProductionRateLimitCounters.counterId(ProductionRateLimitCounters.TARGET_HOUR, targetId, hourStart),
                ProductionRateLimitCounters.TARGET_HOUR, targetId, Timestamp.from(hourStart), Timestamp.from(now));
        jdbc.update("""
                INSERT INTO production_rate_limit_state (counter_id, scope_type, scope_key, window_start, count, updated_at)
                VALUES (?, ?, ?, ?, 0, ?)
                """, ProductionRateLimitCounters.counterId(ProductionRateLimitCounters.CELL_DAY, cellId, dayStart),
                ProductionRateLimitCounters.CELL_DAY, cellId, Timestamp.from(dayStart), Timestamp.from(now));

        jdbc.update("""
                INSERT INTO production_target_health (
                    health_id, production_target_id, health_state, outcome_unknown_count,
                    verification_failure_count, last_checked_at)
                VALUES (?, ?, 'HEALTHY', 0, 0, ?)
                """, UUID.randomUUID(), targetId, Timestamp.from(now));

        jdbc.update("""
                INSERT INTO production_execution_grant (
                    grant_id, production_change_id, phase15_execution_id, target_id, grant_type, status,
                    production_fingerprint, authorization_generation, fencing_token, operation_binding_hash,
                    issued_at, expires_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, grantId, changeId, phase15, targetId, grantType, grantStatus,
                fingerprint, authGeneration, fencingToken, binding,
                Timestamp.from(now), Timestamp.from(expiresAt));

        return new SeededGrant(
                grantId,
                changeId,
                phase15,
                targetId,
                cellId,
                fingerprint,
                authGeneration,
                fencingToken,
                binding,
                grantType,
                BigDecimal.TEN,
                new BigDecimal("12")
        );
    }

    public record SeededGrant(
            UUID grantId,
            UUID productionChangeId,
            UUID phase15ExecutionId,
            String targetId,
            String cellId,
            String fingerprint,
            int authorizationGeneration,
            long fencingToken,
            String operationBindingHash,
            String grantType,
            BigDecimal expectedValue,
            BigDecimal desiredValue
    ) {
    }
}
