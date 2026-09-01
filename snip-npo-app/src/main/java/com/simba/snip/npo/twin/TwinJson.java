package com.simba.snip.npo.twin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.network.CellContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TwinJson {

    private static final TypeReference<List<TwinSnapshot.RadioParameter>> RADIOS = new TypeReference<>() {
    };
    private static final TypeReference<List<TwinSnapshot.MetricValue>> METRICS = new TypeReference<>() {
    };
    private static final TypeReference<List<TwinSnapshot.TemporalSummary>> TEMPORAL = new TypeReference<>() {
    };
    private static final TypeReference<List<TwinSnapshot.NeighbourSummary>> NEIGHBOURS = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public TwinJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("twin json write failed", ex);
        }
    }

    public TwinProvenance provenance(String json) {
        return read(json, TwinProvenance.class);
    }

    public TwinSnapshot.CellIdentity cell(String json) {
        return read(json, TwinSnapshot.CellIdentity.class);
    }

    public List<TwinSnapshot.RadioParameter> radios(String json) {
        return read(json, RADIOS);
    }

    public List<TwinSnapshot.MetricValue> metrics(String json) {
        return read(json, METRICS);
    }

    public List<TwinSnapshot.TemporalSummary> temporal(String json) {
        return read(json, TEMPORAL);
    }

    public List<TwinSnapshot.NeighbourSummary> neighbours(String json) {
        return read(json, NEIGHBOURS);
    }

    public List<String> strings(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return read(json, STRINGS);
    }

    public ServingHolder serving(String json) {
        return read(json, ServingHolder.class);
    }

    public record ServingHolder(TwinSnapshot.CellIdentity cell, TwinSnapshot.ServingIdentity serving) {
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("twin json read failed", ex);
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("twin json read failed", ex);
        }
    }

    public TwinSnapshot snapshotFromContext(CellContext context) {
        return new TwinSnapshot(
                new TwinSnapshot.CellIdentity(
                        context.cell().cellId(),
                        context.cell().name(),
                        context.cell().technology(),
                        context.cell().band(),
                        context.cell().pci(),
                        context.cell().status()
                ),
                new TwinSnapshot.ServingIdentity(
                        context.gnb().gnbId(),
                        context.gnb().name(),
                        context.site().siteId(),
                        context.site().name()
                ),
                context.radioConfiguration().stream()
                        .map(r -> new TwinSnapshot.RadioParameter(
                                r.parameterName(), r.parameterValue(), r.unit(), r.effectiveFrom()))
                        .toList(),
                context.telemetry().stream()
                        .map(s -> new TwinSnapshot.MetricValue(
                                s.metric(), s.current().value(), s.current().unit(), s.current().observedAt()))
                        .toList(),
                context.telemetry().stream()
                        .map(s -> new TwinSnapshot.TemporalSummary(
                                s.metric(), s.trend().name(), s.current().value(), s.history().size()))
                        .toList(),
                context.neighbours().stream()
                        .map(n -> new TwinSnapshot.NeighbourSummary(n.targetCellId(), n.relationType(), n.status()))
                        .toList()
        );
    }
}
