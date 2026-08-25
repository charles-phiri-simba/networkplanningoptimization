package com.simba.snip.npo.twin;

import com.simba.snip.npo.domain.DomainValidationException;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * In-code whitelist of simulatable parameters. Phase 6 supports {@code txPower} only.
 */
public final class SimulatableParameterRegistry {

    public static final String TX_POWER = "txPower";
    public static final String ELECTRICAL_TILT = "electricalTilt";

    private static final SimulatableParameterDefinition TX_POWER_DEF = new SimulatableParameterDefinition(
            TX_POWER,
            "dBm",
            new BigDecimal("20"),
            new BigDecimal("50"),
            TwinScopeType.CELL,
            true
    );

    private SimulatableParameterRegistry() {
    }

    public static Optional<SimulatableParameterDefinition> find(String parameterId) {
        if (TX_POWER.equals(parameterId)) {
            return Optional.of(TX_POWER_DEF);
        }
        return Optional.empty();
    }

    public static SimulatableParameterDefinition requireEnabled(String parameterId, TwinScopeType scope) {
        SimulatableParameterDefinition definition = find(parameterId)
                .orElseThrow(() -> new DomainValidationException("unsupported parameter: " + parameterId));
        if (!definition.enabled()) {
            throw new DomainValidationException("parameter is disabled: " + parameterId);
        }
        if (definition.scope() != scope) {
            throw new DomainValidationException("parameter scope mismatch: " + parameterId);
        }
        return definition;
    }

    public static BigDecimal requireInRange(SimulatableParameterDefinition definition, BigDecimal value) {
        if (value == null) {
            throw new DomainValidationException("parameter value is required: " + definition.parameterId());
        }
        if (value.compareTo(definition.minValue()) < 0 || value.compareTo(definition.maxValue()) > 0) {
            throw new DomainValidationException(
                    "parameter out of range: " + definition.parameterId()
                            + " value=" + value
                            + " min=" + definition.minValue()
                            + " max=" + definition.maxValue()
            );
        }
        return value;
    }
}
