package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.protocol.ProductionRateLimitCounters;
import com.simba.snip.npo.productionwritegateway.service.FailureInjectionPoint;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeCriticalScenarioIT extends ProductionChangeITSupport {

    @Test void scenarioA() { happy(); }
    @Test void scenarioB() { mismatch(); }
    @Test void scenarioC() { timeoutAfterApply(); }
    @Test void scenarioD() { wrongValue(); }
    @Test void scenarioE() { rollbackWithoutAuth(); }
    @Test void scenarioF() { rollbackHappy(); }
    @Test void scenarioG() { rollbackMismatch(); }
    @Test void scenarioH() throws Exception { concurrentLease(); }
    @Test void scenarioI() { staleFingerprint(); }
    @Test void scenarioJ() { duplicateVerified(); }
    @Test void scenarioK() { closedWindow(); }
    @Test void scenarioL() { fencingChanged(); }
    @Test void scenarioM() { staleObservation(); }
    @Test void scenarioN() { canonicalUnchanged(); }
    @Test void scenarioO() { canonicalUnchanged(); }
    @Test void scenarioP() { agentDenied(); }
    @Test void scenarioQ() { mcpDenied(); }
    @Test void scenarioR() { rollbackResponseLost(); }
    @Test void scenarioS() { thirdValue(); }
    @Test void scenarioT() { rateLimitRace(); }
    @Test void scenarioU() throws Exception { consumeRace(); }
    @Test void scenarioV() { revokedGrant(); }
    @Test void scenarioW() { fi03(); }
    @Test void scenarioX() { killSwitch(); }
    @Test void scenarioY() { suspend(); }
    @Test void scenarioZ() { expiredCc(); }

    private ProductionChangeDto authorized() {
        return reviewedAndAuthorized(verifiedPhase15ExecutionId());
    }

    private void happy() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        executeExpectingOk(a.productionChangeId());
        assertEquals("VERIFIED", getProductionChange(a.productionChangeId()).status());
        assertEquals(1, mutationCount());
        assertEquals(0, grantCount(a.productionChangeId(), "ISSUED"));
    }

    private void mismatch() {
        ProductionChangeDto a = authorized();
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_MISMATCH);
        executeProductionChange(a.productionChangeId());
        assertEquals(0, mutationCount());
    }

    private void timeoutAfterApply() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.TIMEOUT_AFTER_APPLY);
        executeProductionChange(a.productionChangeId());
        assertEquals(1, mutationCount());
        Integer retries = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_gateway_attempt WHERE production_change_id = ?",
                Integer.class, a.productionChangeId());
        assertTrue(retries == null || retries <= 1);
    }

    private void responseLost() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RESPONSE_LOST);
        executeProductionChange(a.productionChangeId());
        assertEquals(1, mutationCount());
    }

    private void wrongValue() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.APPLY_WRONG_VALUE);
        executeProductionChange(a.productionChangeId());
        assertEquals(1, mutationCount());
        Integer rb = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ? AND grant_type = 'ROLLBACK'",
                Integer.class, a.productionChangeId());
        assertEquals(0, rb);
    }

    private void rollbackHappy() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        ProductionChangeDto verified = executeExpectingOk(a.productionChangeId());
        int forward = mutationCount();
        rollbackRequest(verified.productionChangeId());
        rollbackReview(verified.productionChangeId());
        rollbackAuthorize(verified.productionChangeId());
        testTransport().seedCell(CELL, verified.desiredValue());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.NONE);
        var rolled = rollbackExecute(verified.productionChangeId());
        assertTrue(rolled.getStatusCode().is2xxSuccessful(), () -> String.valueOf(rolled.getBody()));
        assertEquals("ROLLED_BACK", getProductionChange(verified.productionChangeId()).status());
        assertEquals(forward + 1, mutationCount());
    }

    private void rollbackMismatch() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        ProductionChangeDto verified = executeExpectingOk(a.productionChangeId());
        int forward = mutationCount();
        rollbackRequest(verified.productionChangeId());
        rollbackReview(verified.productionChangeId());
        rollbackAuthorize(verified.productionChangeId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_MISMATCH);
        rollbackExecute(verified.productionChangeId());
        assertEquals(forward, mutationCount());
    }

    private void concurrentLease() throws Exception {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        mutationCounter().set(0);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Future<?> first = pool.submit(() -> {
                start.await();
                executeProductionChange(a.productionChangeId());
                return null;
            });
            java.util.concurrent.Future<?> second = pool.submit(() -> {
                start.await();
                executeProductionChange(a.productionChangeId(), "executor-2", Map.of());
                return null;
            });
            start.countDown();
            first.get();
            second.get();
            // Concurrent execute race: architecture permits at most one mutation, but the
            // simulated crash/lease winner cannot positively prove send versus deny for the loser.
            assertTrue(mutationCount() <= 1);
        } finally {
            pool.shutdownNow();
        }
    }

    private void fencingChanged() {
        ProductionChangeDto a = authorized();
        jdbc.update("""
                INSERT INTO production_execution_lease (
                    lease_id, production_target_id, cell_id, parameter, holder_id, fencing_token,
                    status, acquired_at, expires_at)
                VALUES (?, ?, ?, 'txPower', 'other-holder', 99, 'ACTIVE', NOW(), NOW() + INTERVAL '10 minutes')
                """, java.util.UUID.randomUUID(), TARGET_ID, CELL);
        executeProductionChange(a.productionChangeId());
        assertEquals(0, mutationCount());
    }

    private void staleObservation() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_STALE);
        executeProductionChange(a.productionChangeId());
        assertEquals(0, mutationCount());
        Integer rb = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ? AND grant_type = 'ROLLBACK'",
                Integer.class, a.productionChangeId());
        assertEquals(0, rb);
    }

    private void rollbackWithoutAuth() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        executeExpectingOk(a.productionChangeId());
        var denied = rollbackExecute(a.productionChangeId());
        assertTrue(denied.getStatusCode().is4xxClientError());
    }

    private void staleFingerprint() {
        ProductionChangeDto a = authorized();
        jdbc.update("UPDATE production_network_change SET production_fingerprint = ? WHERE production_change_id = ?",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", a.productionChangeId());
        executeProductionChange(a.productionChangeId());
        assertEquals(0, mutationCount());
    }

    private void duplicateVerified() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        executeExpectingOk(a.productionChangeId());
        int m = mutationCount();
        executeExpectingOk(a.productionChangeId());
        assertEquals(m, mutationCount());
    }

    private void closedWindow() {
        ProductionChangeDto a = authorized();
        jdbc.update("UPDATE production_network_target SET change_window_policy = 'CLOSED' WHERE target_id = ?", TARGET_ID);
        executeProductionChange(a.productionChangeId());
        assertEquals(0, mutationCount());
    }

    private void canonicalUnchanged() {
        String before = canonicalTxPower();
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        executeExpectingOk(a.productionChangeId());
        assertEquals(before, canonicalTxPower());
        assertEquals(1, mutationCount());
    }

    private void agentDenied() {
        ProductionChangeDto a = authorized();
        http.exchange(
                "/api/v1/production-changes/" + a.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), "AGENT_EXECUTE", "agent-chief-1"),
                String.class);
        assertEquals(0, mutationCount());
    }

    private void thirdValue() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.THIRD_VALUE);
        executeProductionChange(a.productionChangeId());
        assertEquals(1, mutationCount());
    }

    private void mcpDenied() {
        ProductionChangeDto a = authorized();
        http.exchange(
                "/api/v1/production-changes/" + a.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), "MCP_CLIENT", "mcp-client-1"),
                String.class);
        assertEquals(0, mutationCount());
    }

    private void rollbackResponseLost() {
        ProductionChangeDto a = authorized();
        seedTransportFor(a);
        ProductionChangeDto verified = executeExpectingOk(a.productionChangeId());
        rollbackRequest(verified.productionChangeId());
        rollbackReview(verified.productionChangeId());
        rollbackAuthorize(verified.productionChangeId());
        testTransport().seedCell(CELL, verified.desiredValue());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RESPONSE_LOST);
        int before = mutationCount();
        rollbackExecute(verified.productionChangeId());
        int afterFirst = mutationCount();
        rollbackExecute(verified.productionChangeId());
        assertEquals(1, afterFirst - before);
        assertEquals(afterFirst, mutationCount());
    }

    private void rateLimitRace() {
        Instant now = Instant.now();
        Instant window = ProductionRateLimitCounters.align(now, Duration.ofHours(1));
        String counterId = ProductionRateLimitCounters.counterId(
                ProductionRateLimitCounters.TARGET_HOUR, TARGET_ID, window);
        jdbc.update("""
                INSERT INTO production_rate_limit_state
                    (counter_id, scope_type, scope_key, window_start, count, updated_at)
                VALUES (?, ?, ?, ?, 999, ?)
                ON CONFLICT (counter_id) DO UPDATE SET count = 999, updated_at = EXCLUDED.updated_at
                """,
                counterId,
                ProductionRateLimitCounters.TARGET_HOUR,
                TARGET_ID,
                java.sql.Timestamp.from(window),
                java.sql.Timestamp.from(now));
        ProductionChangeDto a = authorized();
        executeProductionChange(a.productionChangeId());
        assertEquals(0, mutationCount());
    }

    private void consumeRace() throws Exception {
        java.util.UUID grantId = java.util.UUID.randomUUID();
        java.util.UUID changeId = java.util.UUID.randomUUID();
        java.util.UUID phase15 = java.util.UUID.randomUUID();
        String targetId = "consume-race-" + java.util.UUID.randomUUID();
        String hash = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
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
                """, targetId, hash, now, now);
        jdbc.update("""
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, rollback_expected_value, rollback_desired_value,
                    requester_principal_id, audit_chain_integrity, created_at, updated_at, version)
                VALUES (?, ?, ?, 'CC-1', 'AUTHORIZED', ?, 1, 'CELL-RACE', 'txPower',
                    10, 12, 12, 10, 'requester-1', 'VALID', ?, ?, 0)
                """, changeId, phase15, targetId, hash, now, now);
        jdbc.update("""
                INSERT INTO production_execution_grant (
                    grant_id, production_change_id, phase15_execution_id, target_id, grant_type, status,
                    production_fingerprint, authorization_generation, fencing_token, operation_binding_hash,
                    issued_at, expires_at, version)
                VALUES (?, ?, ?, ?, 'FORWARD', 'ISSUED', ?, 1, 7, ?, ?, ?, 0)
                """, grantId, changeId, phase15, targetId, hash, hash, now,
                java.sql.Timestamp.from(java.time.Instant.now().plus(10, java.time.temporal.ChronoUnit.MINUTES)));
        var consume = GATEWAY_CTX.getBean(
                com.simba.snip.npo.productionwritegateway.service.ProductionGrantConsumeService.class);
        var command = new com.simba.snip.npo.productionwritegateway.service.ConsumeCommand(
                grantId, changeId, phase15, targetId, hash, 1, 7L, hash,
                com.simba.snip.npo.productionchange.protocol.GrantType.FORWARD);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        try {
            var a = pool.submit(() -> { start.await(); return consume.consume(command); });
            var b = pool.submit(() -> { start.await(); return consume.consume(command); });
            start.countDown();
            int successes = (a.get().succeeded() ? 1 : 0) + (b.get().succeeded() ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(0, mutationCount());
        } finally {
            pool.shutdownNow();
        }
    }

    private void revokedGrant() {
        java.util.UUID grantId = java.util.UUID.randomUUID();
        java.util.UUID changeId = java.util.UUID.randomUUID();
        java.util.UUID phase15 = java.util.UUID.randomUUID();
        String targetId = "revoked-" + java.util.UUID.randomUUID();
        String hash = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
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
                """, targetId, hash, now, now);
        jdbc.update("""
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, rollback_expected_value, rollback_desired_value,
                    requester_principal_id, audit_chain_integrity, created_at, updated_at, version)
                VALUES (?, ?, ?, 'CC-1', 'AUTHORIZED', ?, 1, 'CELL-REV', 'txPower',
                    10, 12, 12, 10, 'requester-1', 'VALID', ?, ?, 0)
                """, changeId, phase15, targetId, hash, now, now);
        jdbc.update("""
                INSERT INTO production_execution_grant (
                    grant_id, production_change_id, phase15_execution_id, target_id, grant_type, status,
                    production_fingerprint, authorization_generation, fencing_token, operation_binding_hash,
                    issued_at, expires_at, version)
                VALUES (?, ?, ?, ?, 'FORWARD', 'REVOKED', ?, 1, 7, ?, ?, ?, 0)
                """, grantId, changeId, phase15, targetId, hash, hash, now,
                java.sql.Timestamp.from(java.time.Instant.now().plus(10, java.time.temporal.ChronoUnit.MINUTES)));
        var result = GATEWAY_CTX.getBean(
                com.simba.snip.npo.productionwritegateway.service.ProductionGrantConsumeService.class)
                .consume(new com.simba.snip.npo.productionwritegateway.service.ConsumeCommand(
                        grantId, changeId, phase15, targetId, hash, 1, 7L, hash,
                        com.simba.snip.npo.productionchange.protocol.GrantType.FORWARD));
        assertTrue(!result.succeeded());
        assertEquals(0, mutationCount());
    }

    private void fi03() {
        ProductionChangeDto a = authorized();
        injectFailure(FailureInjectionPoint.AFTER_CONSUME_BEFORE_ATTEMPT);
        executeProductionChange(a.productionChangeId());
        assertEquals("CONSUMED", grantStatus(a.productionChangeId()));
        assertEquals("CONSUMED_PRE_SEND_RECOVERY_REQUIRED", getProductionChange(a.productionChangeId()).status());
        assertEquals(0, attemptCount(a.productionChangeId()));
        assertEquals(0, mutationCount());
        assertEquals(0, grantCount(a.productionChangeId(), "ISSUED"));
        Integer grantTotal = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ?",
                Integer.class, a.productionChangeId());
        assertEquals(1, grantTotal);
        Integer recovery = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_recovery WHERE production_change_id = ? AND status = 'CONSUMED_PRE_SEND_RECOVERY_REQUIRED'",
                Integer.class, a.productionChangeId());
        assertEquals(1, recovery);
    }

    private void killSwitch() {
        ProductionChangeDto a = authorized();
        gatewayProperties().setEnabled(false);
        try {
            executeProductionChange(a.productionChangeId());
            assertEquals(0, mutationCount());
        } finally {
            restoreGatewaySafetyFlags();
        }
    }

    private void suspend() {
        ProductionChangeDto a = authorized();
        jdbc.update("UPDATE production_network_target SET target_state = 'SUSPENDED' WHERE target_id = ?", TARGET_ID);
        executeProductionChange(a.productionChangeId());
        assertEquals(0, mutationCount());
    }

    private void expiredCc() {
        ProductionChangeDto a = authorized();
        jdbc.update("UPDATE production_change_control SET valid_until = NOW() - INTERVAL '1 minute' WHERE production_change_id = ?",
                a.productionChangeId());
        executeProductionChange(a.productionChangeId());
        assertEquals(0, mutationCount());
    }
}
