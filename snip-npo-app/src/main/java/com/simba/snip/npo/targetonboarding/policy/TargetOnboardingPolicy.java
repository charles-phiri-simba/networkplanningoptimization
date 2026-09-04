package com.simba.snip.npo.targetonboarding.policy;

import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import org.springframework.stereotype.Component;

@Component
public class TargetOnboardingPolicy {

    private final Phase17SeparationOfDutiesPolicy sod;

    public TargetOnboardingPolicy(Phase17SeparationOfDutiesPolicy sod) {
        this.sod = sod;
    }

    public Phase17SeparationOfDutiesPolicy sod() {
        return sod;
    }
}
