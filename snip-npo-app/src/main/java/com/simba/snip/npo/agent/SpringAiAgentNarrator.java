package com.simba.snip.npo.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "snip.generator", havingValue = "spring-ai")
public class SpringAiAgentNarrator implements AgentNarrator {

    private final ChatClient chatClient;

    public SpringAiAgentNarrator(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String narrate(AgentDefinition definition, AgentModelProfile profile, String payload) {
        String instructions = "Role: " + definition.description()
                + " Temperature hint: " + definition.temperature()
                + ". Return a concise structured summary. Do not invent evidence. Do not approve actions or invoke tools.";
        String content = chatClient.prompt()
                .system(instructions)
                .user(payload == null ? "" : payload)
                .call()
                .content();
        return content == null || content.isBlank() ? "No model summary." : content.trim();
    }
}
