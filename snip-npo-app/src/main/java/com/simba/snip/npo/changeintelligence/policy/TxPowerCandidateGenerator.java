package com.simba.snip.npo.changeintelligence.policy;

import com.simba.snip.npo.changeintelligence.config.ChangeIntelligenceProperties;
import com.simba.snip.npo.twin.SimulatableParameterDefinition;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TxPowerCandidateGenerator {

    private final ChangeIntelligenceProperties properties;

    public TxPowerCandidateGenerator(ChangeIntelligenceProperties properties) {
        this.properties = properties;
    }

    public List<BigDecimal> generate(BigDecimal currentValue) {
        properties.validate();
        SimulatableParameterDefinition definition = SimulatableParameterRegistry.requireEnabled(
                SimulatableParameterRegistry.TX_POWER,
                com.simba.snip.npo.twin.TwinScopeType.CELL
        );
        List<BigDecimal> candidates = new ArrayList<>();
        candidates.add(currentValue);
        int step = properties.getCandidateStep();
        int maxDelta = properties.getMaxDelta();
        int maxCandidates = properties.getMaxCandidates();
        for (int delta = step; delta <= maxDelta; delta += step) {
            BigDecimal lower = currentValue.subtract(BigDecimal.valueOf(delta));
            if (lower.compareTo(definition.minValue()) >= 0) {
                candidates.add(lower);
            }
            BigDecimal higher = currentValue.add(BigDecimal.valueOf(delta));
            if (higher.compareTo(definition.maxValue()) <= 0) {
                candidates.add(higher);
            }
        }
        return candidates.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .limit(maxCandidates)
                .toList();
    }
}
