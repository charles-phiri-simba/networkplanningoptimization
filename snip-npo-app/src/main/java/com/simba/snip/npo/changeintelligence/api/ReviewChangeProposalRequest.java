package com.simba.snip.npo.changeintelligence.api;

public record ReviewChangeProposalRequest(
        String reviewer,
        String reasonCode,
        String comment
) {
}
