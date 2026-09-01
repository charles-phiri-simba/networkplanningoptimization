package com.simba.snip.npo.twin;

import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.network.NetworkContextService;
import com.simba.snip.npo.persist.NetworkTwinEntity;
import com.simba.snip.npo.persist.NetworkTwinRepository;
import com.simba.snip.npo.persist.NetworkTwinVersionEntity;
import com.simba.snip.npo.persist.NetworkTwinVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TwinSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(TwinSynchronizationService.class);

    private final NetworkContextService networkContextService;
    private final NetworkTwinRepository twinRepository;
    private final NetworkTwinVersionRepository versionRepository;
    private final TwinJson twinJson;
    private final TwinMetrics metrics;
    private final SnipProperties properties;

    public TwinSynchronizationService(
            NetworkContextService networkContextService,
            NetworkTwinRepository twinRepository,
            NetworkTwinVersionRepository versionRepository,
            TwinJson twinJson,
            TwinMetrics metrics,
            SnipProperties properties
    ) {
        this.networkContextService = networkContextService;
        this.twinRepository = twinRepository;
        this.versionRepository = versionRepository;
        this.twinJson = twinJson;
        this.metrics = metrics;
        this.properties = properties;
    }

    @Transactional
    public NetworkTwinEntity synchronizeCell(String cellId) {
        try {
            CellContext context = networkContextService.resolve(cellId);
            Instant now = Instant.now();
            NetworkTwinEntity twin = twinRepository
                    .findByScopeTypeAndScopeId(TwinScopeType.CELL.name(), cellId)
                    .orElseGet(() -> twinRepository.save(NetworkTwinEntity.create(
                            UUID.randomUUID(),
                            "Cell Twin " + cellId,
                            TwinScopeType.CELL.name(),
                            cellId,
                            TwinStatus.ACTIVE.name(),
                            now,
                            context.provenance().synthetic()
                    )));
            TwinSnapshot snapshot = twinJson.snapshotFromContext(context);
            String fingerprint = TwinFingerprint.of(context);
            TwinProvenance provenance = new TwinProvenance(
                    TwinProvenance.SOURCE_SNIP_OPERATIONAL_STATE,
                    cellId,
                    fingerprint,
                    context.lastEventTime(),
                    now,
                    context.provenance().synthetic()
            );
            int version = twin.getLatestVersion() + 1;
            versionRepository.save(NetworkTwinVersionEntity.create(
                    UUID.randomUUID(),
                    twin,
                    version,
                    now,
                    now,
                    context.lastEventTime(),
                    fingerprint,
                    twinJson.write(provenance),
                    twinJson.write(new TwinJson.ServingHolder(snapshot.cell(), snapshot.serving())),
                    twinJson.write(snapshot.configuration()),
                    twinJson.write(snapshot.currentMetrics()),
                    twinJson.write(snapshot.temporalSummary()),
                    twinJson.write(snapshot.neighbourSummary())
            ));
            twin.recordSynchronization(version, now, context.provenance().synthetic());
            twinRepository.save(twin);
            metrics.incrementSynchronizations();
            metrics.incrementVersionsCreated();
            log.info(
                    "twinSynchronizations=1 twinVersionsCreated=1 cellId={} twinId={} version={} synthetic={}",
                    cellId, twin.getId(), version, twin.isSynthetic()
            );
            return twin;
        } catch (RuntimeException ex) {
            metrics.incrementSynchronizationFailures();
            log.info("twinSynchronizationFailures=1 cellId={} error={}", cellId, ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public TwinFreshness freshness(NetworkTwinVersionEntity version) {
        CellContext current = networkContextService.resolve(version.getTwin().getScopeId());
        return TwinFreshnessEvaluator.evaluate(
                version.getSynchronizedAt(),
                version.getSourceContextVersion(),
                TwinFingerprint.of(current),
                Instant.now(),
                properties.getTwinExpiredHours()
        );
    }

    @Transactional(readOnly = true)
    public void requireCurrentForSimulation(NetworkTwinVersionEntity version) {
        TwinFreshness freshness = freshness(version);
        if (freshness == TwinFreshness.STALE) {
            metrics.incrementStaleDetections();
            log.info(
                    "twinStaleDetections=1 twinId={} version={} cellId={}",
                    version.getTwin().getId(), version.getVersion(), version.getTwin().getScopeId()
            );
            throw new com.simba.snip.npo.domain.DomainConflictException(
                    "Twin is STALE; resynchronization required before simulation"
            );
        }
        if (freshness == TwinFreshness.EXPIRED) {
            throw new com.simba.snip.npo.domain.DomainConflictException(
                    "Twin is EXPIRED; resynchronization required before simulation"
            );
        }
    }

    @Transactional(readOnly = true)
    public NetworkTwinEntity requireTwin(UUID twinId) {
        return twinRepository.findById(twinId)
                .orElseThrow(() -> new DomainNotFoundException("twin", twinId.toString()));
    }

    @Transactional(readOnly = true)
    public NetworkTwinVersionEntity requireVersion(UUID twinId, int version) {
        return versionRepository.findByTwin_IdAndVersion(twinId, version)
                .orElseThrow(() -> new DomainNotFoundException("twin version", twinId + "/" + version));
    }
}
