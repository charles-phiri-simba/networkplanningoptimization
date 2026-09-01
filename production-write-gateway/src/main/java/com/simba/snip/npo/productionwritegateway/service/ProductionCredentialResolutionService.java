package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import com.simba.snip.npo.productionwritegateway.security.ProductionWriteCredentialResolver;
import com.simba.snip.npo.productionwritegateway.security.WriteCredentialHandle;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductionCredentialResolutionService {

    private static final String[] READ_PROFILE_MARKERS = {
            "inventory-reader",
            "read-only",
            "read_profile",
            "ericsson-enm-int-inventory-reader"
    };

    private final ProductionWriteCredentialResolver resolver;
    private final AtomicInteger credentialResolutionCount = new AtomicInteger();

    public ProductionCredentialResolutionService(ProductionWriteCredentialResolver resolver) {
        this.resolver = resolver;
    }

    public WriteCredentialHandle resolveAfterPreflight(String credentialProfileId, UUID grantId, UUID changeId) {
        if (credentialProfileId == null || credentialProfileId.isBlank()) {
            throw GatewayDeniedException.deny(
                    ProductionReasonCode.PRODUCTION_CREDENTIAL_RESOLUTION_FAILURE, grantId, changeId);
        }
        String lower = credentialProfileId.toLowerCase(Locale.ROOT);
        for (String marker : READ_PROFILE_MARKERS) {
            if (lower.contains(marker)) {
                throw GatewayDeniedException.deny(
                        ProductionReasonCode.PRODUCTION_CREDENTIAL_RESOLUTION_FAILURE, grantId, changeId);
            }
        }
        credentialResolutionCount.incrementAndGet();
        try {
            return resolver.resolveLatest(credentialProfileId);
        } catch (GatewayDeniedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw GatewayDeniedException.deny(
                    ProductionReasonCode.PRODUCTION_CREDENTIAL_RESOLUTION_FAILURE, grantId, changeId);
        }
    }

    public int getCredentialResolutionCount() {
        return credentialResolutionCount.get();
    }

    public void resetCredentialResolutionCount() {
        credentialResolutionCount.set(0);
    }
}
