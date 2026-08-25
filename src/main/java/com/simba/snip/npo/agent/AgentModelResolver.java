package com.simba.snip.npo.agent;

import com.simba.snip.npo.config.SnipProperties;
import org.springframework.stereotype.Component;

@Component
public class AgentModelResolver {

    private final SnipProperties properties;

    public AgentModelResolver(SnipProperties properties) {
        this.properties = properties;
    }

    public AgentModelProfile resolve(AgentDefinition definition) {
        String generator = properties.getGenerator() == null || properties.getGenerator().isBlank()
                ? "stub"
                : properties.getGenerator();
        String physical = "spring-ai".equals(generator) ? properties.getChatModel() : "stub";
        return new AgentModelProfile(definition.modelProfile(), physical, generator);
    }
}
