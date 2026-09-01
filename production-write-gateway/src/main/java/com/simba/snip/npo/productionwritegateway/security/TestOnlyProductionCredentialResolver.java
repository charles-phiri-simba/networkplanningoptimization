package com.simba.snip.npo.productionwritegateway.security;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test-only resolver. Never used when production-runtime is true.
 * Does not store real secret values.
 */
@Component
@Primary
@ConditionalOnExpression("'${snip.production-change.test-transport-enabled:false}'=='true' && '${snip.integration.security.production-runtime:false}'!='true'")
public class TestOnlyProductionCredentialResolver implements ProductionWriteCredentialResolver {

    private final AtomicBoolean failNext = new AtomicBoolean(false);
    private final AtomicBoolean refuseOldVersions = new AtomicBoolean(true);

    public void failNext() {
        failNext.set(true);
    }

    public void reset() {
        failNext.set(false);
        refuseOldVersions.set(true);
    }

    @Override
    public WriteCredentialHandle resolveLatest(String credentialProfileId) {
        if (failNext.compareAndSet(true, false)) {
            throw GatewayDeniedException.deny(
                    ProductionReasonCode.PRODUCTION_CREDENTIAL_RESOLUTION_FAILURE, null, null);
        }
        if (credentialProfileId != null) {
            String lower = credentialProfileId.toLowerCase(Locale.ROOT);
            if (lower.contains("inventory-reader") || lower.contains("read-only") || lower.contains("read_profile")) {
                throw GatewayDeniedException.deny(
                        ProductionReasonCode.PRODUCTION_CREDENTIAL_RESOLUTION_FAILURE, null, null);
            }
        }
        return new WriteCredentialHandle(credentialProfileId, "latest", new char[0]);
    }

    @Override
    public WriteCredentialHandle resolveVersion(String credentialProfileId, String version) {
        if (refuseOldVersions.get()) {
            throw GatewayDeniedException.deny(
                    ProductionReasonCode.PRODUCTION_CREDENTIAL_RESOLUTION_FAILURE, null, null);
        }
        return resolveLatest(credentialProfileId);
    }
}
