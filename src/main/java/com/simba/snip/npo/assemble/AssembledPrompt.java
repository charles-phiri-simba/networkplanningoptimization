package com.simba.snip.npo.assemble;

import com.simba.snip.npo.context.KpiRecord;
import com.simba.snip.npo.retrieve.Chunk;

import java.util.List;
import java.util.Optional;

public record AssembledPrompt(
        String question,
        Optional<KpiRecord> kpi,
        List<Chunk> chunks
) {
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("BEHAVIOURAL INSTRUCTIONS:\n");
        sb.append("- You are a read-only radio planning assistant.\n");
        sb.append("- Answer only from RETRIEVED KNOWLEDGE below.\n");
        sb.append("- Do not invent network facts, 3GPP clauses, or citations.\n");
        sb.append("- If evidence is insufficient, say so explicitly.\n");
        sb.append("- Never claim you changed, applied, or executed a network action.\n");
        sb.append("- Produce an engineering recommendation for a human reviewer, not an execution result.\n\n");

        sb.append("USER QUESTION:\n").append(question).append("\n\n");

        sb.append("SYNTHETIC KPI CONTEXT:\n");
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

        sb.append("RETRIEVED KNOWLEDGE:\n");
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
