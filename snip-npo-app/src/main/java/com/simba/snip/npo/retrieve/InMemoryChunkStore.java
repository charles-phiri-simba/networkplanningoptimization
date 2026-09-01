package com.simba.snip.npo.retrieve;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class InMemoryChunkStore implements ChunkStore {

    private final List<Chunk> chunks = new ArrayList<>();

    @Override
    public synchronized void replaceAll(List<Chunk> incoming) {
        chunks.clear();
        chunks.addAll(incoming);
    }

    @Override
    public synchronized List<Chunk> all() {
        return List.copyOf(chunks);
    }
}
