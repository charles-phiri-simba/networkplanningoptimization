package com.simba.snip.npo.integration.enm;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SimulatorEnmScenarioController {

    private final AtomicReference<SimulatorEnmScenario> scenario =
            new AtomicReference<>(SimulatorEnmScenario.SUCCESS_SINGLE_PAGE);
    private final AtomicInteger rateLimitRemaining = new AtomicInteger(2);
    private final AtomicInteger unavailableRemaining = new AtomicInteger(2);

    public SimulatorEnmScenario scenario() {
        return scenario.get();
    }

    public void use(SimulatorEnmScenario next) {
        scenario.set(next);
        rateLimitRemaining.set(2);
        unavailableRemaining.set(2);
    }

    public int decrementRateLimit() {
        return rateLimitRemaining.getAndDecrement();
    }

    public int decrementUnavailable() {
        return unavailableRemaining.getAndDecrement();
    }

    public void setRateLimitHits(int hits) {
        rateLimitRemaining.set(hits);
    }

    public void setUnavailableHits(int hits) {
        unavailableRemaining.set(hits);
    }
}
