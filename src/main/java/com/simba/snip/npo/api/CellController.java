package com.simba.snip.npo.api;

import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.network.NetworkContextService;
import com.simba.snip.npo.network.NetworkDomainService;
import com.simba.snip.npo.persist.CellEntity;
import com.simba.snip.npo.persist.KpiObservationEntity;
import com.simba.snip.npo.persist.NeighbourRelationshipEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CellController {

    private final NetworkDomainService domainService;
    private final NetworkContextService contextService;

    public CellController(NetworkDomainService domainService, NetworkContextService contextService) {
        this.domainService = domainService;
        this.contextService = contextService;
    }

    @GetMapping("/api/v1/cells")
    public List<CellDto> list() {
        return domainService.listCells().stream().map(CellController::toDto).toList();
    }

    @GetMapping("/api/v1/cells/{cellId}")
    public CellDto get(@PathVariable String cellId) {
        return toDto(domainService.requireCell(cellId));
    }

    @GetMapping("/api/v1/cells/{cellId}/kpis")
    public List<KpiObservationDto> kpis(@PathVariable String cellId) {
        CellEntity cell = domainService.requireCell(cellId);
        return domainService.kpisForCell(cell).stream().map(CellController::toKpi).toList();
    }

    @GetMapping("/api/v1/cells/{cellId}/neighbours")
    public List<NeighbourDto> neighbours(@PathVariable String cellId) {
        CellEntity cell = domainService.requireCell(cellId);
        return domainService.neighboursForCell(cell).stream().map(CellController::toNeighbour).toList();
    }

    @GetMapping("/api/v1/cells/{cellId}/context")
    public CellContextDto context(@PathVariable String cellId) {
        return toContextDto(contextService.resolve(cellId));
    }

    static CellDto toDto(CellEntity cell) {
        return new CellDto(
                cell.getCellId(),
                cell.getName(),
                cell.getGnb().getGnbId(),
                cell.getGnb().getSite().getSiteId(),
                cell.getTechnology(),
                cell.getBand(),
                cell.getArfcn(),
                cell.getPci(),
                cell.getBandwidthMhz(),
                cell.getDuplexMode(),
                cell.getStatus()
        );
    }

    static KpiObservationDto toKpi(KpiObservationEntity kpi) {
        return new KpiObservationDto(
                kpi.getMetric(),
                kpi.getValue(),
                kpi.getUnit(),
                kpi.getObservedAt(),
                kpi.getSource(),
                kpi.isSynthetic()
        );
    }

    static NeighbourDto toNeighbour(NeighbourRelationshipEntity rel) {
        return new NeighbourDto(rel.getTargetCell().getCellId(), rel.getRelationType(), rel.getStatus());
    }

    static CellContextDto toContextDto(CellContext ctx) {
        return new CellContextDto(
                new CellDto(
                        ctx.cell().cellId(),
                        ctx.cell().name(),
                        ctx.gnb().gnbId(),
                        ctx.site().siteId(),
                        ctx.cell().technology(),
                        ctx.cell().band(),
                        ctx.cell().arfcn(),
                        ctx.cell().pci(),
                        ctx.cell().bandwidthMhz(),
                        ctx.cell().duplexMode(),
                        ctx.cell().status()
                ),
                new GnbDto(
                        ctx.gnb().gnbId(),
                        ctx.gnb().name(),
                        ctx.site().siteId(),
                        ctx.gnb().vendor(),
                        ctx.gnb().model(),
                        ctx.gnb().status()
                ),
                new SiteDto(
                        ctx.site().siteId(),
                        ctx.site().name(),
                        ctx.site().latitude(),
                        ctx.site().longitude(),
                        ctx.site().status()
                ),
                ctx.radioConfiguration().stream()
                        .map(r -> new CellContextDto.RadioParameterDto(
                                r.parameterName(), r.parameterValue(), r.unit(), r.effectiveFrom()))
                        .toList(),
                ctx.kpis().stream()
                        .map(k -> new KpiObservationDto(
                                k.metric(), k.value(), k.unit(), k.observedAt(), k.source(), k.synthetic()))
                        .toList(),
                ctx.neighbours().stream()
                        .map(n -> new NeighbourDto(n.targetCellId(), n.relationType(), n.status()))
                        .toList(),
                new CellContextDto.ContextProvenanceDto(ctx.provenance().source(), ctx.provenance().synthetic())
        );
    }
}
