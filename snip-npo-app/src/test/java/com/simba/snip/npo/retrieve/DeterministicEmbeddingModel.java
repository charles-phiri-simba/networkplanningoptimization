package com.simba.snip.npo.retrieve;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tiny token-hash embedding for CI vector tests. Not used on the local-ai acceptance path.
 */
public class DeterministicEmbeddingModel implements EmbeddingModel {

    static final int DIM = 64;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        int index = 0;
        for (String input : request.getInstructions()) {
            embeddings.add(new Embedding(embed(input), index++));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIM];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.length() < 2) {
                continue;
            }
            int slot = Math.floorMod(token.hashCode(), DIM);
            vector[slot] += 1.0f;
        }
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= (float) norm;
            }
        }
        return vector;
    }

    @Override
    public int dimensions() {
        return DIM;
    }

    public Document asDocument(String id, String text) {
        return new Document(id, text, java.util.Map.of());
    }
}
