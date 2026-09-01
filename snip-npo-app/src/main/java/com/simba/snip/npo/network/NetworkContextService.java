package com.simba.snip.npo.network;

import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.persist.CellEntity;
import com.simba.snip.npo.persist.GnbEntity;
import com.simba.snip.npo.persist.KpiObservationEntity;
import com.simba.snip.npo.persist.KpiObservationRepository;
import com.simba.snip.npo.persist.NeighbourRelationshipEntity;
import com.simba.snip.npo.persist.NeighbourRelationshipRepository;
import com.simba.snip.npo.persist.RadioConfigurationEntity;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.persist.SiteEntity;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.Trend;
import com.simba.snip.npo.telemetry.TrendClassifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NetworkContextService {

    private final NetworkDomainService domainService;
    private final RadioConfigurationRepository radioConfigurationRepository;
    private final KpiObservationRepository kpiObservationRepository;
    private final NeighbourRelationshipRepository neighbourRelationshipRepository;
    private final SnipProperties properties;

    public NetworkContextService(
            NetworkDomainService domainService,
            RadioConfigurationRepository radioConfigurationRepository,
            KpiObservationRepository kpiObservationRepository,
            NeighbourRelationshipRepository neighbourRelationshipRepository,
            SnipProperties properties
    ) {
        this.domainService = domainService;
        this.radioConfigurationRepository = radioConfigurationRepository;
        this.kpiObservationRepository = kpiObservationRepository;
        this.neighbourRelationshipRepository = neighbourRelationshipRepository;
        this.properties = properties;
    }

    public CellContext resolve(String cellId) {
        CellEntity cell = domainService.requireCell(cellId);
        GnbEntity gnb = cell.getGnb();
        SiteEntity site = gnb.getSite();

        Instant since = Instant.now().minus(properties.getRecentKpiHours(), ChronoUnit.HOURS);
        List<KpiObservationEntity> recent = kpiObservationRepository
                .findByCell_IdAndObservedAtGreaterThanEqualOrderByObservedAtDesc(cell.getId(), since);
        int limit = properties.getRecentKpiLimit();
        List<KpiObservationEntity> kpis = recent.size() > limit ? recent.subList(0, limit) : recent;

        List<RadioConfigurationEntity> radios = radioConfigurationRepository.findByCell_IdOrderByParameterNameAsc(cell.getId());
        List<NeighbourRelationshipEntity> neighbours =
                neighbourRelationshipRepository.findBySourceCell_IdOrderByTargetCell_CellIdAsc(cell.getId());

        boolean synthetic = kpis.stream().allMatch(KpiObservationEntity::isSynthetic) && !kpis.isEmpty();
        String source = kpis.stream().map(KpiObservationEntity::getSource).findFirst().orElse("DEMO_SEED");

        return new CellContext(
                new CellContext.CellView(
                        cell.getCellId(),
                        cell.getName(),
                        cell.getTechnology(),
                        cell.getBand(),
                        cell.getArfcn(),
                        cell.getPci(),
                        cell.getBandwidthMhz(),
                        cell.getDuplexMode(),
                        cell.getStatus()
                ),
                new CellContext.GnbView(
                        gnb.getGnbId(),
                        gnb.getName(),
                        gnb.getVendor(),
                        gnb.getModel(),
                        gnb.getStatus()
                ),
                new CellContext.SiteView(
                        site.getSiteId(),
                        site.getName(),
                        site.getLatitude(),
                        site.getLongitude(),
                        site.getStatus()
                ),
                radios.stream()
                        .map(r -> new CellContext.RadioParameterView(
                                r.getParameterName(),
                                r.getParameterValue(),
                                r.getUnit(),
                                r.getEffectiveFrom()
                        ))
                        .toList(),
                kpis.stream().map(NetworkContextService::toKpiView).toList(),
                neighbours.stream()
                        .map(n -> new CellContext.NeighbourView(
                                n.getTargetCell().getCellId(),
                                n.getRelationType(),
                                n.getStatus()
                        ))
                        .toList(),
                buildTelemetry(recent),
                new CellContext.ContextProvenance(source, synthetic || "DEMO_SEED".equals(source))
        );
    }

    private List<CellContext.KpiSeriesView> buildTelemetry(List<KpiObservationEntity> recentDesc) {
        int window = properties.getTelemetryHistoryN();
        Map<String, List<KpiObservationEntity>> byMetric = recentDesc.stream()
                .collect(Collectors.groupingBy(KpiObservationEntity::getMetric, LinkedHashMap::new, Collectors.toList()));
        List<CellContext.KpiSeriesView> series = new ArrayList<>();
        for (Map.Entry<String, List<KpiObservationEntity>> entry : byMetric.entrySet()) {
            List<KpiObservationEntity> rows = entry.getValue();
            boolean hasSimulator = rows.stream().anyMatch(k -> TelemetryEvent.SOURCE_SIMULATOR.equals(k.getSource()));
            if (hasSimulator) {
                rows = rows.stream().filter(k -> TelemetryEvent.SOURCE_SIMULATOR.equals(k.getSource())).toList();
            }
            List<KpiObservationEntity> lastNDesc = rows.size() > window ? rows.subList(0, window) : rows;
            List<KpiObservationEntity> chronological = new ArrayList<>(lastNDesc);
            chronological.sort(Comparator.comparing(KpiObservationEntity::getObservedAt));
            List<Double> values = chronological.stream().map(KpiObservationEntity::getValue).toList();
            Trend trend = TrendClassifier.classify(values);
            KpiObservationEntity current = chronological.get(chronological.size() - 1);
            series.add(new CellContext.KpiSeriesView(
                    entry.getKey(),
                    toKpiView(current),
                    chronological.stream().map(NetworkContextService::toKpiView).toList(),
                    trend
            ));
        }
        series.sort(Comparator.comparing(CellContext.KpiSeriesView::metric));
        return series;
    }

    private static CellContext.KpiObservationView toKpiView(KpiObservationEntity k) {
        return new CellContext.KpiObservationView(
                k.getMetric(),
                k.getValue(),
                k.getUnit(),
                k.getObservedAt(),
                k.getIngestedAt(),
                k.getEventId(),
                k.getSource(),
                k.isSynthetic()
        );
    }
}
