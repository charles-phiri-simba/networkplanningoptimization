package com.simba.snip.npo.assemble;

import com.simba.snip.npo.context.KpiRecord;
import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.retrieve.Chunk;

import java.util.List;
import java.util.Optional;

public record AssembledPrompt(
        String question,
        Optional<KpiRecord> kpi,
        Optional<CellContext> cellContext,
        List<Chunk> chunks
) {
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("SAFETY / BEHAVIOURAL INSTRUCTIONS:\n");
        sb.append("- You are a read-only radio planning assistant.\n");
        sb.append("- Answer from STRUCTURED NETWORK CONTEXT, TEMPORAL KPI HISTORY / TRENDS, and RETRIEVED ENGINEERING KNOWLEDGE below.\n");
        sb.append("- TREND values are precomputed deterministic facts. Do not recalculate them.\n");
        sb.append("- Distinguish observations (from context/knowledge) from inference.\n");
        sb.append("- Do not invent network facts, 3GPP clauses, or citations.\n");
        sb.append("- If evidence is insufficient, say so explicitly.\n");
        sb.append("- Never claim you changed, applied, or executed a network action.\n");
        sb.append("- Do not present a definitive autonomous root-cause determination.\n");
        sb.append("- Produce an engineering recommendation for a human reviewer.\n");
        sb.append("- Bundled operational context is SYNTHETIC demo data, not live OSS/NMS/EMS telemetry.\n\n");

        sb.append("USER QUESTION:\n").append(question).append("\n\n");

        sb.append("STRUCTURED NETWORK CONTEXT:\n");
        if (cellContext.isPresent()) {
            CellContext ctx = cellContext.get();
            sb.append("synthetic=").append(ctx.provenance().synthetic())
                    .append(" source=").append(ctx.provenance().source()).append('\n');
            sb.append("cellId=").append(ctx.cell().cellId())
                    .append(" name=").append(ctx.cell().name())
                    .append(" technology=").append(ctx.cell().technology())
                    .append(" band=").append(ctx.cell().band())
                    .append(" pci=").append(ctx.cell().pci())
                    .append(" arfcn=").append(ctx.cell().arfcn())
                    .append(" bandwidthMhz=").append(ctx.cell().bandwidthMhz())
                    .append(" duplex=").append(ctx.cell().duplexMode())
                    .append(" status=").append(ctx.cell().status())
                    .append('\n');
            sb.append("gnbId=").append(ctx.gnb().gnbId())
                    .append(" vendor=").append(ctx.gnb().vendor())
                    .append(" model=").append(ctx.gnb().model())
                    .append('\n');
            sb.append("siteId=").append(ctx.site().siteId())
                    .append(" name=").append(ctx.site().name())
                    .append('\n');
            sb.append("radioConfiguration:\n");
            for (CellContext.RadioParameterView radio : ctx.radioConfiguration()) {
                sb.append("- ").append(radio.parameterName()).append('=').append(radio.parameterValue());
                if (radio.unit() != null) {
                    sb.append(' ').append(radio.unit());
                }
                sb.append(" effectiveFrom=").append(radio.effectiveFrom()).append('\n');
            }
            sb.append("recentKpiObservations:\n");
            for (CellContext.KpiObservationView kpiObs : ctx.kpis()) {
                sb.append("- ").append(kpiObs.formatted())
                        .append(" observedAt=").append(kpiObs.observedAt())
                        .append(" synthetic=").append(kpiObs.synthetic())
                        .append('\n');
            }
            sb.append("neighbours:\n");
            for (CellContext.NeighbourView neighbour : ctx.neighbours()) {
                sb.append("- ").append(neighbour.targetCellId())
                        .append(" type=").append(neighbour.relationType())
                        .append(" status=").append(neighbour.status())
                        .append('\n');
            }
        } else {
            sb.append("None supplied.\n");
        }
        sb.append('\n');

        sb.append("TEMPORAL KPI HISTORY / TRENDS:\n");
        if (cellContext.isPresent() && !cellContext.get().telemetry().isEmpty()) {
            sb.append("Trends are precomputed. Use them as given.\n");
            for (CellContext.KpiSeriesView series : cellContext.get().telemetry()) {
                sb.append(series.metric()).append(":\n");
                sb.append("  current: ").append(series.current().formatted())
                        .append(" eventTime=").append(series.current().observedAt())
                        .append(" ingestedAt=").append(series.current().ingestedAt())
                        .append(" source=").append(series.current().source())
                        .append('\n');
                if (series.history().size() >= 2) {
                    CellContext.KpiObservationView previous = series.history().get(series.history().size() - 2);
                    sb.append("  previous: ").append(previous.formatted())
                            .append(" eventTime=").append(previous.observedAt())
                            .append('\n');
                }
                sb.append("  trend: ").append(series.trend()).append('\n');
                sb.append("  history (eventTime order, last N):\n");
                for (CellContext.KpiObservationView point : series.history()) {
                    sb.append("    - ").append(point.formatted())
                            .append(" eventTime=").append(point.observedAt())
                            .append(" eventId=").append(point.eventId())
                            .append('\n');
                }
            }
        } else {
            sb.append("None supplied.\n");
        }
        sb.append('\n');

        sb.append("SYNTHETIC KPI CONTEXT (legacy optional contextId):\n");
        if (kpi.isPresent()) {
            KpiRecord record = kpi.get();
            sb.append("This context is SYNTHETIC demo data, not production telemetry. ");
            sb.append("id=").append(record.id())
                    .append(" site=").append(record.site())
                    .append(" cell=").append(record.cell())
                    .append(" band=").append(record.band())
                    .append(" bler=").append(record.bler())
                    .append(" dropRate=").append(record.dropRate())
                    .append(" latencyMs=").append(record.latencyMs())
                    .append('\n');
        } else {
            sb.append("None supplied.\n");
        }
        sb.append('\n');

        sb.append("RETRIEVED ENGINEERING KNOWLEDGE:\n");
        for (Chunk chunk : chunks) {
            sb.append("[id=").append(chunk.id())
                    .append(" sourceId=").append(chunk.sourceId())
                    .append(" locator=").append(chunk.locator())
                    .append("]\n")
                    .append(chunk.text())
                    .append("\n\n");
        }
        sb.append("Write a grounded recommendation. Do not invent source ids.");
        return sb.toString();
    }
}
