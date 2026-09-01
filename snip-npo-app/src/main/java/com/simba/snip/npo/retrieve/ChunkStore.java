package com.simba.snip.npo.retrieve;

import java.util.List;

public interface ChunkStore {

    void replaceAll(List<Chunk> chunks);

    List<Chunk> all();
}
