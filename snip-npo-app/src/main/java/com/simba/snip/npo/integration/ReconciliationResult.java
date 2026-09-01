package com.simba.snip.npo.integration;

public record ReconciliationResult(
        int entitiesRead,
        int created,
        int updated,
        int unchanged,
        int rejected,
        int conflicts,
        int missing
) {
}
