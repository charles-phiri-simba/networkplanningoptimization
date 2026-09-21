package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.CanonicalJson;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CampaignHandoffIdFactory {

    public String create(UUID campaignId, int campaignRevision, UUID itemId, String bindingDigest) {
        if (campaignId == null || itemId == null || bindingDigest == null || bindingDigest.isBlank()) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH,
                    "CampaignHandoffId material is incomplete"
            );
        }
        Map<String, Object> material = new LinkedHashMap<>();
        material.put("campaignId", campaignId.toString());
        material.put("campaignRevision", Integer.toString(campaignRevision));
        material.put("itemId", itemId.toString());
        material.put("bindingDigest", bindingDigest.toLowerCase());
        return Sha256Hex.hash(CanonicalJson.serialize(material));
    }

    public String bindingDigest(Map<String, Object> bindingFields) {
        return Sha256Hex.hash(CanonicalJson.serialize(bindingFields));
    }

    public static String canonicalDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return stripped.toPlainString();
    }
}
