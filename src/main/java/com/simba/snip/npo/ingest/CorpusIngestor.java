package com.simba.snip.npo.ingest;

import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.retrieve.Chunk;
import com.simba.snip.npo.retrieve.ChunkStore;
import com.simba.snip.npo.retrieve.VectorSimilarityRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class CorpusIngestor implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CorpusIngestor.class);

    private final SnipProperties properties;
    private final DocumentChunker chunker = new DocumentChunker();
    private final ChunkStore store;
    private final ObjectProvider<VectorStore> vectorStore;

    public CorpusIngestor(
            SnipProperties properties,
            ChunkStore store,
            ObjectProvider<VectorStore> vectorStore
    ) {
        this.properties = properties;
        this.store = store;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Path dir = Path.of(properties.getCorpusDir());
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("Corpus directory not found: " + dir.toAbsolutePath());
        }
        List<Chunk> chunks = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> path.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .forEach(path -> chunks.addAll(read(path)));
        }
        store.replaceAll(chunks);
        VectorStore vectors = vectorStore.getIfAvailable();
        if (vectors != null) {
            List<Document> documents = chunks.stream().map(CorpusIngestor::toDocument).toList();
            vectors.add(documents);
            log.info("Embedded corpus into vector store chunks={}", documents.size());
        }
        log.info("Ingested corpus chunks count={} retrievalMode={}", chunks.size(), properties.getRetrievalMode());
    }

    public static Document toDocument(Chunk chunk) {
        Map<String, Object> meta = new HashMap<>();
        meta.put(VectorSimilarityRetriever.META_ID, chunk.id());
        meta.put(VectorSimilarityRetriever.META_SOURCE, chunk.sourceId());
        meta.put(VectorSimilarityRetriever.META_LOCATOR, chunk.locator());
        meta.put(VectorSimilarityRetriever.META_SNIPPET, chunk.snippet());
        return new Document(chunk.id(), chunk.text(), meta);
    }

    private List<Chunk> read(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return chunker.chunk(path.getFileName().toString(), content);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read corpus file " + path, ex);
        }
    }
}
