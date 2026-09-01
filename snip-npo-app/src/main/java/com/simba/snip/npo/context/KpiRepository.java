package com.simba.snip.npo.context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.config.SnipProperties;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class KpiRepository {

    private final Map<String, KpiRecord> byId;

    public KpiRepository(SnipProperties properties, ObjectMapper mapper) throws IOException {
        Path file = Path.of(properties.getKpiFile());
        List<KpiRecord> rows = mapper.readValue(file.toFile(), new TypeReference<>() {
        });
        this.byId = rows.stream().collect(Collectors.toUnmodifiableMap(KpiRecord::id, Function.identity()));
    }

    public Optional<KpiRecord> find(String contextId) {
        if (contextId == null || contextId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(contextId));
    }
}
