package com.simba.snip.npo.changeintelligence.policy;

import com.simba.snip.npo.changeintelligence.config.ChangeIntelligenceProperties;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import com.simba.snip.npo.twin.SimulatableParameterDefinition;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import com.simba.snip.npo.twin.TwinScopeType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class ChangeProposalConstraintValidator {

    private final ChangeIntelligenceProperties properties;

    public ChangeProposalConstraintValidator(ChangeIntelligenceProperties properties) {
        this.properties = properties;
    }

    public record ValidationResult(boolean valid, ChangeProposalFailureCode failureCode, String reason) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult fail(ChangeProposalFailureCode code, String reason) {
            return new ValidationResult(false, code, reason);
        }
    }

    public ValidationResult validateParameter(String parameterName, TwinScopeType scope) {
        Optional<SimulatableParameterDefinition> definition = SimulatableParameterRegistry.find(parameterName);
        if (definition.isEmpty() || !definition.get().enabled()) {
            return ValidationResult.fail(ChangeProposalFailureCode.UNSUPPORTED_PARAMETER, parameterName);
        }
        if (definition.get().scope() != scope) {
            return ValidationResult.fail(ChangeProposalFailureCode.UNSUPPORTED_TARGET, scope.name());
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateCandidate(
            SimulatableParameterDefinition definition,
            BigDecimal currentValue,
            BigDecimal candidateValue
    ) {
        properties.validate();
        try {
            SimulatableParameterRegistry.requireInRange(definition, candidateValue);
        } catch (RuntimeException ex) {
            return ValidationResult.fail(ChangeProposalFailureCode.CANDIDATE_OUT_OF_RANGE, ex.getMessage());
        }
        BigDecimal delta = candidateValue.subtract(currentValue).abs();
        if (delta.compareTo(BigDecimal.valueOf(properties.getMaxDelta())) > 0) {
            return ValidationResult.fail(
                    ChangeProposalFailureCode.CANDIDATE_OUT_OF_RANGE,
                    "candidate exceeds configured maxDelta"
            );
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateCurrentValuePresent(BigDecimal currentValue) {
        if (currentValue == null) {
            return ValidationResult.fail(ChangeProposalFailureCode.CURRENT_STATE_UNAVAILABLE, "txPower missing");
        }
        return ValidationResult.ok();
    }
}
