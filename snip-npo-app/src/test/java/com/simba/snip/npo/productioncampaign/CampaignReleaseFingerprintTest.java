package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.service.CampaignReleaseFingerprintFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CampaignReleaseFingerprintTest {

    private static Map<String, Object> base() {
        return CampaignReleaseFingerprintFactory.material(
                "campaign-1",
                1,
                "a".repeat(64),
                "cohort-1",
                List.of("item-1", "item-2"),
                List.of("b".repeat(64), "c".repeat(64)),
                3L,
                4L,
                5L,
                CampaignReleaseFingerprintFactory.NONE_DIGEST,
                CampaignReleaseFingerprintFactory.NONE_DIGEST,
                2,
                "releaser-1",
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    @Test
    void sameAuthoritativeScopeYieldsSameFingerprint() {
        assertEquals(
                CampaignReleaseFingerprintFactory.hash(base()),
                CampaignReleaseFingerprintFactory.hash(CampaignReleaseFingerprintFactory.copy(base()))
        );
    }

    @Test
    void eachFrozenFieldIsBound() {
        String original = CampaignReleaseFingerprintFactory.hash(base());
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("campaignRevision", 2)));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("campaignFingerprint", "d".repeat(64))));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("orderedItemIds", List.of("item-2", "item-1"))));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("orderedItemFingerprints", List.of("e".repeat(64), "c".repeat(64)))));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("networkObservationHealthEvidenceDigest", "f".repeat(64))));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("operationalSafetyHealthEvidenceDigest", "1".repeat(64))));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("authorizationGeneration", 9)));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("releaseGeneration", 30L)));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("campaignControlGeneration", 40L)));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("campaignFencingToken", 50L)));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate("releasedBy", "other")));
        assertNotEquals(original, CampaignReleaseFingerprintFactory.hash(mutate(
                "releasedAt", Instant.parse("2026-01-02T00:00:00Z").toString())));
    }

    private static Map<String, Object> mutate(String key, Object value) {
        Map<String, Object> copy = CampaignReleaseFingerprintFactory.copy(base());
        copy.put(key, value);
        return copy;
    }
}
