package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.service.ConsumeCommand;
import com.simba.snip.npo.productionwritegateway.service.ConsumeResult;
import com.simba.snip.npo.productionwritegateway.service.ProductionGrantConsumeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeGrantConsumeIT extends ProductionChangeITSupport {

    private static final String HASH_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Test
    void concurrentConsume_oneWinner() throws Exception {
        SeededGrant seeded = seedIssuedForwardGrant();
        ConsumeCommand command = commandFrom(seeded, GrantType.FORWARD);
        ProductionGrantConsumeService consumeService = consumeService();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ConsumeResult> first = pool.submit(() -> {
                start.await();
                return consumeService.consume(command);
            });
            Future<ConsumeResult> second = pool.submit(() -> {
                start.await();
                return consumeService.consume(command);
            });
            start.countDown();
            ConsumeResult a = first.get();
            ConsumeResult b = second.get();
            int successes = (a.succeeded() ? 1 : 0) + (b.succeeded() ? 1 : 0);
            assertEquals(1, successes);
            ConsumeResult loser = a.succeeded() ? b : a;
            assertFalse(loser.succeeded());
            assertEquals(0, loser.rowsUpdated());
            assertEquals(ProductionReasonCode.PRODUCTION_GRANT_ALREADY_CONSUMED, loser.denyReason());
            assertEquals("CONSUMED", grantStatusById(seeded.grantId));
            assertEquals(0, mutationCount());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void consumeDeny_wrongTarget() {
        SeededGrant seeded = seedIssuedForwardGrant();
        ConsumeResult result = consumeService().consume(new ConsumeCommand(
                seeded.grantId, seeded.changeId, seeded.phase15, "wrong-target",
                seeded.fingerprint, seeded.authGen, seeded.fencing, seeded.binding, GrantType.FORWARD));
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_wrongFingerprint() {
        SeededGrant seeded = seedIssuedForwardGrant();
        ConsumeResult result = consumeService().consume(new ConsumeCommand(
                seeded.grantId, seeded.changeId, seeded.phase15, seeded.targetId,
                HASH_B, seeded.authGen, seeded.fencing, seeded.binding, GrantType.FORWARD));
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_wrongAuthGeneration() {
        SeededGrant seeded = seedIssuedForwardGrant();
        ConsumeResult result = consumeService().consume(new ConsumeCommand(
                seeded.grantId, seeded.changeId, seeded.phase15, seeded.targetId,
                seeded.fingerprint, seeded.authGen + 9, seeded.fencing, seeded.binding, GrantType.FORWARD));
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_wrongFencingToken() {
        SeededGrant seeded = seedIssuedForwardGrant();
        ConsumeResult result = consumeService().consume(new ConsumeCommand(
                seeded.grantId, seeded.changeId, seeded.phase15, seeded.targetId,
                seeded.fingerprint, seeded.authGen, seeded.fencing + 99, seeded.binding, GrantType.FORWARD));
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_FENCING_MISMATCH);
    }

    @Test
    void consumeDeny_wrongOperationBinding() {
        SeededGrant seeded = seedIssuedForwardGrant();
        ConsumeResult result = consumeService().consume(new ConsumeCommand(
                seeded.grantId, seeded.changeId, seeded.phase15, seeded.targetId,
                seeded.fingerprint, seeded.authGen, seeded.fencing, HASH_B, GrantType.FORWARD));
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_expired() {
        SeededGrant seeded = seedGrant("ISSUED", "FORWARD", Instant.now().minus(1, ChronoUnit.MINUTES));
        ConsumeResult result = consumeService().consume(commandFrom(seeded, GrantType.FORWARD));
        assertFalse(result.succeeded());
        assertEquals(0, result.rowsUpdated());
        assertEquals(ProductionReasonCode.PRODUCTION_GRANT_EXPIRED, result.denyReason());
        assertEquals("ISSUED", grantStatusById(seeded.grantId));
        assertEquals(0, mutationCount());
    }

    @Test
    void consumeDeny_revoked() {
        SeededGrant seeded = seedGrant("REVOKED", "FORWARD", Instant.now().plus(10, ChronoUnit.MINUTES));
        ConsumeResult result = consumeService().consume(commandFrom(seeded, GrantType.FORWARD));
        assertFalse(result.succeeded());
        assertEquals(ProductionReasonCode.PRODUCTION_GRANT_REVOKED, result.denyReason());
        assertEquals("REVOKED", grantStatusById(seeded.grantId));
        assertEquals(0, mutationCount());
    }

    @Test
    void consumeDeny_wrongGrantType() {
        SeededGrant seeded = seedIssuedForwardGrant();
        ConsumeResult result = consumeService().consume(commandFrom(seeded, GrantType.ROLLBACK));
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_alreadyConsumed() {
        SeededGrant seeded = seedIssuedForwardGrant();
        assertTrue(consumeService().consume(commandFrom(seeded, GrantType.FORWARD)).succeeded());
        ConsumeResult second = consumeService().consume(commandFrom(seeded, GrantType.FORWARD));
        assertFalse(second.succeeded());
        assertEquals(ProductionReasonCode.PRODUCTION_GRANT_ALREADY_CONSUMED, second.denyReason());
        assertEquals("CONSUMED", grantStatusById(seeded.grantId));
        assertEquals(0, mutationCount());
    }

    @Test
    void noValidGrantGatewayDeny() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-SNIP-GATEWAY-CALLER-ID", "snip-npo-app");
        Map<String, String> payload = Map.of(
                "grantId", UUID.randomUUID().toString(),
                "productionChangeId", UUID.randomUUID().toString(),
                "correlationId", "missing");
        String body;
        try {
            ResponseEntity<String> response = new RestTemplate().postForEntity(
                    gatewayBaseUrl() + "/internal/v1/gateway/execute",
                    new HttpEntity<>(payload, headers),
                    String.class);
            body = String.valueOf(response.getBody());
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            body = ex.getResponseBodyAsString();
        }
        assertTrue(body.contains(ProductionReasonCode.PRODUCTION_GRANT_MISSING.name())
                || body.contains(ProductionReasonCode.PRODUCTION_GRANT_NOT_FOUND.name())
                || body.contains("GRANT"));
        assertEquals(0, mutationCount());
    }

    private ProductionGrantConsumeService consumeService() {
        return GATEWAY_CTX.getBean(ProductionGrantConsumeService.class);
    }

    private void assertDeniedIssued(SeededGrant seeded, ConsumeResult result, ProductionReasonCode expected) {
        assertFalse(result.succeeded());
        assertEquals(0, result.rowsUpdated());
        assertEquals(expected, result.denyReason());
        assertEquals("ISSUED", grantStatusById(seeded.grantId));
        assertEquals(0, mutationCount());
    }

    private ConsumeCommand commandFrom(SeededGrant seeded, GrantType grantType) {
        return new ConsumeCommand(
                seeded.grantId, seeded.changeId, seeded.phase15, seeded.targetId,
                seeded.fingerprint, seeded.authGen, seeded.fencing, seeded.binding, grantType);
    }

    private String grantStatusById(UUID grantId) {
        return jdbc.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?",
                String.class,
                grantId);
    }

    private SeededGrant seedIssuedForwardGrant() {
        return seedGrant("ISSUED", "FORWARD", Instant.now().plus(10, ChronoUnit.MINUTES));
    }

    private SeededGrant seedGrant(String grantStatus, String grantType, Instant expiresAt) {
        String targetId = "target-" + UUID.randomUUID();
        UUID changeId = UUID.randomUUID();
        UUID grantId = UUID.randomUUID();
        UUID phase15 = UUID.randomUUID();
        Instant now = Instant.now();
        String cellId = "cell-" + UUID.randomUUID();
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
                VALUES (?, ?, ?, 'CC-1', 'AUTHORIZED', ?, 1, ?, 'txPower',
                    10, 12, 12, 10, 'requester-1', 'VALID', ?, ?, 0)
                """, changeId, phase15, targetId, HASH_A, cellId, Timestamp.from(now), Timestamp.from(now));
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
                VALUES (?, ?, ?, 'txPower', 'gateway-test', 7, 'ACTIVE', ?, ?)
                """, UUID.randomUUID(), targetId, cellId, Timestamp.from(now), Timestamp.from(now.plus(30, ChronoUnit.MINUTES)));
        jdbc.update("""
                INSERT INTO production_execution_grant (
                    grant_id, production_change_id, phase15_execution_id, target_id, grant_type, status,
                    production_fingerprint, authorization_generation, fencing_token, operation_binding_hash,
                    issued_at, expires_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, 7, ?, ?, ?, 0)
                """, grantId, changeId, phase15, targetId, grantType, grantStatus, HASH_A, HASH_A,
                Timestamp.from(now), Timestamp.from(expiresAt));
        return new SeededGrant(grantId, changeId, phase15, targetId, HASH_A, 1, 7L, HASH_A);
    }

    private record SeededGrant(
            UUID grantId, UUID changeId, UUID phase15, String targetId,
            String fingerprint, int authGen, long fencing, String binding
    ) {
    }
}
