package com.simba.snip.npo.generate;

import com.simba.snip.npo.assemble.AssembledPrompt;
import com.simba.snip.npo.retrieve.Chunk;

import java.util.List;

public interface RecommendationGenerator {

    String generate(AssembledPrompt prompt, List<Chunk> chunks);
}
