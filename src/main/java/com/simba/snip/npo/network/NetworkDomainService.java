package com.simba.snip.npo.network;

import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.persist.CellEntity;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.GnbEntity;
import com.simba.snip.npo.persist.GnbRepository;
import com.simba.snip.npo.persist.KpiObservationEntity;
import com.simba.snip.npo.persist.KpiObservationRepository;
import com.simba.snip.npo.persist.NeighbourRelationshipEntity;
import com.simba.snip.npo.persist.NeighbourRelationshipRepository;
import com.simba.snip.npo.persist.SiteEntity;
import com.simba.snip.npo.persist.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NetworkDomainService {

    private final SiteRepository siteRepository;
    private final GnbRepository gnbRepository;
    private final CellRepository cellRepository;
    private final KpiObservationRepository kpiObservationRepository;
    private final NeighbourRelationshipRepository neighbourRelationshipRepository;

    public NetworkDomainService(
            SiteRepository siteRepository,
            GnbRepository gnbRepository,
            CellRepository cellRepository,
            KpiObservationRepository kpiObservationRepository,
            NeighbourRelationshipRepository neighbourRelationshipRepository
    ) {
        this.siteRepository = siteRepository;
        this.gnbRepository = gnbRepository;
        this.cellRepository = cellRepository;
        this.kpiObservationRepository = kpiObservationRepository;
        this.neighbourRelationshipRepository = neighbourRelationshipRepository;
    }

    public List<SiteEntity> listSites() {
        return siteRepository.findAllByOrderBySiteIdAsc();
    }

    public SiteEntity requireSite(String siteId) {
        return siteRepository.findBySiteId(siteId)
                .orElseThrow(() -> new DomainNotFoundException("site", siteId));
    }

    public List<GnbEntity> listGnbs() {
        return gnbRepository.findAllByOrderByGnbIdAsc();
    }

    public GnbEntity requireGnb(String gnbId) {
        return gnbRepository.findByGnbId(gnbId)
                .orElseThrow(() -> new DomainNotFoundException("gnb", gnbId));
    }

    public List<CellEntity> listCells() {
        return cellRepository.findAllByOrderByCellIdAsc();
    }

    public CellEntity requireCell(String cellId) {
        return cellRepository.findByCellId(cellId)
                .orElseThrow(() -> new DomainNotFoundException("cell", cellId));
    }

    public List<KpiObservationEntity> kpisForCell(CellEntity cell) {
        return kpiObservationRepository.findByCell_IdAndObservedAtGreaterThanEqualOrderByObservedAtDesc(
                cell.getId(),
                java.time.Instant.EPOCH
        );
    }

    public List<NeighbourRelationshipEntity> neighboursForCell(CellEntity cell) {
        return neighbourRelationshipRepository.findBySourceCell_IdOrderByTargetCell_CellIdAsc(cell.getId());
    }
}
