package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.Vendor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectorRegistry {

    public static final String ERICSSON_ENDPOINT_REF = "ericsson-enm-int";
    public static final String NOKIA_ENDPOINT_REF = "nokia-netact-int";
    public static final String ERICSSON_CREDENTIAL_REF = "ericsson-enm-int-inventory-reader";
    public static final String NOKIA_CREDENTIAL_REF = "nokia-netact-int-inventory-reader";
    public static final String ERICSSON_TRUST = "ericsson-int-custom-ca";
    public static final String NOKIA_TRUST = "nokia-int-custom-ca";
    public static final String ERICSSON_NETWORK = "ericsson-int-egress";
    public static final String NOKIA_NETWORK = "nokia-int-egress";
    public static final String SYSTEM_TRUST = "system-ca";

    private final Map<String, ConnectorDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, ConnectorTrustProfile> trustProfiles = new ConcurrentHashMap<>();
    private final Map<String, ConnectorAuthorizationProfile> authorizationProfiles = new ConcurrentHashMap<>();
    private final Map<String, ConnectorNetworkPolicy> networkPolicies = new ConcurrentHashMap<>();
    private final ConnectorEndpointRegistry endpoints;

    public ConnectorRegistry(ConnectorEndpointRegistry endpoints) {
        this.endpoints = endpoints;
        authorizationProfiles.put(
                ConnectorAuthorizationProfile.READ_ONLY_NETWORK_INVENTORY,
                ConnectorAuthorizationProfile.readOnlyNetworkInventory()
        );
        trustProfiles.put(SYSTEM_TRUST, new ConnectorTrustProfile(
                SYSTEM_TRUST, TrustMode.SYSTEM_CA, List.of(), true, List.of(), null));
        trustProfiles.put(ERICSSON_TRUST, new ConnectorTrustProfile(
                ERICSSON_TRUST, TrustMode.CUSTOM_CA, List.of(), true, List.of("localhost"), null));
        trustProfiles.put(NOKIA_TRUST, new ConnectorTrustProfile(
                NOKIA_TRUST, TrustMode.CUSTOM_CA, List.of(), true, List.of("localhost"), null));
        networkPolicies.put(ERICSSON_NETWORK, new ConnectorNetworkPolicy(
                ERICSSON_NETWORK, List.of("enm.invalid"), Set.of(443), true, false));
        networkPolicies.put(NOKIA_NETWORK, new ConnectorNetworkPolicy(
                NOKIA_NETWORK, List.of("netact.invalid"), Set.of(443), true, false));
        endpoints.register(new ConnectorEndpoint(ERICSSON_ENDPOINT_REF, URI.create("https://enm.invalid")));
        endpoints.register(new ConnectorEndpoint(NOKIA_ENDPOINT_REF, URI.create("https://netact.invalid")));
        definitions.put(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER, ericsson(false, AuthenticationMethod.BASIC));
        definitions.put(ConnectorDefinition.NOKIA_NETACT_INT_INVENTORY_READER, nokia(false, AuthenticationMethod.BASIC));
    }

    public ConnectorDefinition require(String connectorId) {
        ConnectorDefinition definition = definitions.get(connectorId);
        if (definition == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CONNECTOR_DISABLED, "connector is not registered");
        }
        return definition;
    }

    public List<ConnectorDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public ConnectorTrustProfile trust(String trustProfileId) {
        ConnectorTrustProfile profile = trustProfiles.get(trustProfileId);
        if (profile == null) {
            throw new ConnectorSecurityException(ImportFailureCode.TLS_TRUST_FAILED, "trust profile is not registered");
        }
        return profile;
    }

    public ConnectorAuthorizationProfile authorization(String authorizationProfileId) {
        ConnectorAuthorizationProfile profile = authorizationProfiles.get(authorizationProfileId);
        if (profile == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED, "authorization profile is not registered");
        }
        return profile;
    }

    public ConnectorNetworkPolicy networkPolicy(String networkPolicyId) {
        ConnectorNetworkPolicy policy = networkPolicies.get(networkPolicyId);
        if (policy == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.NETWORK_POLICY_DENIED, "network policy is not registered");
        }
        return policy;
    }

    public void replace(ConnectorDefinition definition) {
        definitions.put(definition.connectorId(), definition);
    }

    public void replaceTrust(ConnectorTrustProfile profile) {
        trustProfiles.put(profile.trustProfileId(), profile);
    }

    public void replaceNetworkPolicy(ConnectorNetworkPolicy policy) {
        networkPolicies.put(policy.networkPolicyId(), policy);
    }

    public void replaceAuthorization(ConnectorAuthorizationProfile profile) {
        authorizationProfiles.put(profile.authorizationProfileId(), profile);
    }

    public ConnectorDefinition enable(String connectorId, boolean enabled) {
        ConnectorDefinition current = require(connectorId);
        ConnectorDefinition updated = new ConnectorDefinition(
                current.connectorId(),
                current.vendor(),
                current.sourceSystem(),
                current.sourceScope(),
                current.endpointRef(),
                current.inventoryPath(),
                current.credentialRef(),
                current.trustProfileId(),
                current.authorizationProfileId(),
                current.networkPolicyId(),
                current.authenticationMethod(),
                current.credentialProvider(),
                current.requiredCapabilities(),
                enabled,
                current.mode()
        );
        replace(updated);
        return updated;
    }

    private static ConnectorDefinition ericsson(boolean enabled, AuthenticationMethod method) {
        return new ConnectorDefinition(
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                Vendor.ERICSSON,
                "ERICSSON_SECURE_MOCK",
                "DEFAULT",
                ERICSSON_ENDPOINT_REF,
                "/inventory",
                ERICSSON_CREDENTIAL_REF,
                ERICSSON_TRUST,
                ConnectorAuthorizationProfile.READ_ONLY_NETWORK_INVENTORY,
                ERICSSON_NETWORK,
                method,
                CredentialProviderType.LOCAL_DEVELOPMENT,
                ConnectorAuthorizationProfile.READ_INVENTORY_CAPABILITIES,
                enabled,
                ConnectorMode.MOCK_SECURE
        );
    }

    private static ConnectorDefinition nokia(boolean enabled, AuthenticationMethod method) {
        return new ConnectorDefinition(
                ConnectorDefinition.NOKIA_NETACT_INT_INVENTORY_READER,
                Vendor.NOKIA,
                "NOKIA_SECURE_MOCK",
                "DEFAULT",
                NOKIA_ENDPOINT_REF,
                "/inventory",
                NOKIA_CREDENTIAL_REF,
                NOKIA_TRUST,
                ConnectorAuthorizationProfile.READ_ONLY_NETWORK_INVENTORY,
                NOKIA_NETWORK,
                method,
                CredentialProviderType.LOCAL_DEVELOPMENT,
                ConnectorAuthorizationProfile.READ_INVENTORY_CAPABILITIES,
                enabled,
                ConnectorMode.MOCK_SECURE
        );
    }
}
