package com.simba.snip.npo.agent;

public interface AgentNarrator {

    String narrate(AgentDefinition definition, AgentModelProfile profile, String payload);
}
