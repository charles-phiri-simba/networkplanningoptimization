package com.simba.snip.npo.twin;

public record SimulationModelMetadata(
        String modelId,
        String modelVersion,
        String modelType,
        java.util.List<String> assumptions
) {
}
