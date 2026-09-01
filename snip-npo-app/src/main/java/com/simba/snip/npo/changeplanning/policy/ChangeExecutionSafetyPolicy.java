package com.simba.snip.npo.changeplanning.policy;

import com.simba.snip.npo.changeintelligence.config.ChangeIntelligenceProperties;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import com.simba.snip.npo.twin.TwinScopeType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ChangeExecutionSafetyPolicy {

    private final ChangeIntelligenceProperties changeIntelligenceProperties;

    public ChangeExecutionSafetyPolicy(ChangeIntelligenceProperties changeIntelligenceProperties) {
        this.changeIntelligenceProperties = changeIntelligenceProperties;
    }

    public record SafetyResult(boolean pass, ChangePlanFailureCode failureCode, String reason) {
        public static SafetyResult ok() {
            return new SafetyResult(true, null, null);
        }

        public static SafetyResult fail(ChangePlanFailureCode code, String reason) {
            return new SafetyResult(false, code, reason);
        }
    }

    public SafetyResult evaluateParameter(ParameterChangeIntent intent) {
        if (!SimulatableParameterRegistry.TX_POWER.equals(intent.parameter())) {
            return SafetyResult.fail(ChangePlanFailureCode.PLAN_PARAMETER_UNSUPPORTED, intent.parameter());
        }
        var def = SimulatableParameterRegistry.find(intent.parameter());
        if (def.isEmpty() || !def.get().enabled() || def.get().scope() != TwinScopeType.CELL) {
            return SafetyResult.fail(ChangePlanFailureCode.PLAN_PARAMETER_UNSUPPORTED, intent.parameter());
        }
        BigDecimal desired = new BigDecimal(intent.desiredValue());
        BigDecimal expected = new BigDecimal(intent.expectedCurrentValue());
        if (desired.compareTo(def.get().minValue()) < 0 || desired.compareTo(def.get().maxValue()) > 0) {
            return SafetyResult.fail(ChangePlanFailureCode.PLAN_VALUE_OUT_OF_RANGE, desired.toPlainString());
        }
        BigDecimal delta = expected.subtract(desired).abs();
        if (delta.compareTo(BigDecimal.valueOf(changeIntelligenceProperties.getMaxDelta())) > 0) {
            return SafetyResult.fail(ChangePlanFailureCode.PLAN_DELTA_TOO_LARGE, delta.toPlainString());
        }
        return SafetyResult.ok();
    }
}
