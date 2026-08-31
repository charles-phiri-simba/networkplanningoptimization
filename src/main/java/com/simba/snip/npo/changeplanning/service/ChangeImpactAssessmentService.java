package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.model.ChangeImpactLevel;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ChangeImpactAssessmentService {

    public ChangeImpactLevel assess(ParameterChangeIntent intent) {
        BigDecimal delta = new BigDecimal(intent.expectedCurrentValue())
                .subtract(new BigDecimal(intent.desiredValue()))
                .abs();
        if (delta.compareTo(BigDecimal.ONE) <= 0) {
            return ChangeImpactLevel.MINIMAL;
        }
        if (delta.compareTo(new BigDecimal("2")) <= 0) {
            return ChangeImpactLevel.LOW;
        }
        if (delta.compareTo(new BigDecimal("3")) <= 0) {
            return ChangeImpactLevel.MEDIUM;
        }
        return ChangeImpactLevel.HIGH;
    }
}
