package com.simba.snip.npo.generate;

import com.simba.snip.npo.assemble.AssembledPrompt;
import com.simba.snip.npo.retrieve.Chunk;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "snip.generator", havingValue = "spring-ai")
public class SpringAiRecommendationGenerator implements RecommendationGenerator {

    private final ChatClient chatClient;

    public SpringAiRecommendationGenerator(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String generate(AssembledPrompt prompt, List<Chunk> chunks) {
        return chatClient.prompt()
                .user(prompt.render())
                .call()
                .content();
    }
}
