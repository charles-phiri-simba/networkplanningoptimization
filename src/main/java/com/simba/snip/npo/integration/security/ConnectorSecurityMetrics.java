package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ConnectorSecurityMetrics {

    private static final Logger log = LoggerFactory.getLogger(ConnectorSecurityMetrics.class);

    private final AtomicLong sessionsStarted = new AtomicLong();
    private final AtomicLong sessionsSucceeded = new AtomicLong();
    private final AtomicLong sessionsFailed = new AtomicLong();
    private final AtomicLong credentialResolutionFailures = new AtomicLong();
    private final AtomicLong authenticationFailures = new AtomicLong();
    private final AtomicLong tlsTrustFailures = new AtomicLong();
    private final AtomicLong authorizationDenied = new AtomicLong();
    private final AtomicLong networkPolicyDenied = new AtomicLong();
    private final AtomicLong rotationsObserved = new AtomicLong();

    public void incrementSessionsStarted() {
        sessionsStarted.incrementAndGet();
    }

    public void incrementSessionsSucceeded() {
        sessionsSucceeded.incrementAndGet();
        log.info("connectorSessionSucceeded");
    }

    public void incrementSessionsFailed() {
        sessionsFailed.incrementAndGet();
    }

    public void incrementCredentialResolutionFailures() {
        credentialResolutionFailures.incrementAndGet();
    }

    public void incrementRotationsObserved() {
        rotationsObserved.incrementAndGet();
    }

    public void incrementFailure(ImportFailureCode code) {
        if (code == null) {
            return;
        }
        switch (code) {
            case CONNECTOR_AUTHENTICATION_FAILED -> authenticationFailures.incrementAndGet();
            case TLS_TRUST_FAILED -> tlsTrustFailures.incrementAndGet();
            case CONNECTOR_AUTHORIZATION_DENIED -> authorizationDenied.incrementAndGet();
            case NETWORK_POLICY_DENIED -> networkPolicyDenied.incrementAndGet();
            case CREDENTIAL_RESOLUTION_FAILED -> credentialResolutionFailures.incrementAndGet();
            default -> {
            }
        }
        log.warn("connectorSessionFailed failureCode={}", code.name());
    }

    public long sessionsStarted() {
        return sessionsStarted.get();
    }

    public long sessionsSucceeded() {
        return sessionsSucceeded.get();
    }

    public long sessionsFailed() {
        return sessionsFailed.get();
    }
}
