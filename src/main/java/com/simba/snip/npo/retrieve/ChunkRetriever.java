package com.simba.snip.npo.retrieve;

import java.util.List;

public interface ChunkRetriever {

    String mode();

    List<RetrievedChunk> retrieve(String question, int topK);
}
