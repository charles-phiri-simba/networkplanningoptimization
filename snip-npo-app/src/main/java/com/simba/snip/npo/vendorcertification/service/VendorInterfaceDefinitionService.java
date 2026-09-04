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
public class VendorInterfaceDefinitionService {

    private final NamedParameterJdbcTemplate jdbc;
    private final Phase17SeparationOfDutiesPolicy sod;
    private final Phase17CertificationAuditService audit;
    private final Clock clock;

    public VendorInterfaceDefinitionService(
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
    public UUID createDraft(
            String actor,
            String contentDigest,
            String documentationReference,
            String documentationVersion
    ) {
        sod.requirePrincipal(actor, "creator");
        sod.denyAgentOrMcp(actor);
        if (contentDigest != null && !contentDigest.matches("^[0-9a-f]{64}$")) {
            throw new Phase17Exception(Phase17DenialCode.P17_INTERFACE_UNRESOLVED, "invalid digest");
        }
        UUID versionId = UUID.randomUUID();
        UUID logicalId = UUID.randomUUID();
        Timestamp now = Timestamp.from(clock.instant());
        jdbc.update(
                "INSERT INTO vendor_interface_definition ("
                        + "interface_definition_version_id, interface_definition_id, version_no, content_digest, "
                        + "vendor, platform, vendor_product_version_predicate, interface_type_category, "
                        + "documentation_reference, documentation_version, documentation_status, status, "
                        + "effective_from, created_at, created_by, updated_at) "
                        + "VALUES (:vid,:lid,1,:digest,'ERICSSON','ENM','EXPLICIT','UNRESOLVED',"
                        + ":docRef,:docVer,'ACTIVE','DRAFT',:now,:now,:actor,:now)",
                new MapSqlParameterSource()
                        .addValue("vid", versionId)
                        .addValue("lid", logicalId)
                        .addValue("digest", contentDigest)
                        .addValue("docRef", documentationReference)
                        .addValue("docVer", documentationVersion)
                        .addValue("now", now)
                        .addValue("actor", actor)
        );
        audit.append("INTERFACE", logicalId.toString(), versionId.toString(), "INTERFACE_CREATED", actor, "{}");
        return versionId;
    }
}
