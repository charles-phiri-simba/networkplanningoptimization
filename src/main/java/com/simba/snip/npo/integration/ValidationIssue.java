package com.simba.snip.npo.integration;

public record ValidationIssue(
        RejectionReasonCode reasonCode,
        CanonicalEntityType entityType,
        String sourceEntityId,
        String details
) {
}
