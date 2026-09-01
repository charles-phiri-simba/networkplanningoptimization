package com.simba.snip.npo.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "snip.generator", havingValue = "stub", matchIfMissing = true)
public class StubAgentNarrator implements AgentNarrator {

    @Override
    public String narrate(AgentDefinition definition, AgentModelProfile profile, String payload) {
        String clipped = payload == null ? "" : payload.replaceAll("\\s+", " ").trim();
        if (clipped.length() > 240) {
            clipped = clipped.substring(0, 240);
        }
        return definition.role().name() + " summary via " + profile.physicalModel() + ": " + clipped;
    }
}
