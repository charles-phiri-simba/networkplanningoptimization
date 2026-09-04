package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.AuthorizedParameterMutation;
import com.simba.snip.npo.productionchange.protocol.GatewayAttemptStatus;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteRequest;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteResponse;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionExecutionContext;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.protocol.SendPhase;
import com.simba.snip.npo.productionwritegateway.adapter.EricssonEnmWriteAdapter;
import com.simba.snip.npo.productionwritegateway.adapter.MutationResult;
import com.simba.snip.npo.productionwritegateway.adapter.PostMutationObservation;
import com.simba.snip.npo.productionwritegateway.adapter.VendorMutationResult;
import com.simba.snip.npo.productionwritegateway.audit.ProductionGatewayAuditService;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionRecoveryEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionGatewayAttemptEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import com.simba.snip.npo.productionwritegateway.exception.GatewayFailureInjectionException;
import com.simba.snip.npo.productionwritegateway.metrics.ProductionGatewayMetrics;
import com.simba.snip.npo.productionwritegateway.repository.ProductionExecutionGrantRepository;
import com.simba.snip.npo.productionwritegateway.repository.ProductionExecutionRecoveryRepository;
import com.simba.snip.npo.productionwritegateway.repository.ProductionNetworkChangeRepository;
import com.simba.snip.npo.productionwritegateway.security.WriteCredentialHandle;
import com.simba.snip.npo.productionwritegateway.vendortransport.CertificationSendBoundaryPreflight;
import com.simba.snip.npo.productionwritegateway.vendortransport.Phase17SendDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GatewayExecutionOrchestrator {

    private final GatewayAdmissionService admissionService;
    private final ProductionGrantConsumeService consumeService;
    private final ProductionGatewayAttemptService attemptService;
    private final ProductionGatewayEvidenceService evidenceService;
    private final ProductionGatewayPreflightService preflightService;
    private final ExpectedStateObservationService observationService;
    private final ProductionVerificationService verificationService;
    private final ProductionCredentialResolutionService credentialService;
    private final ProductionKillSwitchEnforcementService killSwitch;
    private final ProductionGatewayAuditService auditService;
    private final ProductionGatewayFailureInjector failureInjector;
    private final ProductionExecutionGrantRepository grantRepository;
    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionExecutionRecoveryRepository recoveryRepository;
    private final ProductionTargetHealthService healthService;
    private final ProductionGatewayMetrics metrics;
    private final EricssonEnmWriteAdapter writeAdapter;
    private final CertificationSendBoundaryPreflight phase17Preflight;
    private final AtomicInteger mutationInvocationCounter;

    public GatewayExecutionOrchestrator(
            GatewayAdmissionService admissionService,
            ProductionGrantConsumeService consumeService,
            ProductionGatewayAttemptService attemptService,
            ProductionGatewayEvidenceService evidenceService,
            ProductionGatewayPreflightService preflightService,
            ExpectedStateObservationService observationService,
            ProductionVerificationService verificationService,
            ProductionCredentialResolutionService credentialService,
            ProductionKillSwitchEnforcementService killSwitch,
            ProductionGatewayAuditService auditService,
            ProductionGatewayFailureInjector failureInjector,
            ProductionExecutionGrantRepository grantRepository,
            ProductionNetworkChangeRepository changeRepository,
            ProductionExecutionRecoveryRepository recoveryRepository,
            ProductionTargetHealthService healthService,
            ProductionGatewayMetrics metrics,
            EricssonEnmWriteAdapter writeAdapter,
            CertificationSendBoundaryPreflight phase17Preflight,
            org.springframework.beans.factory.ObjectProvider<AtomicInteger> mutationCounterProvider
    ) {
        this.admissionService = admissionService;
        this.consumeService = consumeService;
        this.attemptService = attemptService;
        this.evidenceService = evidenceService;
        this.preflightService = preflightService;
        this.observationService = observationService;
        this.verificationService = verificationService;
        this.credentialService = credentialService;
        this.killSwitch = killSwitch;
        this.auditService = auditService;
        this.failureInjector = failureInjector;
        this.grantRepository = grantRepository;
        this.changeRepository = changeRepository;
        this.recoveryRepository = recoveryRepository;
        this.healthService = healthService;
        this.metrics = metrics;
        this.writeAdapter = writeAdapter;
        this.phase17Preflight = phase17Preflight;
        this.mutationInvocationCounter = mutationCounterProvider.getIfAvailable(() -> new AtomicInteger(0));
    }

    public GatewayExecuteResponse execute(
            GatewayExecuteRequest request,
            String callerId,
            String authorizationHeader,
            GrantType grantType
    ) {
        if (request == null || request.grantId() == null || request.productionChangeId() == null) {
            throw GatewayDeniedException.deny(ProductionReasonCode.PRODUCTION_INVALID_REQUEST, null, null);
        }
        FailureInjectionPoint beforeLookup = grantType == GrantType.ROLLBACK
                ? FailureInjectionPoint.RB_BEFORE_GRANT_LOOKUP
                : FailureInjectionPoint.BEFORE_GRANT_LOOKUP;
        FailureInjectionPoint beforeConsume = grantType == GrantType.ROLLBACK
                ? FailureInjectionPoint.RB_BEFORE_CONSUME
                : FailureInjectionPoint.BEFORE_CONSUME;
        FailureInjectionPoint afterConsume = grantType == GrantType.ROLLBACK
                ? FailureInjectionPoint.RB_AFTER_CONSUME_BEFORE_ATTEMPT
                : FailureInjectionPoint.AFTER_CONSUME_BEFORE_ATTEMPT;

        admissionService.authenticate(callerId, authorizationHeader);
        failureInjector.inject(beforeLookup);
        ProductionExecutionGrantEntity grant = admissionService.loadGrant(request.grantId());
        admissionService.validateBindings(grant, request.productionChangeId(), grantType);

        failureInjector.inject(beforeConsume);
        // Consume binds the grant row to itself (anti-tamper). Live currentness
        // vs change/target/lease/control/window/kill-switch/profiles is preflight.
        ConsumeCommand command = new ConsumeCommand(
                grant.getGrantId(),
                grant.getProductionChangeId(),
                grant.getPhase15ExecutionId(),
                grant.getTargetId(),
                grant.getProductionFingerprint(),
                grant.getAuthorizationGeneration(),
                grant.getFencingToken(),
                grant.getOperationBindingHash(),
                grantType
        );
        ConsumeResult consumeResult = consumeService.consume(command);
        if (!consumeResult.succeeded()) {
            auditService.append(
                    request.productionChangeId(),
                    "PRODUCTION_GRANT_CONSUME_DENIED",
                    callerId,
                    List.of(consumeResult.denyReason().name()),
                    Map.of("grantId", request.grantId().toString())
            );
            throw GatewayDeniedException.deny(
                    consumeResult.denyReason(), request.grantId(), request.productionChangeId());
        }
        auditService.append(
                request.productionChangeId(),
                "PRODUCTION_GRANT_CONSUME_SUCCEEDED",
                callerId,
                List.of(),
                Map.of("grantId", request.grantId().toString())
        );

        try {
            failureInjector.inject(afterConsume);
        } catch (GatewayFailureInjectionException ex) {
            return handleConsumedPreSendRecovery(request, callerId, grantType);
        }

        ProductionExecutionGrantEntity consumed = grantRepository.findById(grant.getGrantId()).orElseThrow();
        ProductionGatewayAttemptEntity attempt;
        try {
            attempt = attemptService.insertPreSend(consumed);
        } catch (RuntimeException ex) {
            return handleConsumedPreSendRecovery(request, callerId, grantType);
        }
        auditService.append(
                request.productionChangeId(),
                "PRODUCTION_GATEWAY_ATTEMPT_PRE_SEND",
                callerId,
                List.of(),
                Map.of("attemptId", attempt.getAttemptId().toString())
        );

        WriteCredentialHandle handle = null;
        try {
            failureInjector.inject(grantType == GrantType.ROLLBACK
                    ? FailureInjectionPoint.RB_AFTER_ATTEMPT_BEFORE_PREFLIGHT
                    : FailureInjectionPoint.AFTER_ATTEMPT_BEFORE_PREFLIGHT);
            ProductionGatewayPreflightService.PreflightSnapshot snapshot =
                    preflightService.run(consumed, grantType, attempt.getAttemptId());
            try {
                phase17Preflight.evaluate(
                        consumed,
                        snapshot.change(),
                        snapshot.target()
                );
            } catch (Phase17SendDeniedException ex) {
                throw GatewayDeniedException.denyPhase17(
                        ex.denialCode(), grant.getGrantId(), grant.getProductionChangeId());
            }
            failureInjector.inject(grantType == GrantType.ROLLBACK
                    ? FailureInjectionPoint.RB_BEFORE_CREDENTIAL
                    : FailureInjectionPoint.BEFORE_CREDENTIAL);
            handle = credentialService.resolveAfterPreflight(
                    snapshot.target().getCredentialProfileId(),
                    grant.getGrantId(),
                    grant.getProductionChangeId()
            );
            failureInjector.inject(grantType == GrantType.ROLLBACK
                    ? FailureInjectionPoint.RB_BEFORE_OBSERVATION
                    : FailureInjectionPoint.BEFORE_OBSERVATION);
            PostMutationObservation expected = observationService.observeExpected(snapshot.change(), grantType);
            evidenceService.persist(
                    attempt.getAttemptId(),
                    "EXPECTED_STATE",
                    "{\"status\":\"" + expected.status().name() + "\"}"
            );
            observationService.requireMatch(expected, grant.getGrantId(), grant.getProductionChangeId());

            attemptService.updateStatus(
                    attempt.getAttemptId(),
                    GatewayAttemptStatus.SEND_ELIGIBLE,
                    SendPhase.PRE_SEND,
                    MutationOutcome.NOT_SENT
            );

            failureInjector.inject(grantType == GrantType.ROLLBACK
                    ? FailureInjectionPoint.RB_BEFORE_MUTATION
                    : FailureInjectionPoint.BEFORE_MUTATION);
            killSwitch.assertEnabled(grant.getGrantId(), grant.getProductionChangeId());

            attemptService.updateStatus(
                    attempt.getAttemptId(),
                    GatewayAttemptStatus.MAY_HAVE_SENT,
                    SendPhase.MAY_HAVE_SENT,
                    MutationOutcome.OUTCOME_UNKNOWN
            );
            failureInjector.inject(grantType == GrantType.ROLLBACK
                    ? FailureInjectionPoint.MUTATION_INVOKE_START
                    : FailureInjectionPoint.MUTATION_INVOKE_START);
            failureInjector.inject(grantType == GrantType.ROLLBACK
                    ? FailureInjectionPoint.RB_MUTATION_INVOKE_START
                    : FailureInjectionPoint.MUTATION_INVOKE_START);

            VendorMutationResult vendorResult = invokeMutation(snapshot, attempt.getAttemptId());
            persistMutationOutcome(attempt.getAttemptId(), snapshot.change(), vendorResult, callerId);

            if (vendorResult.outcome() == MutationOutcome.REJECTED) {
                attemptService.updateStatus(
                        attempt.getAttemptId(),
                        GatewayAttemptStatus.VENDOR_REJECTED,
                        SendPhase.PRE_SEND,
                        MutationOutcome.REJECTED
                );
                return response(
                        snapshot.change().getProductionChangeId(),
                        grant.getGrantId(),
                        attempt.getAttemptId(),
                        snapshot.change().getStatus(),
                        GatewayAttemptStatus.VENDOR_REJECTED,
                        MutationOutcome.REJECTED,
                        vendorResult.reasonCode() == null
                                ? ProductionReasonCode.PRODUCTION_VENDOR_REJECTION.name()
                                : vendorResult.reasonCode().name()
                );
            }

            if (vendorResult.outcome() == MutationOutcome.VENDOR_ACCEPTED) {
                attemptService.updateStatus(
                        attempt.getAttemptId(),
                        GatewayAttemptStatus.VENDOR_ACCEPTED,
                        SendPhase.MAY_HAVE_SENT,
                        MutationOutcome.VENDOR_ACCEPTED
                );
            } else {
                attemptService.updateStatus(
                        attempt.getAttemptId(),
                        GatewayAttemptStatus.OUTCOME_UNKNOWN,
                        SendPhase.MAY_HAVE_SENT,
                        MutationOutcome.OUTCOME_UNKNOWN
                );
                healthService.recordOutcomeUnknown(snapshot.target().getTargetId());
                metrics.incrementOutcomeUnknown();
            }

            attemptService.updateStatus(
                    attempt.getAttemptId(),
                    GatewayAttemptStatus.VERIFYING,
                    SendPhase.MAY_HAVE_SENT,
                    vendorResult.outcome()
            );
            failureInjector.inject(FailureInjectionPoint.BEFORE_VERIFICATION);
            ProductionVerificationService.VerificationDecision decision =
                    verificationService.verify(snapshot.change(), grantType);
            failureInjector.inject(FailureInjectionPoint.AFTER_VERIFICATION_BEFORE_PERSIST);
            BigDecimal desired = grantType == GrantType.ROLLBACK
                    ? snapshot.change().getRollbackDesiredValue()
                    : snapshot.change().getDesiredValue();
            verificationService.persist(
                    snapshot.change().getProductionChangeId(),
                    attempt.getAttemptId(),
                    decision,
                    desired
            );
            evidenceService.persist(
                    attempt.getAttemptId(),
                    "VERIFICATION",
                    "{\"status\":\"" + decision.attemptStatus().name() + "\"}"
            );
            attemptService.updateStatus(
                    attempt.getAttemptId(),
                    decision.attemptStatus(),
                    SendPhase.MAY_HAVE_SENT,
                    vendorResult.outcome() == MutationOutcome.VENDOR_ACCEPTED
                            ? MutationOutcome.VENDOR_ACCEPTED
                            : MutationOutcome.OUTCOME_UNKNOWN
            );
            if (decision.attemptStatus() == GatewayAttemptStatus.VERIFIED) {
                metrics.incrementVerified();
                auditService.append(
                        request.productionChangeId(),
                        "PRODUCTION_VERIFIED",
                        callerId,
                        List.of(),
                        Map.of("attemptId", attempt.getAttemptId().toString())
                );
            } else if (decision.attemptStatus() == GatewayAttemptStatus.MANUAL_INTERVENTION_REQUIRED) {
                healthService.recordVerificationFailure(snapshot.target().getTargetId());
            } else {
                healthService.recordVerificationFailure(snapshot.target().getTargetId());
                metrics.incrementVerificationFailures();
            }
            ProductionNetworkChangeEntity updated =
                    changeRepository.findById(snapshot.change().getProductionChangeId()).orElseThrow();
            return response(
                    updated.getProductionChangeId(),
                    grant.getGrantId(),
                    attempt.getAttemptId(),
                    updated.getStatus(),
                    decision.attemptStatus(),
                    vendorResult.outcome(),
                    decision.reasonCode() == null ? null : decision.reasonCode().name()
            );
        } catch (GatewayDeniedException ex) {
            if (attempt.getStatus() != null
                    && SendPhase.MAY_HAVE_SENT.name().equals(
                    attemptService.countByGrantId(grant.getGrantId()) >= 0
                            ? attempt.getSendPhase() : attempt.getSendPhase())) {
                // keep durable MAY_HAVE_SENT
            }
            throw new GatewayDeniedException(
                    ex.reasonCode(),
                    MutationOutcome.NOT_SENT,
                    GatewayAttemptStatus.PRE_SEND,
                    attempt.getAttemptId(),
                    grant.getGrantId(),
                    grant.getProductionChangeId(),
                    ProductionChangeStatus.EXECUTE_DENIED.name()
            );
        } catch (GatewayFailureInjectionException ex) {
            if (ex.point() == FailureInjectionPoint.MUTATION_INVOKE_START
                    || ex.point() == FailureInjectionPoint.RB_MUTATION_INVOKE_START
                    || ex.point() == FailureInjectionPoint.VENDOR_APPLIED_RESPONSE_LOST
                    || ex.point() == FailureInjectionPoint.RB_VENDOR_APPLIED_RESPONSE_LOST) {
                attemptService.updateStatus(
                        attempt.getAttemptId(),
                        GatewayAttemptStatus.OUTCOME_UNKNOWN,
                        SendPhase.MAY_HAVE_SENT,
                        MutationOutcome.OUTCOME_UNKNOWN
                );
                evidenceService.persist(attempt.getAttemptId(), "OUTCOME", "{\"outcome\":\"OUTCOME_UNKNOWN\"}");
                throw new GatewayDeniedException(
                        ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN,
                        MutationOutcome.OUTCOME_UNKNOWN,
                        GatewayAttemptStatus.OUTCOME_UNKNOWN,
                        attempt.getAttemptId(),
                        grant.getGrantId(),
                        grant.getProductionChangeId(),
                        ProductionChangeStatus.OUTCOME_UNKNOWN.name()
                );
            }
            throw new GatewayDeniedException(
                    ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED,
                    MutationOutcome.NOT_SENT,
                    GatewayAttemptStatus.PRE_SEND,
                    attempt.getAttemptId(),
                    grant.getGrantId(),
                    grant.getProductionChangeId(),
                    ProductionChangeStatus.PREFLIGHT_DENIED.name()
            );
        } finally {
            if (handle != null) {
                handle.destroy();
            }
        }
    }

    private VendorMutationResult invokeMutation(
            ProductionGatewayPreflightService.PreflightSnapshot snapshot,
            UUID attemptId
    ) {
        metrics.incrementExecutionAttempts();
        ProductionNetworkChangeEntity change = snapshot.change();
        GrantType grantType = snapshot.grantType();
        BigDecimal expected = grantType == GrantType.ROLLBACK
                ? change.getRollbackExpectedValue()
                : change.getExpectedValue();
        BigDecimal desired = grantType == GrantType.ROLLBACK
                ? change.getRollbackDesiredValue()
                : change.getDesiredValue();
        AuthorizedParameterMutation mutation = new AuthorizedParameterMutation(
                "CELL",
                "txPower",
                change.getCellId(),
                expected,
                desired
        );
        ProductionExecutionContext context = new ProductionExecutionContext(
                change.getProductionChangeId(),
                null,
                snapshot.target().getTargetId(),
                change.getProductionFingerprint(),
                change.getAuthorizationGeneration(),
                0L,
                "",
                grantType
        );
        try {
            MutationResult result = writeAdapter.applyAuthorizedMutation(context, mutation);
            MutationOutcome outcome = result.outcome();
            if (outcome == MutationOutcome.VENDOR_ACCEPTED) {
                return VendorMutationResult.accepted();
            }
            if (outcome == MutationOutcome.REJECTED) {
                return VendorMutationResult.rejected(
                        result.reasonCode() == null
                                ? ProductionReasonCode.PRODUCTION_VENDOR_REJECTION
                                : result.reasonCode());
            }
            if (outcome == MutationOutcome.NOT_SENT) {
                return VendorMutationResult.notSent(
                        result.reasonCode() == null
                                ? ProductionReasonCode.PRODUCTION_WRITE_TRANSPORT_NOT_CONFIGURED
                                : result.reasonCode());
            }
            return VendorMutationResult.unknown(
                    result.reasonCode() == null
                            ? ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN
                            : result.reasonCode(),
                    true
            );
        } catch (RuntimeException ex) {
            return VendorMutationResult.unknown(ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN, true);
        }
    }

    private void persistMutationOutcome(
            UUID attemptId,
            ProductionNetworkChangeEntity change,
            VendorMutationResult result,
            String callerId
    ) {
        String eventType = switch (result.outcome()) {
            case NOT_SENT -> "PRODUCTION_MUTATION_NOT_SENT";
            case REJECTED -> "PRODUCTION_MUTATION_REJECTED";
            case VENDOR_ACCEPTED -> "PRODUCTION_MUTATION_VENDOR_ACCEPTED";
            case OUTCOME_UNKNOWN -> "PRODUCTION_MUTATION_OUTCOME_UNKNOWN";
        };
        evidenceService.persist(
                attemptId,
                "MUTATION_OUTCOME",
                "{\"outcome\":\"" + result.outcome().name() + "\"}"
        );
        auditService.append(
                change.getProductionChangeId(),
                eventType,
                callerId,
                result.reasonCode() == null ? List.of() : List.of(result.reasonCode().name()),
                Map.of("attemptId", attemptId.toString())
        );
    }

    private GatewayExecuteResponse handleConsumedPreSendRecovery(
            GatewayExecuteRequest request,
            String callerId,
            GrantType grantType
    ) {
        ProductionNetworkChangeEntity change = changeRepository.findById(request.productionChangeId()).orElseThrow();
        change.setStatus(ProductionChangeStatus.CONSUMED_PRE_SEND_RECOVERY_REQUIRED.name());
        change.setReasonCode(ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN.name());
        change.setUpdatedAt(Instant.now());
        change.setVersion(change.getVersion() + 1);
        changeRepository.saveAndFlush(change);

        ProductionExecutionRecoveryEntity recovery = new ProductionExecutionRecoveryEntity();
        recovery.setRecoveryId(UUID.randomUUID());
        recovery.setProductionChangeId(request.productionChangeId());
        recovery.setStatus(ProductionChangeStatus.CONSUMED_PRE_SEND_RECOVERY_REQUIRED.name());
        recovery.setReasonCodes(ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN.name());
        recovery.setSignaledAt(Instant.now());
        recoveryRepository.saveAndFlush(recovery);

        auditService.append(
                request.productionChangeId(),
                "PRODUCTION_RECOVERY_REQUIRED",
                callerId,
                List.of(ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN.name()),
                Map.of("grantId", request.grantId().toString(), "grantType", grantType.name())
        );
        return new GatewayExecuteResponse(
                request.productionChangeId(),
                request.grantId(),
                null,
                ProductionChangeStatus.CONSUMED_PRE_SEND_RECOVERY_REQUIRED.name(),
                null,
                MutationOutcome.NOT_SENT,
                ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN.name(),
                mutationCount()
        );
    }

    private GatewayExecuteResponse response(
            UUID productionChangeId,
            UUID grantId,
            UUID attemptId,
            String productionChangeStatus,
            GatewayAttemptStatus attemptStatus,
            MutationOutcome mutationOutcome,
            String reasonCode
    ) {
        return new GatewayExecuteResponse(
                productionChangeId,
                grantId,
                attemptId,
                productionChangeStatus,
                attemptStatus,
                mutationOutcome,
                reasonCode,
                mutationCount()
        );
    }

    private Integer mutationCount() {
        return mutationInvocationCounter.get();
    }
}
