package com.simba.snip.npo.assemble;

import com.simba.snip.npo.context.KpiRecord;
import com.simba.snip.npo.retrieve.Chunk;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ContextAssembler {

    public AssembledPrompt assemble(String question, Optional<KpiRecord> kpi, List<Chunk> chunks) {
        return new AssembledPrompt(question, kpi, List.copyOf(chunks));
    }
}
