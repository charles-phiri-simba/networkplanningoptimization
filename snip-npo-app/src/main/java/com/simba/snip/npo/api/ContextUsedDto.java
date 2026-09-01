package com.simba.snip.npo.api;

import java.util.Map;

public record ContextUsedDto(
        String id,
        Map<String, Object> kpis
) {
}
