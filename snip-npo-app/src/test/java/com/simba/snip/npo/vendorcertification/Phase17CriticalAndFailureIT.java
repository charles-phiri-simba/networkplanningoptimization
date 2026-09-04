package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.ProductionChangeITSupport;
import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import com.simba.snip.npo.vendorcertification.api.VendorCertificationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Phase17CriticalAndFailureIT extends ProductionChangeITSupport {

    @AfterEach
    void cleanupPhase17() {
        Phase17GraphCleanup.deleteAll(jdbc);
    }

    @Test
    void cs17NAndFi17ResponseLossMutationExactlyOne() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RESPONSE_LOST);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
        Integer grants = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ? AND status = 'ISSUED'",
                Integer.class, change.productionChangeId());
        assertEquals(0, grants);
    }

    @Test
    void fi17PreSendDenyMutationZero() {
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_MISMATCH);
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(0, mutationCount());
    }

    @Test
    void cs17TAgentDenied() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(VendorCertificationController.ACTOR_HEADER, "agent:chief");
        headers.add(VendorCertificationController.PERMISSION_HEADER, "TRANSPORT_CERTIFY");
        ResponseEntity<Map> response = http.exchange(
                "/api/v1/transport-certifications/" + UUID.randomUUID() + "/transition",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(Map.of("from", "DRAFT", "to", "INTERFACE_VERIFIED"), headers),
                Map.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("P17_AGENT_DENIED", response.getBody().get("code"));
    }

    @Test
    void cs17UMcpDenied() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(VendorCertificationController.ACTOR_HEADER, "mcp:tool");
        headers.add(VendorCertificationController.PERMISSION_HEADER, "TRANSPORT_CERTIFY");
        ResponseEntity<Map> response = http.exchange(
                "/api/v1/transport-certifications/" + UUID.randomUUID() + "/transition",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(Map.of("from", "DRAFT", "to", "INTERFACE_VERIFIED"), headers),
                Map.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("P17_MCP_DENIED", response.getBody().get("code"));
    }

    @Test
    void killSwitchStillDeniesWithZeroMutation() {
        gatewayProperties().setEnabled(false);
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(0, mutationCount());
    }

    @Test
    void cs17ODesiredObservedVerifiedMutationOne() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RESPONSE_LOST);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
        assertEquals(0, grantCount(change.productionChangeId(), "ISSUED"));
    }

    @Test
    void cs17PExpectedObservedStopMutationOne() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RETURN_EXPECTED_AFTER_APPLY);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
        assertEquals(0, grantCount(change.productionChangeId(), "ISSUED"));
    }

    @Test
    void cs17QThirdValueManualMutationOne() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.THIRD_VALUE);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
    }

    @Test
    void cs17RReadbackUnavailableMutationOne() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.TIMEOUT_AFTER_APPLY);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
    }

    @Test
    void fi17_018TimeoutAfterDispatchMutationOneNoResend() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.TIMEOUT_AFTER_APPLY);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        int first = mutationCount();
        assertEquals(1, first);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
    }

    @Test
    void fi17_020DuplicateConsumeSecondMutationZero() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        executeExpectingOk(change.productionChangeId());
        assertEquals(1, mutationCount());
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
    }

    @Test
    void cs17SOnboardingExecutorEqualsCreatorDenied() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(com.simba.snip.npo.targetonboarding.api.TargetOnboardingController.ACTOR_HEADER, "creator-1");
        headers.add(com.simba.snip.npo.targetonboarding.api.TargetOnboardingController.PERMISSION_HEADER, "TARGET_ONBOARD_APPROVE");
        ResponseEntity<Map> response = http.exchange(
                "/api/v1/target-onboardings/" + UUID.randomUUID() + "/approve",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(Map.of(), headers),
                Map.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void c17i09ProductionClassL0BypassDeniedMutationZero() {
        String prodTarget = "ERICSSON-ENM-PHASE17-PROD";
        targetRegistry.register(new com.simba.snip.npo.productionchange.service.ProductionTargetRegistry.TargetRegistration(
                prodTarget, "ERICSSON", "ENM", "PRODUCTION", "test", "RAN",
                "ericsson-enm-write-l0-prod", "1", "security-l0", "credential-profile-ref-l0-prod",
                "CELL", "txPower", "MANUAL", "p16-rollback-v1", "p16-verification-v1",
                com.simba.snip.npo.productionchange.domain.CertificationLevel.L0, true,
                com.simba.snip.npo.productionchange.domain.ProductionTargetState.ACTIVE,
                com.simba.snip.npo.productionchange.domain.ExpectedStateGuardStrength.READ_THEN_WRITE));
        ensureTargetHealth(prodTarget);
        ProductionChangeDto change = createProductionChange(
                verifiedPhase15ExecutionId(), prodTarget, PRINCIPAL_CC_VALIDATOR,
                java.time.Instant.now().plus(2, java.time.temporal.ChronoUnit.HOURS));
        reviewProductionChange(change.productionChangeId());
        authorizeProductionChange(change.productionChangeId());
        seedTransportFor(change);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(0, mutationCount());
    }

    @Test
    void fi17_016KillSwitch() {
        gatewayProperties().setEnabled(false);
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(0, mutationCount());
    }

    @Test
    void c17i09SimulatorAndSandboxL0BypassAllowed() {
        executeL0Class("ERICSSON-ENM-PHASE17-SIM", "SIMULATOR");
        assertEquals(1, mutationCount());
        testTransport().reset();
        testTransport().seedCell(CELL, new java.math.BigDecimal(SEED_TX_POWER));
        executeL0Class("ERICSSON-ENM-PHASE17-SBX", "CONTROLLED_SANDBOX");
        assertEquals(1, mutationCount());
    }

    @Test
    void c17i09UnknownTargetClassDeniesL0Bypass() {
        executeL0Class("ERICSSON-ENM-PHASE17-UNK", "UNKNOWN");
        assertEquals(0, mutationCount());
    }

    @Test
    void fi17_019ConnectionLossAfterDispatchMutationOneNoResend() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.CONNECTION_LOST_AFTER_APPLY);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
    }

    private void executeL0Class(String targetId, String environment) {
        targetRegistry.register(new com.simba.snip.npo.productionchange.service.ProductionTargetRegistry.TargetRegistration(
                targetId, "ERICSSON", "ENM", environment, "test", "RAN",
                "ericsson-enm-write-l0-" + environment.toLowerCase(), "1", "security-l0",
                "credential-profile-ref-l0-" + environment.toLowerCase(),
                "CELL", "txPower", "MANUAL", "p16-rollback-v1", "p16-verification-v1",
                com.simba.snip.npo.productionchange.domain.CertificationLevel.L0, true,
                com.simba.snip.npo.productionchange.domain.ProductionTargetState.ACTIVE,
                com.simba.snip.npo.productionchange.domain.ExpectedStateGuardStrength.READ_THEN_WRITE));
        ensureTargetHealth(targetId);
        ProductionChangeDto change = createProductionChange(
                verifiedPhase15ExecutionId(), targetId, PRINCIPAL_CC_VALIDATOR,
                java.time.Instant.now().plus(2, java.time.temporal.ChronoUnit.HOURS));
        reviewProductionChange(change.productionChangeId());
        authorizeProductionChange(change.productionChangeId());
        seedTransportFor(change);
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
    }
}
