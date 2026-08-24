package com.simba.snip.npo.assemble;

import com.simba.snip.npo.assurance.AssuranceCaseView;
import com.simba.snip.npo.context.KpiRecord;
import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.retrieve.Chunk;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ContextAssembler {

    public AssembledPrompt assemble(
            String question,
            Optional<KpiRecord> kpi,
            Optional<CellContext> cellContext,
            List<Chunk> chunks
    ) {
        return assemble(question, kpi, cellContext, chunks, Optional.empty());
    }

    public AssembledPrompt assemble(
            String question,
            Optional<KpiRecord> kpi,
            Optional<CellContext> cellContext,
            List<Chunk> chunks,
            Optional<AssuranceCaseView> assuranceCase
    ) {
        return new AssembledPrompt(question, kpi, cellContext, List.copyOf(chunks), assuranceCase);
    }
}
