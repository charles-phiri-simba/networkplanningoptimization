package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productionchange.protocol.CanonicalJson;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frozen CohortReleaseFingerprint material (architecture §12 / specification §7).
 */
public final class CampaignReleaseFingerprintFactory {

    public static final String NONE_DIGEST = Sha256Hex.hash("{\"status\":\"NONE\"}");

    private CampaignReleaseFingerprintFactory() {
    }

    public static Map<String, Object> material(
            String campaignId,
            int campaignRevision,
            String campaignFingerprint,
            String cohortId,
            List<String> orderedItemIds,
            List<String> orderedItemFingerprints,
            long releaseGeneration,
            long campaignControlGeneration,
            long campaignFencingToken,
            String networkObservationHealthEvidenceDigest,
            String operationalSafetyHealthEvidenceDigest,
            int authorizationGeneration,
            String releasedBy,
            Instant releasedAt
    ) {
        Map<String, Object> material = new LinkedHashMap<>();
        material.put("campaignId", campaignId);
        material.put("campaignRevision", campaignRevision);
        material.put("campaignFingerprint", campaignFingerprint);
        material.put("cohortId", cohortId);
        material.put("orderedItemIds", orderedItemIds == null ? List.of() : new ArrayList<>(orderedItemIds));
        material.put("orderedItemFingerprints",
                orderedItemFingerprints == null ? List.of() : new ArrayList<>(orderedItemFingerprints));
        material.put("releaseGeneration", releaseGeneration);
        material.put("campaignControlGeneration", campaignControlGeneration);
        material.put("campaignFencingToken", campaignFencingToken);
        material.put("networkObservationHealthEvidenceDigest",
                networkObservationHealthEvidenceDigest == null ? NONE_DIGEST : networkObservationHealthEvidenceDigest);
        material.put("operationalSafetyHealthEvidenceDigest",
                operationalSafetyHealthEvidenceDigest == null ? NONE_DIGEST : operationalSafetyHealthEvidenceDigest);
        material.put("authorizationGeneration", authorizationGeneration);
        material.put("releasedBy", releasedBy);
        material.put("releasedAt", releasedAt == null ? null : releasedAt.toString());
        return material;
    }

    public static String hash(Map<String, Object> material) {
        return Sha256Hex.hash(CanonicalJson.serialize(material));
    }

    public static Map<String, Object> copy(Map<String, Object> material) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : material.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                copy.put(entry.getKey(), new ArrayList<>(list));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }
}
