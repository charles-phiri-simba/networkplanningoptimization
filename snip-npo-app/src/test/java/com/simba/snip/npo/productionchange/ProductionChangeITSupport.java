package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.changeexecution.adapter.simulator.SimulatorExecutionAdapter;
import com.simba.snip.npo.changeexecution.api.AuthorizeExecutionRequest;
import com.simba.snip.npo.changeexecution.api.CreateExecutionRequest;
import com.simba.snip.npo.changeexecution.api.ExecutionDetailDto;
import com.simba.snip.npo.changeexecution.api.ReviewExecutionRequest;
import com.simba.snip.npo.changeexecution.security.ChangeExecutionAuthorizer;
import com.simba.snip.npo.changeintelligence.api.ChangeProposalDetailDto;
import com.simba.snip.npo.changeintelligence.api.GenerateChangeProposalRequest;
import com.simba.snip.npo.changeintelligence.api.ReviewChangeProposalRequest;
import com.simba.snip.npo.changeintelligence.authorization.ChangeProposalAuthorizer;
import com.simba.snip.npo.changeintelligence.model.GenerationInitiator;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeplanning.api.AuthorizeChangePlanRequest;
import com.simba.snip.npo.changeplanning.api.ChangePlanDetailDto;
import com.simba.snip.npo.changeplanning.api.CreateChangePlanRequest;
import com.simba.snip.npo.changeplanning.api.ReviewChangePlanRequest;
import com.simba.snip.npo.changeplanning.authorization.ChangePlanAuthorizer;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenario;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.SimulatorEnmSyncState;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.sync.SynchronizationControlPlane;
import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.security.ProductionChangeAuthorizer;
import com.simba.snip.npo.productionchange.service.ProductionTargetRegistry;
import com.simba.snip.npo.productionwritegateway.ProductionWriteGatewayApplication;
import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.service.FailureInjectionPoint;
import com.simba.snip.npo.productionwritegateway.service.ProductionGatewayFailureInjector;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.TelemetryProjectionService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ProductionChangeITSupport extends AbstractPostgresIT {

    public static final String CELL = "CELL-001";
    public static final String SEED_TX_POWER = "46";
    public static final String TARGET_ID = ProductionTargetRegistry.DEFAULT_L0_TARGET_ID;
    public static final String PHASE15_TARGET = SimulatorExecutionAdapter.TARGET_ID;

    public static final String PRINCIPAL_REQUESTER = "requester-1";
    public static final String PRINCIPAL_REVIEWER = "reviewer-1";
    public static final String PRINCIPAL_AUTHORIZER = "authorizer-1";
    public static final String PRINCIPAL_EXECUTOR = "executor-1";
    public static final String PRINCIPAL_CC_VALIDATOR = "cc-validator-1";

    protected static final ConfigurableApplicationContext GATEWAY_CTX;

    static {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        GATEWAY_CTX = SpringApplication.run(
                ProductionWriteGatewayApplication.class,
                "--server.port=0",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--spring.datasource.hikari.maximum-pool-size=4",
                "--spring.datasource.hikari.minimum-idle=0",
                "--spring.flyway.enabled=false",
                "--spring.jpa.hibernate.ddl-auto=none",
                "--spring.jpa.open-in-view=false",
                "--snip.production-change.enabled=true",
                "--snip.production-change.global-execution-enabled=true",
                "--snip.production-change.test-transport-enabled=true",
                "--snip.production-change.failure-injection.enabled=true",
                "--snip.integration.security.production-runtime=false",
                "--spring.profiles.active=test"
        );
    }

    @DynamicPropertySource
    static void enableProductionChange(DynamicPropertyRegistry registry) {
        registry.add("snip.change-execution.enabled", () -> "true");
        registry.add("snip.production-change.enabled", () -> "true");
        registry.add("snip.production-change.global-execution-enabled", () -> "true");
        registry.add("snip.production-change.gateway-base-url", ProductionChangeITSupport::gatewayBaseUrl);
        registry.add("snip.production-change.instance-id", () -> "snip-npo-app");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "15000");
    }

    @Autowired protected TestRestTemplate http;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected ProductionTargetRegistry targetRegistry;
    @Autowired protected TelemetryProjectionService projectionService;
    @Autowired protected AssuranceCaseService assuranceCaseService;
    @Autowired protected SynchronizationControlPlane controlPlane;
    @Autowired protected com.simba.snip.npo.integration.enm.VendorImportAuthorizer vendorImportAuthorizer;
    @Autowired protected SimulatorEnmScenarioController scenarios;
    @Autowired protected SimulatorEnmSyncState syncState;
    @Autowired protected SimulatorExecutionAdapter simulatorAdapter;

    protected UUID assuranceCaseId;

    protected static String gatewayBaseUrl() {
        return "http://127.0.0.1:" + GATEWAY_CTX.getEnvironment().getProperty("local.server.port");
    }

    protected static ConfigurableApplicationContext gatewayContext() {
        return GATEWAY_CTX;
    }

    protected ControlledTestEricssonWriteTransport testTransport() {
        return GATEWAY_CTX.getBean(ControlledTestEricssonWriteTransport.class);
    }

    protected AtomicInteger mutationCounter() {
        return GATEWAY_CTX.getBean("mutationInvocationCounter", AtomicInteger.class);
    }

    protected ProductionGatewayFailureInjector failureInjector() {
        return GATEWAY_CTX.getBean(ProductionGatewayFailureInjector.class);
    }

    protected ProductionChangeGatewayProperties gatewayProperties() {
        return GATEWAY_CTX.getBean(ProductionChangeGatewayProperties.class);
    }

    protected int mutationCount() {
        return mutationCounter().get();
    }

    @BeforeEach
    void productionChangeFixtures() {
        restoreGatewaySafetyFlags();
        failureInjector().setNextHook(null);
        testTransport().reset();
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.NONE);
        simulatorAdapter.clearFailureMode();
        seedTelemetry();
        runTrustedBaseline();
        assuranceCaseId = assuranceCaseService.listForCell(CELL).stream()
                .findFirst()
                .map(c -> c.getId())
                .orElse(null);
        http.postForEntity("/api/v1/twins/cells/" + CELL + "/synchronize", null, Map.class);
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_ID));
        ensureTargetHealth(TARGET_ID);
        testTransport().seedCell(CELL, new BigDecimal(SEED_TX_POWER));
    }

    @AfterEach
    void productionChangeCleanup() {
        restoreGatewaySafetyFlags();
        failureInjector().setNextHook(null);
        testTransport().reset();
        simulatorAdapter.clearFailureMode();
        cleanupPhase16();
        cleanupPhase15();
        cleanupPhase14();
        cleanupPhase13();
        jdbc.update("DELETE FROM network_drift_observation WHERE summary = 'phase14-test-drift'");
        restoreSharedPriorPhaseState();
    }

    protected void restoreGatewaySafetyFlags() {
        ProductionChangeGatewayProperties properties = gatewayProperties();
        properties.setEnabled(true);
        properties.setGlobalExecutionEnabled(true);
        properties.getFailureInjection().setEnabled(true);
        properties.getFailureInjection().setHook("");
        properties.getSsl().setHostnameVerification(true);
        properties.getSsl().setTrustAll(false);
    }

    protected void seedTransportFor(ProductionChangeDto change) {
        testTransport().seedCell(change.cellId(), change.expectedValue());
    }

    protected UUID verifiedPhase15ExecutionId() {
        jdbc.update("DELETE FROM simulator_execution_cell_state");
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createPhase15Execution(planId);
        reviewPhase15(created.executionId());
        authorizePhase15(created.executionId());
        ExecutionDetailDto executed = executePhase15(created.executionId());
        assertEquals("VERIFIED", executed.status());
        return executed.executionId();
    }

    protected ProductionChangeDto createProductionChange(UUID phase15ExecutionId) {
        return createProductionChange(phase15ExecutionId, TARGET_ID, PRINCIPAL_CC_VALIDATOR, Instant.now().plus(2, ChronoUnit.HOURS));
    }

    protected ProductionChangeDto createProductionChange(
            UUID phase15ExecutionId,
            String targetId,
            String validatedByPrincipalId,
            Instant validUntil
    ) {
        Map<String, Object> body = createRequestBody(phase15ExecutionId, targetId, validatedByPrincipalId, validUntil);
        ResponseEntity<ProductionChangeDto> response = http.exchange(
                "/api/v1/production-changes",
                HttpMethod.POST,
                productionEntity(body, ProductionChangePermission.REQUEST_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                ProductionChangeDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        assertNotNull(response.getBody());
        return response.getBody();
    }

    protected Map<String, Object> createRequestBody(
            UUID phase15ExecutionId,
            String targetId,
            String validatedByPrincipalId,
            Instant validUntil
    ) {
        Map<String, Object> cc = new LinkedHashMap<>();
        cc.put("system", "MANUAL");
        cc.put("reference", "CC-" + UUID.randomUUID());
        cc.put("status", "VALID");
        cc.put("validatedByPrincipalId", validatedByPrincipalId);
        cc.put("validatedAt", Instant.now().toString());
        cc.put("validUntil", validUntil.toString());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phase15ExecutionId", phase15ExecutionId.toString());
        body.put("productionTargetId", targetId);
        body.put("changeControlReference", cc);
        return body;
    }

    protected ProductionChangeDto reviewProductionChange(UUID id) {
        Map<String, Object> body = Map.of("decision", "APPROVED", "reasonCodes", List.of());
        ResponseEntity<ProductionChangeDto> response = http.exchange(
                "/api/v1/production-changes/" + id + "/review",
                HttpMethod.POST,
                productionEntity(body, ProductionChangePermission.REVIEW_PRODUCTION_CHANGE, PRINCIPAL_REVIEWER),
                ProductionChangeDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        return response.getBody();
    }

    protected ProductionChangeDto authorizeProductionChange(UUID id) {
        return authorizeProductionChange(id, PRINCIPAL_AUTHORIZER);
    }

    protected ProductionChangeDto authorizeProductionChange(UUID id, String authorizerPrincipalId) {
        ResponseEntity<ProductionChangeDto> response = http.exchange(
                "/api/v1/production-changes/" + id + "/authorize",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.AUTHORIZE_PRODUCTION_CHANGE, authorizerPrincipalId),
                ProductionChangeDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        return response.getBody();
    }

    protected ResponseEntity<ProductionChangeDto> executeProductionChange(UUID id) {
        return executeProductionChange(id, PRINCIPAL_EXECUTOR, Map.of());
    }

    protected ResponseEntity<ProductionChangeDto> executeProductionChange(
            UUID id,
            String executorPrincipalId,
            Object body
    ) {
        return http.exchange(
                "/api/v1/production-changes/" + id + "/execute",
                HttpMethod.POST,
                productionEntity(body, ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, executorPrincipalId),
                ProductionChangeDto.class);
    }

    protected ProductionChangeDto executeExpectingOk(UUID id) {
        ResponseEntity<ProductionChangeDto> response = executeProductionChange(id);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        return response.getBody();
    }

    protected ProductionChangeDto getProductionChange(UUID id) {
        ResponseEntity<ProductionChangeDto> response = http.exchange(
                "/api/v1/production-changes/" + id,
                HttpMethod.GET,
                productionEntity(null, ProductionChangePermission.VIEW_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                ProductionChangeDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    protected ProductionChangeDto reviewedAndAuthorized(UUID phase15ExecutionId) {
        ProductionChangeDto created = createProductionChange(phase15ExecutionId);
        reviewProductionChange(created.productionChangeId());
        ProductionChangeDto authorized = authorizeProductionChange(created.productionChangeId());
        seedTransportFor(authorized);
        return authorized;
    }

    protected ProductionChangeDto rollbackRequest(UUID id) {
        ResponseEntity<ProductionChangeDto> response = http.exchange(
                "/api/v1/production-changes/" + id + "/rollback/request",
                HttpMethod.POST,
                productionEntity(null, ProductionChangePermission.REQUEST_PRODUCTION_ROLLBACK, PRINCIPAL_REQUESTER),
                ProductionChangeDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        return response.getBody();
    }

    protected ProductionChangeDto rollbackReview(UUID id) {
        Map<String, Object> body = Map.of("decision", "APPROVED", "reasonCodes", List.of());
        ResponseEntity<ProductionChangeDto> response = http.exchange(
                "/api/v1/production-changes/" + id + "/rollback/review",
                HttpMethod.POST,
                productionEntity(body, ProductionChangePermission.REVIEW_PRODUCTION_ROLLBACK, PRINCIPAL_REVIEWER),
                ProductionChangeDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        return response.getBody();
    }

    protected ProductionChangeDto rollbackAuthorize(UUID id) {
        ResponseEntity<ProductionChangeDto> response = http.exchange(
                "/api/v1/production-changes/" + id + "/rollback/authorize",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.AUTHORIZE_PRODUCTION_ROLLBACK, PRINCIPAL_AUTHORIZER),
                ProductionChangeDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        return response.getBody();
    }

    protected ResponseEntity<ProductionChangeDto> rollbackExecute(UUID id) {
        return http.exchange(
                "/api/v1/production-changes/" + id + "/rollback/execute",
                HttpMethod.POST,
                productionEntity(null, ProductionChangePermission.EXECUTE_PRODUCTION_ROLLBACK, PRINCIPAL_EXECUTOR),
                ProductionChangeDto.class);
    }

    protected String grantStatus(UUID productionChangeId) {
        List<String> statuses = jdbc.queryForList(
                "SELECT status FROM production_execution_grant WHERE production_change_id = ? ORDER BY issued_at DESC",
                String.class,
                productionChangeId);
        return statuses.isEmpty() ? null : statuses.get(0);
    }

    protected int grantCount(UUID productionChangeId, String status) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ? AND status = ?",
                Integer.class,
                productionChangeId,
                status);
        return count == null ? 0 : count;
    }

    protected int attemptCount(UUID productionChangeId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_gateway_attempt WHERE production_change_id = ?",
                Integer.class,
                productionChangeId);
        return count == null ? 0 : count;
    }

    protected String latestAttemptStatus(UUID productionChangeId) {
        List<String> statuses = jdbc.queryForList(
                "SELECT status FROM production_gateway_attempt WHERE production_change_id = ? ORDER BY started_at DESC",
                String.class,
                productionChangeId);
        return statuses.isEmpty() ? null : statuses.get(0);
    }

    protected String canonicalTxPower() {
        return jdbc.queryForObject(
                "SELECT parameter_value FROM radio_configuration rc JOIN cell c ON rc.cell_id = c.id WHERE c.cell_id = ? AND rc.parameter_name = 'txPower'",
                String.class,
                CELL);
    }

    protected int issuedGrantCount() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE status = 'ISSUED'",
                Integer.class);
        return count == null ? 0 : count;
    }

    protected void injectFailure(FailureInjectionPoint point) {
        gatewayProperties().getFailureInjection().setEnabled(true);
        failureInjector().setNextHook(point);
    }

    protected <T> HttpEntity<T> productionEntity(T body, String permission, String actorPrincipalId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ProductionChangeAuthorizer.HEADER, permission);
        headers.set(ProductionChangeAuthorizer.ACTOR_HEADER, actorPrincipalId);
        return new HttpEntity<>(body, headers);
    }

    protected UUID readyPlanId() {
        setKnowledge("HIGH");
        UUID proposalId = approveProposal(generateProposal());
        ChangePlanDetailDto plan = createPlan(proposalId);
        reviewPlan(plan.plan().id());
        authorizePlan(plan.plan().id());
        evaluateReadiness(plan.plan().id());
        ChangePlanDetailDto ready = getPlan(plan.plan().id());
        assertEquals(PlanStatus.READY_FOR_EXECUTION.name(), ready.plan().status());
        return ready.plan().id();
    }

    protected ExecutionDetailDto createPhase15Execution(UUID planId) {
        ResponseEntity<ExecutionDetailDto> response = http.exchange(
                "/api/v1/change-execution/executions",
                HttpMethod.POST,
                execEntity(new CreateExecutionRequest(planId, PHASE15_TARGET), ChangeExecutionAuthorizer.PERMISSION_REQUEST),
                ExecutionDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    protected void reviewPhase15(UUID executionId) {
        http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/review",
                HttpMethod.POST,
                execEntity(new ReviewExecutionRequest("reviewer", "ok"), ChangeExecutionAuthorizer.PERMISSION_REVIEW),
                ExecutionDetailDto.class);
    }

    protected void authorizePhase15(UUID executionId) {
        http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/authorize",
                HttpMethod.POST,
                execEntity(new AuthorizeExecutionRequest("authorizer"), ChangeExecutionAuthorizer.PERMISSION_AUTHORIZE),
                ExecutionDetailDto.class);
    }

    protected ExecutionDetailDto executePhase15(UUID executionId) {
        ResponseEntity<ExecutionDetailDto> response = http.exchange(
                "/api/v1/change-execution/executions/" + executionId + "/execute",
                HttpMethod.POST,
                execEntity(null, ChangeExecutionAuthorizer.PERMISSION_AUTHORIZE),
                ExecutionDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        return response.getBody();
    }

    protected ChangeProposalDetailDto generateProposal() {
        ResponseEntity<ChangeProposalDetailDto> response = http.exchange(
                "/api/v1/change-intelligence/proposals",
                HttpMethod.POST,
                proposalEntity(new GenerateChangeProposalRequest("CELL", CELL, "txPower", assuranceCaseId, null,
                        GenerationInitiator.MANUAL, "generator"), ChangeProposalAuthorizer.PERMISSION_GENERATE),
                ChangeProposalDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    protected UUID approveProposal(ChangeProposalDetailDto recommended) {
        assertEquals(ProposalStatus.RECOMMENDED.name(), recommended.proposal().status());
        http.exchange(
                "/api/v1/change-intelligence/proposals/" + recommended.proposal().id() + "/approve",
                HttpMethod.POST,
                proposalEntity(new ReviewChangeProposalRequest("approver", null, "approved"), ChangeProposalAuthorizer.PERMISSION_APPROVE),
                ChangeProposalDetailDto.class);
        return recommended.proposal().id();
    }

    protected ChangePlanDetailDto createPlan(UUID proposalId) {
        ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                "/api/v1/change-planning/plans",
                HttpMethod.POST,
                planEntity(new CreateChangePlanRequest(proposalId), ChangePlanAuthorizer.PERMISSION_CREATE),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    protected void reviewPlan(UUID planId) {
        http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/review",
                HttpMethod.POST,
                planEntity(new ReviewChangePlanRequest("reviewer", "reviewed"), ChangePlanAuthorizer.PERMISSION_REVIEW),
                ChangePlanDetailDto.class);
    }

    protected void authorizePlan(UUID planId) {
        http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/authorize",
                HttpMethod.POST,
                planEntity(new AuthorizeChangePlanRequest("authorizer"), ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                ChangePlanDetailDto.class);
    }

    protected void evaluateReadiness(UUID planId) {
        http.exchange(
                "/api/v1/change-planning/plans/" + planId + "/readiness",
                HttpMethod.POST,
                planEntity(null, ChangePlanAuthorizer.PERMISSION_AUTHORIZE),
                ChangePlanDetailDto.class);
    }

    protected ChangePlanDetailDto getPlan(UUID planId) {
        ResponseEntity<ChangePlanDetailDto> response = http.exchange(
                "/api/v1/change-planning/plans/" + planId,
                HttpMethod.GET,
                planEntity(null, ChangePlanAuthorizer.PERMISSION_VIEW),
                ChangePlanDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    protected void cleanupPhase16() {
        jdbc.update("DELETE FROM production_gateway_evidence");
        jdbc.update("DELETE FROM production_execution_verification");
        jdbc.update("DELETE FROM production_execution_recovery");
        jdbc.update("DELETE FROM production_gateway_attempt");
        jdbc.update("DELETE FROM production_execution_grant");
        jdbc.update("DELETE FROM production_execution_rollback");
        jdbc.update("DELETE FROM production_change_audit_event");
        jdbc.update("DELETE FROM production_change_authorization");
        jdbc.update("DELETE FROM production_change_review");
        jdbc.update("DELETE FROM production_change_control");
        jdbc.update("DELETE FROM production_execution_lease");
        jdbc.update("DELETE FROM production_rate_limit_state");
        jdbc.update("DELETE FROM production_target_health");
        jdbc.update("DELETE FROM production_network_change");
        jdbc.update("DELETE FROM production_network_target");
    }

    protected void cleanupPhase15() {
        jdbc.update("DELETE FROM network_change_execution_lease");
        jdbc.update("DELETE FROM network_change_execution_audit_event");
        jdbc.update("DELETE FROM network_change_execution_verification");
        jdbc.update("DELETE FROM network_change_execution_recovery");
        jdbc.update("DELETE FROM network_change_execution_attempt");
        jdbc.update("DELETE FROM network_change_execution_authorization");
        jdbc.update("DELETE FROM network_change_execution_rollback");
        jdbc.update("DELETE FROM network_change_execution_operation");
        jdbc.update("DELETE FROM simulator_execution_cell_state");
        jdbc.update("DELETE FROM network_change_execution");
    }

    protected void cleanupPhase14() {
        jdbc.update("DELETE FROM network_change_plan_audit_event");
        jdbc.update("DELETE FROM network_change_plan_readiness_assessment");
        jdbc.update("DELETE FROM network_change_plan_review");
        jdbc.update("DELETE FROM network_change_plan_precondition");
        jdbc.update("DELETE FROM network_change_plan_operation_dependency");
        jdbc.update("DELETE FROM network_change_plan_rollback_operation");
        jdbc.update("DELETE FROM network_change_plan_operation");
        jdbc.update("DELETE FROM network_change_plan");
    }

    protected void cleanupPhase13() {
        jdbc.update("DELETE FROM change_proposal_audit_event");
        jdbc.update("DELETE FROM change_proposal_review");
        jdbc.update("DELETE FROM network_change_candidate");
        jdbc.update("DELETE FROM network_change_proposal");
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p15-%'");
    }

    protected void seedTelemetry() {
        Instant t0 = Instant.now().minusSeconds(3_600);
        double[] bler = {0.04, 0.06, 0.09, 0.12};
        double[] prb = {0.60, 0.68, 0.77, 0.84};
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            String prefix = "p15-" + UUID.randomUUID();
            projectionService.project(event(prefix + "-bler", CELL, "BLER_DL", bler[i], ts));
            projectionService.project(event(prefix + "-prb", CELL, "PRB_UTILIZATION_DL", prb[i], ts));
        }
    }

    protected void runTrustedBaseline() {
        syncState.resetAll();
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        jdbc.update("DELETE FROM network_import_lease");
        jdbc.update("UPDATE synchronization_checkpoint SET status = 'VALID' WHERE status = 'RECOVERY_REQUIRED'");
        jdbc.update("""
                UPDATE synchronization_source_state
                SET consecutive_failures = 0
                WHERE source_system = 'ERICSSON_ENM_SIMULATOR'
                """);
        vendorImportAuthorizer.runWith(com.simba.snip.npo.integration.enm.VendorImportAuthorizer.PERMISSION, () ->
                controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER));
    }

    protected void setKnowledge(String confidence) {
        jdbc.update(
                "UPDATE network_knowledge_status SET confidence = ?, reason_codes = ?, freshness = 'FRESH', source_health = 'HEALTHY'",
                confidence, confidence.equals("HIGH") ? "TRUSTED_BASELINE" : "DEGRADED");
    }

    protected void restoreSharedPriorPhaseState() {
        jdbc.update(
                "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                SEED_TX_POWER, CELL);
        syncState.resetAll();
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        jdbc.update("DELETE FROM network_import_lease");
        jdbc.update("UPDATE synchronization_checkpoint SET status = 'VALID' WHERE status = 'RECOVERY_REQUIRED'");
        jdbc.update("""
                UPDATE network_knowledge_status
                SET confidence = 'HIGH', reason_codes = 'TRUSTED_BASELINE', freshness = 'FRESH', source_health = 'HEALTHY'
                """);
        jdbc.update("""
                UPDATE synchronization_source_state
                SET consecutive_failures = 0
                WHERE source_system = 'ERICSSON_ENM_SIMULATOR'
                """);
    }

    protected void ensureTargetHealth(String targetId) {
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_target_health WHERE production_target_id = ?",
                Integer.class,
                targetId);
        if (existing != null && existing > 0) {
            return;
        }
        jdbc.update("""
                INSERT INTO production_target_health (
                    health_id, production_target_id, health_state, outcome_unknown_count,
                    verification_failure_count, last_checked_at)
                VALUES (?, ?, 'HEALTHY', 0, 0, NOW())
                """, UUID.randomUUID(), targetId);
    }

    private <T> HttpEntity<T> execEntity(T body, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ChangeExecutionAuthorizer.HEADER, permission);
        return new HttpEntity<>(body, headers);
    }

    private <T> HttpEntity<T> planEntity(T body, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ChangePlanAuthorizer.HEADER, permission);
        return new HttpEntity<>(body, headers);
    }

    private <T> HttpEntity<T> proposalEntity(T body, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ChangeProposalAuthorizer.HEADER, permission);
        return new HttpEntity<>(body, headers);
    }

    private static TelemetryEvent event(String eventId, String cellId, String metric, double value, Instant eventTime) {
        return new TelemetryEvent(
                eventId,
                TelemetryEvent.TYPE_CELL_KPI_OBSERVED,
                TelemetryEvent.SCHEMA_V1,
                TelemetryEvent.SOURCE_SIMULATOR,
                cellId,
                metric,
                value,
                "ratio",
                eventTime,
                null,
                true
        );
    }
}
