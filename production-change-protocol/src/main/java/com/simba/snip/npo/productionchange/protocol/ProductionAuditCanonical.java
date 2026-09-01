package com.simba.snip.npo.productionchange.protocol;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Shared canonical audit-event map for app and gateway hash chains.
 */
public final class ProductionAuditCanonical {

    private ProductionAuditCanonical() {
    }

    public static Map<String, Object> eventPayload(
            UUID productionChangeId,
            String eventType,
            int eventVersion,
            long sequenceNumber,
            Instant occurredAt,
            String actorPrincipalId,
            List<String> reasonCodes,
            Object safePayload
    ) {
        Map<String, Object> canonical = new TreeMap<>();
        canonical.put("actorPrincipalId", actorPrincipalId);
        canonical.put("eventType", eventType);
        canonical.put("eventVersion", eventVersion);
        canonical.put("occurredAt", occurredAt);
        canonical.put("productionChangeId", productionChangeId);
        canonical.put("reasonCodes", reasonCodes == null ? List.of() : reasonCodes);
        canonical.put("safePayload", safePayload == null ? Map.of() : safePayload);
        canonical.put("sequenceNumber", sequenceNumber);
        return canonical;
    }

    public static String eventHash(String previousEventHash, Map<String, Object> canonicalEvent) {
        return Sha256Hex.hash(previousEventHash + CanonicalJson.serialize(canonicalEvent));
    }
}
