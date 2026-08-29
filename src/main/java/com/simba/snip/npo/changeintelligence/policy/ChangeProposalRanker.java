package com.simba.snip.npo.changeintelligence.policy;

import com.simba.snip.npo.action.RiskLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class ChangeProposalRanker {

    public record RankedCandidate(
            BigDecimal candidateValue,
            BigDecimal benefitScore,
            RiskLevel riskLevel,
            BigDecimal proposalScore,
            boolean baseline,
            int rank
    ) {
    }

    public List<RankedCandidate> rank(
            List<CandidateEvaluation> evaluations,
            BigDecimal currentValue
    ) {
        List<RankedCandidate> ranked = evaluations.stream()
                .filter(e -> !e.baseline())
                .sorted(Comparator
                        .comparing(CandidateEvaluation::proposalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(e -> e.riskLevel().ordinal())
                        .thenComparing(e -> e.candidateValue().subtract(currentValue).abs())
                        .thenComparing(CandidateEvaluation::candidateValue))
                .map(e -> new RankedCandidate(
                        e.candidateValue(),
                        e.benefitScore(),
                        e.riskLevel(),
                        e.proposalScore(),
                        false,
                        0
                ))
                .toList();
        int rank = 1;
        List<RankedCandidate> withRank = new java.util.ArrayList<>();
        for (RankedCandidate candidate : ranked) {
            withRank.add(new RankedCandidate(
                    candidate.candidateValue(),
                    candidate.benefitScore(),
                    candidate.riskLevel(),
                    candidate.proposalScore(),
                    false,
                    rank++
            ));
        }
        return withRank;
    }

    public record CandidateEvaluation(
            BigDecimal candidateValue,
            BigDecimal benefitScore,
            RiskLevel riskLevel,
            BigDecimal proposalScore,
            boolean baseline
    ) {
    }
}
