package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.changeintelligence.policy.TwinCompatibilityChecker;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
import com.simba.snip.npo.changeintelligence.service.ChangeProposalValidityService;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.changeplanning.model.PreconditionResult;
import com.simba.snip.npo.changeplanning.model.PreconditionType;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanPreconditionEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import com.simba.snip.npo.changeplanning.policy.ChangeExecutionSafetyPolicy;
import com.simba.snip.npo.integration.sync.NetworkDriftService;
import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import com.simba.snip.npo.integration.sync.SynchronizationFreshness;
import com.simba.snip.npo.integration.sync.SynchronizationFreshnessEvaluator;
import com.simba.snip.npo.integration.sync.SynchronizationPolicy;
import com.simba.snip.npo.integration.sync.SynchronizationPolicyRegistry;
import com.simba.snip.npo.integration.sync.SynchronizationSourceStateService;
import com.simba.snip.npo.persist.CellEntity;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.NetworkDriftObservationEntity;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusEntity;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChangePlanSafetyService {

    private final ChangeExecutionSafetyPolicy safetyPolicy;
    private final ChangePlanRollbackService rollbackService;

    public ChangePlanSafetyService(
            ChangeExecutionSafetyPolicy safetyPolicy,
            ChangePlanRollbackService rollbackService
    ) {
        this.safetyPolicy = safetyPolicy;
        this.rollbackService = rollbackService;
    }

    public record SafetyEvaluation(boolean pass, ChangePlanFailureCode failureCode, String reason) {
        public static SafetyEvaluation ok() {
            return new SafetyEvaluation(true, null, null);
        }

        public static SafetyEvaluation fail(ChangePlanFailureCode code, String reason) {
            return new SafetyEvaluation(false, code, reason);
        }
    }

    public SafetyEvaluation evaluateCreation(ParameterChangeIntent intent) {
        ChangeExecutionSafetyPolicy.SafetyResult result = safetyPolicy.evaluateParameter(intent);
        if (!result.pass()) {
            return SafetyEvaluation.fail(result.failureCode(), result.reason());
        }
        return SafetyEvaluation.ok();
    }

    public SafetyEvaluation evaluateRollback(
            com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity forward,
            NetworkChangePlanRollbackOperationEntity rollback
    ) {
        if (!rollbackService.validateRollback(forward, rollback)) {
            return SafetyEvaluation.fail(ChangePlanFailureCode.PLAN_ROLLBACK_UNAVAILABLE, "invalid rollback");
        }
        return SafetyEvaluation.ok();
    }
}
