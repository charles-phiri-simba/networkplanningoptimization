package com.simba.snip.npo.telemetry;

import com.simba.snip.npo.persist.CellEntity;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.KpiObservationEntity;
import com.simba.snip.npo.persist.KpiObservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TelemetryProjectionService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryProjectionService.class);

    private final TelemetryEventValidator validator;
    private final CellRepository cellRepository;
    private final KpiObservationRepository kpiObservationRepository;
    private final TelemetryMetrics metrics;

    public TelemetryProjectionService(
            TelemetryEventValidator validator,
            CellRepository cellRepository,
            KpiObservationRepository kpiObservationRepository,
            TelemetryMetrics metrics
    ) {
        this.validator = validator;
        this.cellRepository = cellRepository;
        this.kpiObservationRepository = kpiObservationRepository;
        this.metrics = metrics;
    }

    @Transactional
    public ProjectionOutcome project(TelemetryEvent event) {
        long started = System.nanoTime();
        validator.validate(event);
        CellEntity cell = cellRepository.findByCellId(event.cellId().trim())
                .orElseThrow(() -> new UnrecoverableTelemetryException("unknown cell: " + event.cellId()));

        if (kpiObservationRepository.findByEventId(event.eventId()).isPresent()) {
            metrics.incrementDuplicatesIgnored();
            log.info(
                    "telemetryDuplicatesIgnored=true eventId={} cellId={} metric={}",
                    event.eventId(), event.cellId(), event.metric()
            );
            return ProjectionOutcome.DUPLICATE;
        }

        Instant ingestedAt = Instant.now();
        KpiObservationEntity observation = KpiObservationEntity.create(
                UUID.randomUUID(),
                cell,
                event.eventId().trim(),
                event.metric().trim(),
                event.value(),
                event.unit().trim(),
                event.eventTime(),
                ingestedAt,
                event.source().trim(),
                Boolean.TRUE.equals(event.synthetic())
        );
        try {
            kpiObservationRepository.saveAndFlush(observation);
        } catch (DataIntegrityViolationException ex) {
            metrics.incrementDuplicatesIgnored();
            log.info(
                    "telemetryDuplicatesIgnored=true eventId={} cellId={} cause=unique-constraint",
                    event.eventId(), event.cellId()
            );
            return ProjectionOutcome.DUPLICATE;
        }

        long latencyMs = (System.nanoTime() - started) / 1_000_000;
        metrics.incrementProjected();
        metrics.recordProjectionLatencyMs(latencyMs);
        log.info(
                "telemetryEventsProjected=1 eventId={} cellId={} metric={} value={} eventTime={} ingestedAt={} "
                        + "source={} synthetic={} telemetryProjectionLatencyMs={}",
                event.eventId(),
                event.cellId(),
                event.metric(),
                event.value(),
                event.eventTime(),
                ingestedAt,
                event.source(),
                event.synthetic(),
                latencyMs
        );
        return ProjectionOutcome.PROJECTED;
    }
}
