package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;

@Component
public class SecureConnectorClientFactory {

    private final ConnectorRegistry registry;
    private final ConnectorEndpointRegistry endpoints;
    private final CredentialProviderRegistry credentialProviders;
    private final ConnectorSslContextFactory sslContextFactory;
    private final ConnectorTrustMaterialProvider trustMaterialProvider;
    private final ConnectorSecurityAuditService auditService;
    private final ConnectorSecurityMetrics metrics;

    public SecureConnectorClientFactory(
            ConnectorRegistry registry,
            ConnectorEndpointRegistry endpoints,
            CredentialProviderRegistry credentialProviders,
            ConnectorSslContextFactory sslContextFactory,
            ConnectorTrustMaterialProvider trustMaterialProvider,
            ConnectorSecurityAuditService auditService,
            ConnectorSecurityMetrics metrics
    ) {
        this.registry = registry;
        this.endpoints = endpoints;
        this.credentialProviders = credentialProviders;
        this.sslContextFactory = sslContextFactory;
        this.trustMaterialProvider = trustMaterialProvider;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    public OpenSession open(ConnectorDefinition definition, UUID executionId) {
        metrics.incrementSessionsStarted();
        UUID sessionId = UUID.randomUUID();
        java.time.Instant startedAt = java.time.Instant.now();
        auditService.openSession(new ConnectorSession(
                sessionId,
                executionId,
                definition.connectorId(),
                definition.sourceSystem(),
                definition.credentialRef(),
                null,
                definition.trustProfileId(),
                definition.endpointRef(),
                null,
                startedAt,
                null,
                ConnectorSessionStatus.OPEN
        ));
        auditService.record(
                sessionId,
                executionId,
                definition.connectorId(),
                ConnectorSecurityAuditEventType.SESSION_REQUESTED,
                definition.credentialRef(),
                null,
                definition.endpointRef(),
                definition.trustProfileId(),
                null,
                null,
                "session requested"
        );
        if (!definition.enabled()) {
            fail(sessionId, executionId, definition, ImportFailureCode.CONNECTOR_DISABLED, "connector is disabled", false);
            throw new ConnectorSecurityException(ImportFailureCode.CONNECTOR_DISABLED, "connector is disabled");
        }
        ConnectorAuthorizationProfile authorization = registry.authorization(definition.authorizationProfileId());
        if (!authorization.allowsAll(definition.requiredCapabilities())) {
            auditService.record(
                    sessionId,
                    executionId,
                    definition.connectorId(),
                    ConnectorSecurityAuditEventType.AUTHORIZATION_DENIED,
                    definition.credentialRef(),
                    null,
                    definition.endpointRef(),
                    definition.trustProfileId(),
                    null,
                    ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED,
                    "required capability is not allowed"
            );
            fail(sessionId, executionId, definition, ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED,
                    "required capability is not allowed", false);
            throw new ConnectorSecurityException(
                    ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED, "required capability is not allowed");
        }
        ConnectorIdentity identity = definition.identity();
        if (!definition.credentialRef().equals(identity.credentialRef())) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential reference is not bound to this identity");
        }
        CredentialHandle handle;
        try {
            handle = credentialProviders.require(definition.credentialProvider()).resolve(identity);
            if (!definition.credentialRef().equals(handle.metadata().credentialRef())) {
                throw new ConnectorSecurityException(
                        ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential reference is not bound to this identity");
            }
        } catch (ConnectorSecurityException ex) {
            fail(sessionId, executionId, definition, ex.failureCode(), "credential resolution failed", true);
            throw ex;
        }
        auditService.record(
                sessionId,
                executionId,
                definition.connectorId(),
                ConnectorSecurityAuditEventType.CREDENTIAL_RESOLVED,
                handle.metadata().credentialRef(),
                handle.metadata().versionIdentifier(),
                definition.endpointRef(),
                definition.trustProfileId(),
                null,
                null,
                "credential resolved provider=" + handle.metadata().provider()
                        + " version=" + handle.metadata().versionIdentifier()
        );
        ConnectorEndpoint endpoint = endpoints.require(definition.endpointRef());
        ConnectorNetworkPolicy policy = registry.networkPolicy(definition.networkPolicyId());
        URI base = NetworkPolicyEnforcer.validate(endpoint.baseUri(), policy);
        URI inventory = base.resolve(definition.inventoryPath());
        NetworkPolicyEnforcer.validate(inventory, policy);
        auditService.record(
                sessionId,
                executionId,
                definition.connectorId(),
                ConnectorSecurityAuditEventType.NETWORK_POLICY_VALIDATED,
                handle.metadata().credentialRef(),
                handle.metadata().versionIdentifier(),
                definition.endpointRef(),
                definition.trustProfileId(),
                null,
                null,
                "network policy validated"
        );
        ConnectorTrustProfile trust = trustMaterialProvider.resolve(registry.trust(definition.trustProfileId()));
        CredentialHandle clientCert = null;
        if (definition.authenticationMethod() == AuthenticationMethod.BASIC_PLUS_MTLS
                || definition.authenticationMethod() == AuthenticationMethod.MTLS) {
            if (handle.certificateDerCopy() == null) {
                fail(sessionId, executionId, definition, ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED,
                        "client certificate is required", false);
                throw new ConnectorSecurityException(
                        ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED, "client certificate is required");
            }
            clientCert = handle;
        }
        ConnectorSslContextFactory.PreparedSsl ssl = sslContextFactory.prepare(trust, clientCert);
        char[] password = handle.secretCopy();
        String username = handle.username();
        if (definition.authenticationMethod() == AuthenticationMethod.MTLS) {
            username = null;
            password = null;
        }
        ReadOnlyVendorClient client = new JavaHttpReadOnlyVendorClient(
                inventory, ssl.sslContext(), ssl.capturingTrustManager(), username, password);
        ConnectorSecurityContext context = new ConnectorSecurityContext(
                identity, handle, authorization, trust, policy);
        ConnectorSession session = new ConnectorSession(
                sessionId,
                executionId,
                definition.connectorId(),
                definition.sourceSystem(),
                handle.metadata().credentialRef(),
                handle.metadata().versionIdentifier(),
                definition.trustProfileId(),
                definition.endpointRef(),
                null,
                startedAt,
                null,
                ConnectorSessionStatus.OPEN
        );
        return new OpenSession(session, context, client, handle);
    }

    public void complete(OpenSession open, boolean succeeded, ImportFailureCode failureCode) {
        String fingerprint = open.client().serverCertificateFingerprint();
        if (succeeded) {
            auditService.record(
                    open.session().sessionId(),
                    open.session().executionId(),
                    open.session().connectorId(),
                    ConnectorSecurityAuditEventType.TLS_VALIDATED,
                    open.session().credentialRef(),
                    open.session().credentialVersion(),
                    open.session().endpointRef(),
                    open.session().trustProfileId(),
                    fingerprint,
                    null,
                    "tls validated"
            );
            auditService.record(
                    open.session().sessionId(),
                    open.session().executionId(),
                    open.session().connectorId(),
                    ConnectorSecurityAuditEventType.AUTHENTICATION_SUCCEEDED,
                    open.session().credentialRef(),
                    open.session().credentialVersion(),
                    open.session().endpointRef(),
                    open.session().trustProfileId(),
                    fingerprint,
                    null,
                    "authentication succeeded"
            );
            auditService.record(
                    open.session().sessionId(),
                    open.session().executionId(),
                    open.session().connectorId(),
                    ConnectorSecurityAuditEventType.SESSION_COMPLETED,
                    open.session().credentialRef(),
                    open.session().credentialVersion(),
                    open.session().endpointRef(),
                    open.session().trustProfileId(),
                    fingerprint,
                    null,
                    "session completed"
            );
            auditService.closeSession(open.session().sessionId(), fingerprint, ConnectorSessionStatus.COMPLETED);
            metrics.incrementSessionsSucceeded();
        } else {
            ConnectorSecurityAuditEventType type = failureCode == ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED
                    ? ConnectorSecurityAuditEventType.AUTHENTICATION_FAILED
                    : ConnectorSecurityAuditEventType.SESSION_FAILED;
            auditService.record(
                    open.session().sessionId(),
                    open.session().executionId(),
                    open.session().connectorId(),
                    type,
                    open.session().credentialRef(),
                    open.session().credentialVersion(),
                    open.session().endpointRef(),
                    open.session().trustProfileId(),
                    fingerprint,
                    failureCode,
                    "session failed"
            );
            auditService.closeSession(open.session().sessionId(), fingerprint, ConnectorSessionStatus.FAILED);
            metrics.incrementSessionsFailed();
            metrics.incrementFailure(failureCode);
        }
        open.handle().clear();
        open.client().close();
    }

    private void fail(
            UUID sessionId,
            UUID executionId,
            ConnectorDefinition definition,
            ImportFailureCode code,
            String detail,
            boolean credentialFailure
    ) {
        ConnectorSecurityAuditEventType type = code == ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED
                ? ConnectorSecurityAuditEventType.AUTHENTICATION_FAILED
                : ConnectorSecurityAuditEventType.SESSION_FAILED;
        auditService.record(
                sessionId,
                executionId,
                definition.connectorId(),
                type,
                definition.credentialRef(),
                null,
                definition.endpointRef(),
                definition.trustProfileId(),
                null,
                code,
                detail
        );
        metrics.incrementSessionsFailed();
        metrics.incrementFailure(code);
        if (credentialFailure) {
            metrics.incrementCredentialResolutionFailures();
        }
    }

    public record OpenSession(
            ConnectorSession session,
            ConnectorSecurityContext context,
            ReadOnlyVendorClient client,
            CredentialHandle handle
    ) {
    }
}
