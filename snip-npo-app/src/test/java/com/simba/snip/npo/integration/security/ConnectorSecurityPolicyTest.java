package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.Vendor;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorSecurityPolicyTest {

    @Test
    void unknownCapabilityIsDenied() {
        ConnectorAuthorizationProfile profile = ConnectorAuthorizationProfile.readOnlyNetworkInventory();
        assertFalse(profile.allowsAll(Set.of(ConnectorCapability.WRITE_CONFIGURATION)));
        assertFalse(profile.allowsAll(Set.of(ConnectorCapability.READ_CONFIGURATION, ConnectorCapability.LOCK)));
        assertTrue(profile.allowsAll(ConnectorAuthorizationProfile.READ_INVENTORY_CAPABILITIES));
    }

    @Test
    void networkPolicyDeniesUnapprovedHostAndScheme() {
        ConnectorNetworkPolicy policy = new ConnectorNetworkPolicy(
                "p", List.of("mock-ericsson.int"), Set.of(443), true, false);
        ConnectorSecurityException http = assertThrows(
                ConnectorSecurityException.class,
                () -> NetworkPolicyEnforcer.validate(URI.create("http://mock-ericsson.int/inventory"), policy));
        assertEquals(ImportFailureCode.NETWORK_POLICY_DENIED, http.failureCode());
        ConnectorSecurityException ssrf = assertThrows(
                ConnectorSecurityException.class,
                () -> NetworkPolicyEnforcer.validate(URI.create("https://169.254.169.254/latest"), policy));
        assertEquals(ImportFailureCode.NETWORK_POLICY_DENIED, ssrf.failureCode());
        ConnectorSecurityException file = assertThrows(
                ConnectorSecurityException.class,
                () -> NetworkPolicyEnforcer.validate(URI.create("file:///tmp/secret"), policy));
        assertEquals(ImportFailureCode.NETWORK_POLICY_DENIED, file.failureCode());
    }

    @Test
    void credentialHandleHidesCanary() {
        CredentialMetadata metadata = new CredentialMetadata(
                "ref", CredentialProviderType.LOCAL_DEVELOPMENT, CredentialType.USERNAME_PASSWORD,
                "v1", java.time.Instant.parse("2026-08-25T10:00:00Z"), null);
        CredentialHandle handle = CredentialHandle.usernamePassword(
                metadata, "reader", LocalDevelopmentCredentialProvider.CANARY_SECRET.toCharArray());
        assertFalse(handle.toString().contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
        assertFalse(handle.equals(CredentialHandle.usernamePassword(
                metadata, "reader", LocalDevelopmentCredentialProvider.CANARY_SECRET.toCharArray())));
    }

    @Test
    void connectorDefinitionRejectsRelativeInventoryPath() {
        assertThrows(IllegalArgumentException.class, () -> new ConnectorDefinition(
                "x", Vendor.ERICSSON, "S", "DEFAULT", "ep", "inventory", "cred", "t", "a", "n",
                AuthenticationMethod.BASIC, CredentialProviderType.LOCAL_DEVELOPMENT,
                Set.of(), false, ConnectorMode.MOCK_SECURE));
    }
}
