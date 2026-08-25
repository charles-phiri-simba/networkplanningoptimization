package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.persist.ConnectorSecurityAuditEventEntity;
import com.simba.snip.npo.persist.ConnectorSecurityAuditEventRepository;
import com.simba.snip.npo.persist.ConnectorSessionEntity;
import com.simba.snip.npo.persist.ConnectorSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConnectorSecurityAuditService {

    private final ConnectorSessionRepository sessionRepository;
    private final ConnectorSecurityAuditEventRepository auditRepository;

    public ConnectorSecurityAuditService(
            ConnectorSessionRepository sessionRepository,
            ConnectorSecurityAuditEventRepository auditRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void openSession(ConnectorSession session) {
        sessionRepository.save(ConnectorSessionEntity.open(
                session.sessionId(),
                session.executionId(),
                session.connectorId(),
                session.sourceSystem(),
                session.credentialRef(),
                session.credentialVersion(),
                session.trustProfileId(),
                session.endpointRef(),
                session.startedAt()
        ));
    }

    @Transactional
    public void closeSession(UUID sessionId, String fingerprint, ConnectorSessionStatus status) {
        sessionRepository.findById(sessionId).ifPresent(entity -> {
            entity.close(Instant.now(), fingerprint, status.name());
            sessionRepository.save(entity);
        });
    }

    @Transactional
    public void record(
            UUID sessionId,
            UUID executionId,
            String connectorId,
            ConnectorSecurityAuditEventType type,
            String credentialRef,
            String credentialVersion,
            String endpointRef,
            String trustProfileId,
            String fingerprint,
            ImportFailureCode failureCode,
            String details
    ) {
        auditRepository.save(ConnectorSecurityAuditEventEntity.create(
                UUID.randomUUID(),
                sessionId,
                executionId,
                connectorId,
                type.name(),
                Instant.now(),
                credentialRef,
                credentialVersion,
                endpointRef,
                trustProfileId,
                fingerprint,
                failureCode == null ? null : failureCode.name(),
                details == null ? type.name() : details
        ));
    }
}
