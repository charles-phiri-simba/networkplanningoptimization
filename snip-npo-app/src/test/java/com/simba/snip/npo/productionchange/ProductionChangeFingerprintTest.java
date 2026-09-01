package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ProductionChangeFingerprintTest extends ProductionChangeITSupport {

    @Test
    void noSilentReauthorization() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        String before = authorized.productionFingerprint();
        jdbc.update("UPDATE production_network_target SET capability_profile_version = '2' WHERE target_id = ?",
                TARGET_ID);
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue4xxOrStale(denied);
        ProductionChangeDto reloaded = getProductionChange(authorized.productionChangeId());
        assertEquals(before, reloaded.productionFingerprint());
        assertNotEquals("VERIFIED", reloaded.status());
        assertEquals(0, mutationCount());
    }

    private static void assertTrue4xxOrStale(ResponseEntity<String> denied) {
        org.junit.jupiter.api.Assertions.assertTrue(
                denied.getStatusCode().is4xxClientError()
                        || String.valueOf(denied.getBody()).contains("STALE")
                        || String.valueOf(denied.getBody()).contains("FINGERPRINT"));
    }
}
