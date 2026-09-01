package com.simba.snip.npo.changeintelligence.api;

import com.simba.snip.npo.changeintelligence.model.GenerationInitiator;

import java.util.UUID;

public record GenerateChangeProposalRequest(
        String targetEntityType,
        String targetEntityId,
        String parameterName,
        UUID assuranceCaseId,
        String decisionReference,
        GenerationInitiator generationInitiator,
        String requestedBy
) {
}
