package com.simba.snip.npo.service;

import com.simba.snip.npo.api.CitationDto;
import com.simba.snip.npo.api.ContextEvidenceDto;
import com.simba.snip.npo.api.ContextUsedDto;
import com.simba.snip.npo.api.RecommendationRequest;
import com.simba.snip.npo.api.RecommendationResponse;
import com.simba.snip.npo.assemble.AssembledPrompt;
import com.simba.snip.npo.assemble.ContextAssembler;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.context.KpiRecord;
import com.simba.snip.npo.context.KpiRepository;
import com.simba.snip.npo.generate.RecommendationGenerator;
import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.network.NetworkContextService;
import com.simba.snip.npo.retrieve.Chunk;
import com.simba.snip.npo.retrieve.ChunkRetriever;
import com.simba.snip.npo.retrieve.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final String EMPTY_MESSAGE =
            "No grounded source was retrieved for this question. I will not invent specification citations.";

    private final ChunkRetriever retriever;
    private final KpiRepository kpiRepository;
    private final NetworkContextService networkContextService;
    private final ContextAssembler assembler;
    private final RecommendationGenerator generator;
    private final SnipProperties properties;

    public RecommendationService(
            ChunkRetriever retriever,
            KpiRepository kpiRepository,
            NetworkContextService networkContextService,
            ContextAssembler assembler,
            RecommendationGenerator generator,
            SnipProperties properties
    ) {
        this.retriever = retriever;
        this.kpiRepository = kpiRepository;
        this.networkContextService = networkContextService;
        this.assembler = assembler;
        this.generator = generator;
        this.properties = properties;
    }

    public RecommendationResponse recommend(RecommendationRequest request) {
        long started = System.nanoTime();

        Optional<CellContext> cellContext = Optional.empty();
        long contextMs = 0L;
        if (request.cellId() != null && !request.cellId().isBlank()) {
            long contextStarted = System.nanoTime();
            cellContext = Optional.of(networkContextService.resolve(request.cellId().trim()));
            contextMs = elapsedMs(contextStarted);
        }

        long retrievalStarted = System.nanoTime();
        List<RetrievedChunk> retrieved = retriever.retrieve(request.question(), properties.getRetrieveTopK());
        long retrievalMs = elapsedMs(retrievalStarted);

        Optional<KpiRecord> kpi = kpiRepository.find(request.contextId());
        ContextUsedDto contextUsed = kpi.map(this::toContext).orElse(null);
        String mode = retriever.mode();
        ContextEvidenceDto evidence = cellContext.map(this::toEvidence).orElse(null);
        boolean contextFound = cellContext.isPresent();
        String contextCellId = cellContext.map(ctx -> ctx.cell().cellId()).orElse(null);
        Integer kpiCount = cellContext.map(ctx -> ctx.kpis().size()).orElse(null);
        Integer neighbourCount = cellContext.map(ctx -> ctx.neighbours().size()).orElse(null);

        if (retrieved.isEmpty()) {
            long totalMs = elapsedMs(started);
            log.info(
                    "recommendation retrievalEmpty=true hits=0 retrievalMode={} contextFound={} contextCellId={} "
                            + "kpiObservationCount={} neighbourCount={} contextResolutionLatencyMs={} "
                            + "retrievalLatencyMs={} generationLatencyMs=0 totalLatencyMs={}",
                    mode, contextFound, contextCellId, kpiCount, neighbourCount, contextMs, retrievalMs, totalMs
            );
            return new RecommendationResponse(
                    EMPTY_MESSAGE, List.of(), contextUsed, true, mode, retrievalMs, 0L, totalMs, 0,
                    evidence, contextMs, contextCellId, contextFound, kpiCount, neighbourCount
            );
        }

        List<Chunk> chunks = retrieved.stream().map(RetrievedChunk::chunk).toList();
        AssembledPrompt prompt = assembler.assemble(request.question(), kpi, cellContext, chunks);

        long generationStarted = System.nanoTime();
        String recommendation = generator.generate(prompt, chunks);
        long generationMs = elapsedMs(generationStarted);

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
        log.info(
                "recommendation retrievalEmpty=false hits={} retrievalMode={} contextFound={} contextCellId={} "
                        + "kpiObservationCount={} neighbourCount={} contextResolutionLatencyMs={} "
                        + "retrievalLatencyMs={} generationLatencyMs={} totalLatencyMs={}",
                citations.size(), mode, contextFound, contextCellId, kpiCount, neighbourCount,
                contextMs, retrievalMs, generationMs, totalMs
        );
        return new RecommendationResponse(
                recommendation, citations, contextUsed, false, mode, retrievalMs, generationMs, totalMs, citations.size(),
                evidence, contextMs, contextCellId, contextFound, kpiCount, neighbourCount
        );
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    private ContextUsedDto toContext(KpiRecord kpi) {
        return new ContextUsedDto(kpi.id(), Map.of(
                "bler", kpi.bler(),
                "band", kpi.band(),
                "dropRate", kpi.dropRate(),
                "latencyMs", kpi.latencyMs(),
                "cell", kpi.cell(),
                "site", kpi.site(),
                "synthetic", true
        ));
    }

    private ContextEvidenceDto toEvidence(CellContext ctx) {
        return new ContextEvidenceDto(
                ctx.cell().cellId(),
                ctx.gnb().gnbId(),
                ctx.site().siteId(),
                ctx.provenance().source(),
                ctx.provenance().synthetic()
        );
    }
}
