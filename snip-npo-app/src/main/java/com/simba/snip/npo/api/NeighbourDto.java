package com.simba.snip.npo.api;

public record NeighbourDto(
        String targetCellId,
        String relationType,
        String status
) {
}
