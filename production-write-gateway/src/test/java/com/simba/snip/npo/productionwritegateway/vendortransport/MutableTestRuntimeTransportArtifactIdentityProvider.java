package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.RuntimeArtifactIdentity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("test")
@ConditionalOnExpression("'${snip.integration.security.production-runtime:false}'!='true'")
public class MutableTestRuntimeTransportArtifactIdentityProvider implements RuntimeTransportArtifactIdentityProvider {

    private final Environment environment;
    private volatile RuntimeArtifactIdentity identity =
            new PackagedRuntimeTransportArtifactIdentityProvider().currentIdentity();

    public MutableTestRuntimeTransportArtifactIdentityProvider(Environment environment) {
        this.environment = environment;
        assertNotProductionRuntime();
    }

    public void setIdentity(RuntimeArtifactIdentity identity) {
        assertNotProductionRuntime();
        this.identity = identity;
    }

    public void resetToPackaged() {
        assertNotProductionRuntime();
        this.identity = new PackagedRuntimeTransportArtifactIdentityProvider().currentIdentity();
    }

    @Override
    public RuntimeArtifactIdentity currentIdentity() {
        assertNotProductionRuntime();
        if (identity == null) {
            throw new IllegalStateException("missing runtime identity");
        }
        return identity;
    }

    private void assertNotProductionRuntime() {
        if (Boolean.parseBoolean(environment.getProperty("snip.integration.security.production-runtime", "false"))
                || Boolean.parseBoolean(environment.getProperty("snip.production-change.production-runtime", "false"))) {
            throw new IllegalStateException("test runtime artifact identity provider forbidden in production-runtime");
        }
    }
}
