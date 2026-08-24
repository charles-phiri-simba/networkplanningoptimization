package com.simba.snip.npo.api;

import jakarta.validation.constraints.NotBlank;

public record RecommendationRequest(
        @NotBlank String question,
        String contextId,
        String cellId
) {
}
