package com.simba.snip.npo.changeintelligence.api;

import java.util.List;

public record ChangeProposalDetailDto(
        ChangeProposalSummaryDto proposal,
        List<CandidateEvidenceDto> candidates
) {
}
