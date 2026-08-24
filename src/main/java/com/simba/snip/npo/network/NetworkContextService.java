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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
        List<KpiObservationEntity> kpis = kpiObservationRepository
                .findByCell_IdAndObservedAtGreaterThanEqualOrderByObservedAtDesc(cell.getId(), since);
        int limit = properties.getRecentKpiLimit();
        if (kpis.size() > limit) {
            kpis = kpis.subList(0, limit);
        }

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
                kpis.stream()
                        .map(k -> new CellContext.KpiObservationView(
                                k.getMetric(),
                                k.getValue(),
                                k.getUnit(),
                                k.getObservedAt(),
                                k.getSource(),
                                k.isSynthetic()
                        ))
                        .toList(),
                neighbours.stream()
                        .map(n -> new CellContext.NeighbourView(
                                n.getTargetCell().getCellId(),
                                n.getRelationType(),
                                n.getStatus()
                        ))
                        .toList(),
                new CellContext.ContextProvenance(source, synthetic || "DEMO_SEED".equals(source))
        );
    }
}
