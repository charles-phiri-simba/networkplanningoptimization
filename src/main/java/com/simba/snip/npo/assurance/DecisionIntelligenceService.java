package com.simba.snip.npo.assurance;

import com.simba.snip.npo.api.CitationDto;
import com.simba.snip.npo.api.DecisionAssessmentDto;
import com.simba.snip.npo.assemble.AssembledPrompt;
import com.simba.snip.npo.assemble.ContextAssembler;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.generate.RecommendationGenerator;
import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.network.NetworkContextService;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.retrieve.Chunk;
import com.simba.snip.npo.retrieve.ChunkRetriever;
import com.simba.snip.npo.retrieve.RetrievedChunk;
import com.simba.snip.npo.telemetry.Trend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DecisionIntelligenceService {

    public static final String CANONICAL_QUESTION =
            "Why has SNIP raised a DEGRADING_RADIO_QUALITY assurance case for CELL-001, and what should I investigate first?";

    private static final Logger log = LoggerFactory.getLogger(DecisionIntelligenceService.class);
    private static final String EMPTY_SUMMARY =
            "No grounded engineering note was retrieved. Operational evidence and deterministic severity/confidence still apply. Confirmed root cause is not established. Human review is required.";

    private final AssuranceCaseService assuranceCaseService;
    private final NetworkContextService networkContextService;
    private final ChunkRetriever retriever;
    private final ContextAssembler assembler;
    private final RecommendationGenerator generator;
    private final SnipProperties properties;
    private final AssuranceMetrics metrics;

    public DecisionIntelligenceService(
            AssuranceCaseService assuranceCaseService,
            NetworkContextService networkContextService,
            ChunkRetriever retriever,
            ContextAssembler assembler,
            RecommendationGenerator generator,
            SnipProperties properties,
            AssuranceMetrics metrics
    ) {
        this.assuranceCaseService = assuranceCaseService;
        this.networkContextService = networkContextService;
        this.retriever = retriever;
        this.assembler = assembler;
        this.generator = generator;
        this.properties = properties;
        this.metrics = metrics;
    }

    public DecisionAssessmentDto assess(UUID caseId) {
        return assess(caseId, CANONICAL_QUESTION);
    }

    public DecisionAssessmentDto assess(UUID caseId, String question) {
        long started = System.nanoTime();
        AssuranceCaseEntity assuranceCase = assuranceCaseService.findById(caseId)
                .orElseThrow(() -> new DomainNotFoundException("assurance case", caseId.toString()));
        CellContext context = networkContextService.resolve(assuranceCase.getAffectedEntityId());
        AssuranceCaseView caseView = AssuranceCaseView.from(assuranceCase);

        long retrievalStarted = System.nanoTime();
        String retrievalQuery = retrievalQuery(question, context, caseView);
        List<RetrievedChunk> retrieved = retriever.retrieve(retrievalQuery, properties.getRetrieveTopK());
        long retrievalMs = elapsedMs(retrievalStarted);

        List<String> contributors = DecisionSupportComposer.likelyContributors(assuranceCase, context);
        List<String> checks = DecisionSupportComposer.recommendedChecks(assuranceCase);
        List<String> missing = DecisionSupportComposer.missingEvidence(assuranceCase, context);
        Urgency urgency = DecisionSupportComposer.urgency(Severity.valueOf(assuranceCase.getSeverity()));

        String summary;
        long generationMs = 0L;
        if (retrieved.isEmpty()) {
            summary = EMPTY_SUMMARY;
        } else {
            List<Chunk> chunks = retrieved.stream().map(RetrievedChunk::chunk).toList();
            AssembledPrompt prompt = assembler.assemble(
                    question, Optional.empty(), Optional.of(context), chunks, Optional.of(caseView));
            long generationStarted = System.nanoTime();
            summary = generator.generate(prompt, chunks);
            generationMs = elapsedMs(generationStarted);
        }

        List<CitationDto> citations = retrieved.stream()
                .map(hit -> new CitationDto(
                        hit.chunk().sourceId(),
                        hit.chunk().locator(),
                        hit.chunk().snippet(),
                        hit.chunk().id(),
                        hit.score()
                ))
                .toList();
        long totalMs = elapsedMs(started);
        metrics.recordAssessmentLatencyMs(totalMs);
        log.info(
                "decisionAssessment caseId={} retrievalEmpty={} hits={} retrievalMode={} "
                        + "humanReviewRequired=true severity={} confidence={} "
                        + "retrievalLatencyMs={} generationLatencyMs={} decisionAssessmentLatencyMs={}",
                caseId,
                retrieved.isEmpty(),
                citations.size(),
                retriever.mode(),
                assuranceCase.getSeverity(),
                assuranceCase.getConfidence(),
                retrievalMs,
                generationMs,
                totalMs
        );
        return new DecisionAssessmentDto(
                assuranceCase.getId(),
                summary,
                contributors,
                checks,
                missing,
                urgency.name(),
                true,
                assuranceCase.getSeverity(),
                assuranceCase.getConfidence(),
                assuranceCase.getCaseType(),
                assuranceCase.getStatus(),
                AssuranceMapper.toEvidence(assuranceCase),
                citations,
                retrieved.isEmpty(),
                retriever.mode(),
                retrievalMs,
                generationMs,
                totalMs
        );
    }

    static String retrievalQuery(String question, CellContext context, AssuranceCaseView assuranceCase) {
        StringBuilder sb = new StringBuilder(question);
        sb.append(' ').append(context.cell().technology()).append(' ').append(context.cell().band());
        sb.append(' ').append(assuranceCase.caseType());
        for (CellContext.KpiSeriesView series : context.telemetry()) {
            if (series.trend() == Trend.INCREASING || series.trend() == Trend.DECREASING) {
                sb.append(' ').append(series.metric());
            }
        }
        return sb.toString();
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
