package com.simba.snip.npo.integration;

public record SourceNeighbour(
        String sourceEntityId,
        String sourceDn,
        String canonicalSourceCellId,
        String canonicalTargetCellId,
        String relationType,
        String operationalStateRaw
) {
}
