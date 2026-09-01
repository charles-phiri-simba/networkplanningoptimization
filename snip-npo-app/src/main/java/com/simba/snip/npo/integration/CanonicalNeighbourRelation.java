package com.simba.snip.npo.integration;

public record CanonicalNeighbourRelation(
        String sourceEntityId,
        String sourceDn,
        String canonicalSourceCellId,
        String canonicalTargetCellId,
        String relationType,
        String status
) {
}
