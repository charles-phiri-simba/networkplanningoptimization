package com.simba.snip.npo.assurance;

import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.network.NetworkContextService;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AssuranceDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AssuranceDetectionService.class);

    private final NetworkContextService networkContextService;
    private final AssuranceCaseService assuranceCaseService;
    private final SnipProperties properties;
    private final AssuranceMetrics metrics;

    public AssuranceDetectionService(
            NetworkContextService networkContextService,
            AssuranceCaseService assuranceCaseService,
            SnipProperties properties,
            AssuranceMetrics metrics
    ) {
        this.networkContextService = networkContextService;
        this.assuranceCaseService = assuranceCaseService;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional
    public Optional<AssuranceCaseEntity> evaluateCell(String cellId) {
        long started = System.nanoTime();
        CellContext context = networkContextService.resolve(cellId);
        Optional<DegradingRadioQualityDetector.Detection> detection = DegradingRadioQualityDetector.evaluate(
                context,
                new DegradingRadioQualityDetector.Thresholds(
                        properties.getAssuranceBlerDlThreshold(),
                        properties.getAssuranceBlerDlMajorThreshold(),
                        properties.getAssuranceBlerDlCriticalThreshold()
                )
        );
        long latencyMs = (System.nanoTime() - started) / 1_000_000;
        metrics.recordDetectionLatencyMs(latencyMs);
        if (detection.isEmpty()) {
            metrics.incrementNoMatch();
            log.info(
                    "assuranceEvaluationsNoMatch=1 cellId={} assuranceDetectionLatencyMs={}",
                    cellId, latencyMs
            );
            return Optional.empty();
        }
        metrics.incrementDetected();
        DegradingRadioQualityDetector.Detection match = detection.get();
        AssuranceCaseEntity stored = assuranceCaseService.upsertActive(cellId, match);
        log.info(
                "assuranceCasesDetected=1 cellId={} caseId={} caseType={} severity={} confidence={} status={} "
                        + "ruleId={} synthetic={} assuranceCaseSeverity={} assuranceDetectionLatencyMs={}",
                cellId,
                stored.getId(),
                stored.getCaseType(),
                stored.getSeverity(),
                stored.getConfidence(),
                stored.getStatus(),
                stored.getRuleId(),
                stored.isSynthetic(),
                stored.getSeverity(),
                latencyMs
        );
        return Optional.of(stored);
    }
}
