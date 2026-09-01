package com.simba.snip.npo.productionwritegateway.security;

public interface ProductionWriteCredentialResolver {

    WriteCredentialHandle resolveLatest(String credentialProfileId);

    WriteCredentialHandle resolveVersion(String credentialProfileId, String version);
}
