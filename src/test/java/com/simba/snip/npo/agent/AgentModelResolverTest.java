package com.simba.snip.npo.agent;

import com.simba.snip.npo.config.SnipProperties;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentModelResolverTest {

    @Test
    void allAgentsResolveToTheSamePhysicalModel() {
        SnipProperties properties = new SnipProperties();
        properties.setGenerator("stub");
        properties.setChatModel("qwen2.5:7b");
        AgentModelResolver resolver = new AgentModelResolver(properties);
        AgentRegistry registry = new AgentRegistry();
        Set<String> physical = registry.list().stream()
                .map(resolver::resolve)
                .map(AgentModelProfile::physicalModel)
                .collect(Collectors.toSet());
        assertEquals(Set.of("stub"), physical);
        assertEquals("shared-llm", resolver.resolve(registry.require(AgentRegistry.KNOWLEDGE)).profileId());
        assertEquals("shared-llm", resolver.resolve(registry.require(AgentRegistry.DECISION)).profileId());
    }

    @Test
    void springAiProfileStillUsesOneSharedPhysicalModel() {
        SnipProperties properties = new SnipProperties();
        properties.setGenerator("spring-ai");
        properties.setChatModel("qwen2.5:7b");
        AgentModelResolver resolver = new AgentModelResolver(properties);
        AgentRegistry registry = new AgentRegistry();
        Set<String> physical = registry.list().stream()
                .map(resolver::resolve)
                .map(AgentModelProfile::physicalModel)
                .collect(Collectors.toSet());
        assertEquals(Set.of("qwen2.5:7b"), physical);
    }
}
