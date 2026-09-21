package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productionchange.protocol.CanonicalJson;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Frozen campaign fingerprint material (§6). Changing any bound field changes the hash.
 */
public final class CampaignFingerprintFactory {

    private CampaignFingerprintFactory() {
    }

    public static Map<String, Object> material() {
        return new LinkedHashMap<>();
    }

    public static String hash(Map<String, Object> material) {
        return Sha256Hex.hash(CanonicalJson.serialize(material));
    }

    public static Map<String, Object> copy(Map<String, Object> material) {
        return new LinkedHashMap<>(material);
    }
}
