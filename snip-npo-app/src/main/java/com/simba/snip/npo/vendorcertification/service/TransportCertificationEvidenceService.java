package com.simba.snip.npo.vendorcertification.service;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.vendorcertification.audit.Phase17CertificationAuditService;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

@Service
public class TransportCertificationEvidenceService {

    private final NamedParameterJdbcTemplate jdbc;
    private final Phase17SeparationOfDutiesPolicy sod;
    private final Phase17CertificationAuditService audit;
    private final Clock clock;

    public TransportCertificationEvidenceService(
            NamedParameterJdbcTemplate jdbc,
            Phase17SeparationOfDutiesPolicy sod,
            Phase17CertificationAuditService audit,
            Clock clock
    ) {
        this.jdbc = jdbc;
        this.sod = sod;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public UUID add(
            UUID subjectVersionId,
            UUID subjectId,
            String actor,
            String permission,
            String evidenceType,
            String result,
            String evidenceHash,
            String reference
    ) {
        sod.requirePrincipal(actor, "issuer");
        sod.denyAgentOrMcp(actor);
        if (evidenceHash == null || reference == null || reference.isBlank()) {
            throw new Phase17Exception(Phase17DenialCode.P17_BUNDLE_INVALID, "hash-only evidence rejected");
        }
        if (!evidenceHash.matches("^[0-9a-f]{64}$")) {
            throw new Phase17Exception(Phase17DenialCode.P17_BUNDLE_INVALID, "invalid evidence hash");
        }
        if ("PASS".equals(result) && (actor == null || actor.isBlank() || permission == null || permission.isBlank())) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "PASS requires authenticated certifier");
        }
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(clock.instant());
        jdbc.update(
                "INSERT INTO transport_certification_evidence ("
                        + "evidence_id, evidence_version, certification_subject_type, certification_subject_id, "
                        + "certification_subject_version_id, evidence_type, environment_level, issuer_principal_id, "
                        + "certifier_permission, result, reference, evidence_hash, created_at, effective_at, status) "
                        + "VALUES (:id,1,'TRANSPORT',:sid,:svid,:type,'L0',:actor,:perm,:result,:ref,:hash,:now,:now,'ACTIVE')",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("sid", subjectId)
                        .addValue("svid", subjectVersionId)
                        .addValue("type", evidenceType)
                        .addValue("actor", actor)
                        .addValue("perm", permission)
                        .addValue("result", result)
                        .addValue("ref", reference)
                        .addValue("hash", evidenceHash)
                        .addValue("now", now)
        );
        audit.append("EVIDENCE", id.toString(), subjectVersionId.toString(), "EVIDENCE_ADDED", actor, "{}");
        return id;
    }

    @Transactional
    public void supersede(UUID oldEvidenceId, UUID newEvidenceId, String actor) {
        sod.requirePrincipal(actor, "issuer");
        jdbc.update(
                "UPDATE transport_certification_evidence SET status = 'SUPERSEDED', superseded_by = :neu "
                        + "WHERE evidence_id = :old AND status = 'ACTIVE'",
                new MapSqlParameterSource().addValue("neu", newEvidenceId).addValue("old", oldEvidenceId)
        );
        audit.append("EVIDENCE", oldEvidenceId.toString(), newEvidenceId.toString(), "EVIDENCE_SUPERSEDED", actor, "{}");
    }
}
