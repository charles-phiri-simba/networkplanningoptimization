package com.simba.snip.npo.targetonboarding.service;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import com.simba.snip.npo.vendorcertification.audit.Phase17CertificationAuditService;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionTargetOnboardingService {

    private final Phase17SeparationOfDutiesPolicy sod;
    private final NamedParameterJdbcTemplate jdbc;
    private final Phase17CertificationAuditService audit;
    private final Clock clock;

    public ProductionTargetOnboardingService(Phase17SeparationOfDutiesPolicy sod) {
        this(sod, null, null, Clock.systemUTC());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProductionTargetOnboardingService(
            Phase17SeparationOfDutiesPolicy sod,
            NamedParameterJdbcTemplate jdbc,
            Phase17CertificationAuditService audit,
            Clock clock
    ) {
        this.sod = sod;
        this.jdbc = jdbc;
        this.audit = audit;
        this.clock = clock;
    }

    public void requireCreateReviewApproveDistinct(String creator, String reviewer, String approver, String executor) {
        sod.requirePrincipal(creator, "creator");
        sod.requirePrincipal(reviewer, "reviewer");
        sod.requirePrincipal(approver, "approver");
        sod.requireDistinct(creator, reviewer, "CREATE != REVIEW");
        sod.requireDistinct(reviewer, approver, "REVIEW != APPROVE");
        sod.requireDistinct(creator, approver, "CREATE != APPROVE");
        if (executor != null && !executor.isBlank()) {
            sod.requireDistinct(creator, executor, "executor must not create own onboarding");
            sod.requireDistinct(reviewer, executor, "executor must not review own onboarding");
            sod.requireDistinct(approver, executor, "executor must not approve own onboarding");
        }
    }

    public void denyStandingL4(String requestedLevel) {
        if ("L4".equals(requestedLevel) || "PRODUCTION_REGISTERED".equals(requestedLevel)
                || "APPROVED".equals(requestedLevel)) {
            throw new Phase17Exception(Phase17DenialCode.P17_LEVEL3_NOT_LEVEL4, "L4 is not persistable");
        }
    }

    public void requirePermission(String held, String required) {
        sod.requirePermission(held, required);
    }

    @Transactional
    public Map<String, Object> create(CreateCommand command) {
        requireJdbc();
        denyStandingL4(command.certificationLevel());
        if (command.certificationLevel() == null
                || !command.certificationLevel().matches("L[0-3]")) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "invalid certification level");
        }
        if (command.productionTargetId() == null || command.productionTargetId().isBlank()) {
            throw new Phase17Exception(Phase17DenialCode.P17_TARGET_NOT_ONBOARDED, "productionTargetId required");
        }
        if (command.bundleVersionId() == null || command.interfaceDefinitionVersionId() == null
                || command.transportProfileVersionId() == null || command.endpointProfileVersionId() == null
                || command.credentialProfileVersionId() == null || command.tlsProfileVersionId() == null
                || command.networkPolicyProfileVersionId() == null || command.capabilityCertVersionId() == null
                || command.securityCertVersionId() == null || command.artifactDigest() == null) {
            throw new Phase17Exception(Phase17DenialCode.P17_BUNDLE_INVALID, "onboarding version bindings required");
        }
        Instant now = clock.instant();
        UUID onboardingId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        String actor = command.actorPrincipalId();
        jdbc.update(
                "INSERT INTO production_target_onboarding ("
                        + "onboarding_id, onboarding_version_id, production_target_id, status, certification_level, "
                        + "created_by, reviewed_by, approved_by, created_at, updated_at) "
                        + "VALUES (:id, NULL, :target, 'DRAFT', :level, :actor, NULL, NULL, :now, :now)",
                new MapSqlParameterSource()
                        .addValue("id", onboardingId)
                        .addValue("target", command.productionTargetId())
                        .addValue("level", command.certificationLevel())
                        .addValue("actor", actor)
                        .addValue("now", Timestamp.from(now))
        );
        String contentDigest = Sha256Hex.hash(onboardingId + "|" + versionId + "|" + command.bundleVersionId());
        jdbc.update(
                "INSERT INTO production_target_onboarding_version ("
                        + "onboarding_version_id, onboarding_id, version_no, content_digest, production_target_id, "
                        + "vendor, platform, vendor_software_version, interface_definition_version_id, "
                        + "transport_profile_version_id, artifact_digest, capability_cert_version_id, "
                        + "security_cert_version_id, credential_profile_version_id, tls_profile_version_id, "
                        + "network_policy_profile_version_id, endpoint_profile_version_id, bundle_version_id, "
                        + "change_control_policy, verification_policy, rollback_policy, monitoring_profile, "
                        + "support_owner, environment, region, network_domain, expires_at, created_at, created_by) "
                        + "VALUES (:vid,:oid,1,:digest,:target,'ERICSSON','ENM',:vsv,:iface,:profile,:artifact,"
                        + ":cap,:sec,:cred,:tls,:net,:ep,:bundle,'p16-change','p16-verify','p16-rollback','lab',"
                        + ":actor,:env,'test','RAN',:exp,:now,:actor)",
                new MapSqlParameterSource()
                        .addValue("vid", versionId)
                        .addValue("oid", onboardingId)
                        .addValue("digest", contentDigest)
                        .addValue("target", command.productionTargetId())
                        .addValue("vsv", command.vendorSoftwareVersion() == null ? "UNRESOLVED" : command.vendorSoftwareVersion())
                        .addValue("iface", command.interfaceDefinitionVersionId())
                        .addValue("profile", command.transportProfileVersionId())
                        .addValue("artifact", command.artifactDigest())
                        .addValue("cap", command.capabilityCertVersionId())
                        .addValue("sec", command.securityCertVersionId())
                        .addValue("cred", command.credentialProfileVersionId())
                        .addValue("tls", command.tlsProfileVersionId())
                        .addValue("net", command.networkPolicyProfileVersionId())
                        .addValue("ep", command.endpointProfileVersionId())
                        .addValue("bundle", command.bundleVersionId())
                        .addValue("actor", actor)
                        .addValue("env", command.environment() == null ? "LAB" : command.environment())
                        .addValue("exp", Timestamp.from(now.plusSeconds(86400L * 30)))
                        .addValue("now", Timestamp.from(now))
        );
        jdbc.update(
                "UPDATE production_target_onboarding SET onboarding_version_id = :vid WHERE onboarding_id = :id",
                new MapSqlParameterSource("vid", versionId).addValue("id", onboardingId)
        );
        if (audit != null) {
            audit.append("ONBOARDING", onboardingId.toString(), versionId.toString(),
                    "ONBOARD_CREATED", actor, "{\"status\":\"DRAFT\"}");
        }
        return Map.of("status", "DRAFT", "id", onboardingId.toString(), "onboardingVersionId", versionId.toString());
    }

    @Transactional
    public Map<String, String> review(UUID onboardingId, String reviewerPrincipalId) {
        requireJdbc();
        Map<String, Object> row = load(onboardingId);
        String createdBy = (String) row.get("created_by");
        sod.requireDistinct(createdBy, reviewerPrincipalId, "CREATE != REVIEW");
        if (!"DRAFT".equals(row.get("status")) && !"IN_REVIEW".equals(row.get("status"))) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "onboarding not reviewable");
        }
        jdbc.update(
                "UPDATE production_target_onboarding SET status = 'IN_REVIEW', reviewed_by = :reviewer, updated_at = :now "
                        + "WHERE onboarding_id = :id",
                new MapSqlParameterSource()
                        .addValue("reviewer", reviewerPrincipalId)
                        .addValue("now", Timestamp.from(clock.instant()))
                        .addValue("id", onboardingId)
        );
        if (audit != null) {
            audit.append("ONBOARDING", onboardingId.toString(), String.valueOf(row.get("onboarding_version_id")),
                    "ONBOARD_REVIEWED", reviewerPrincipalId, "{\"status\":\"IN_REVIEW\"}");
        }
        return Map.of("status", "IN_REVIEW", "id", onboardingId.toString());
    }

    @Transactional
    public Map<String, String> approve(UUID onboardingId, String approverPrincipalId) {
        requireJdbc();
        Map<String, Object> row = load(onboardingId);
        String createdBy = (String) row.get("created_by");
        String reviewedBy = (String) row.get("reviewed_by");
        if (reviewedBy == null || reviewedBy.isBlank()) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "review required before approve");
        }
        requireCreateReviewApproveDistinct(createdBy, reviewedBy, approverPrincipalId, null);
        if (!"IN_REVIEW".equals(row.get("status"))) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "onboarding not approvable");
        }
        UUID versionId = (UUID) row.get("onboarding_version_id");
        Integer currentBundle = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_target_onboarding_version ptov "
                        + "JOIN transport_certification_bundle tcb ON tcb.bundle_version_id = ptov.bundle_version_id "
                        + "JOIN vendor_write_transport_profile p ON p.transport_profile_version_id = ptov.transport_profile_version_id "
                        + "WHERE ptov.onboarding_version_id = :vid AND tcb.status = 'ACTIVE' AND p.status = 'ACTIVE'",
                new MapSqlParameterSource("vid", versionId),
                Integer.class
        );
        if (currentBundle == null || currentBundle < 1) {
            throw new Phase17Exception(Phase17DenialCode.P17_CERTIFICATION_STALE, "current certification binding required");
        }
        jdbc.update(
                "UPDATE production_target_onboarding SET status = 'APPROVED', approved_by = :approver, updated_at = :now "
                        + "WHERE onboarding_id = :id",
                new MapSqlParameterSource()
                        .addValue("approver", approverPrincipalId)
                        .addValue("now", Timestamp.from(clock.instant()))
                        .addValue("id", onboardingId)
        );
        if (audit != null) {
            audit.append("ONBOARDING", onboardingId.toString(), versionId.toString(),
                    "ONBOARD_APPROVED", approverPrincipalId, "{\"status\":\"APPROVED\"}");
        }
        return Map.of("status", "APPROVED", "id", onboardingId.toString());
    }

    public Map<String, Object> load(UUID onboardingId) {
        requireJdbc();
        return jdbc.query(
                "SELECT onboarding_id, onboarding_version_id, production_target_id, status, certification_level, "
                        + "created_by, reviewed_by, approved_by FROM production_target_onboarding WHERE onboarding_id = :id",
                new MapSqlParameterSource("id", onboardingId),
                rs -> {
                    if (!rs.next()) {
                        throw new Phase17Exception(Phase17DenialCode.P17_TARGET_NOT_ONBOARDED, "onboarding not found");
                    }
                    return Map.<String, Object>of(
                            "onboarding_id", rs.getObject("onboarding_id"),
                            "onboarding_version_id", rs.getObject("onboarding_version_id"),
                            "production_target_id", rs.getString("production_target_id"),
                            "status", rs.getString("status"),
                            "certification_level", rs.getString("certification_level"),
                            "created_by", rs.getString("created_by"),
                            "reviewed_by", rs.getString("reviewed_by") == null ? "" : rs.getString("reviewed_by"),
                            "approved_by", rs.getString("approved_by") == null ? "" : rs.getString("approved_by")
                    );
                }
        );
    }

    private void requireJdbc() {
        if (jdbc == null) {
            throw new IllegalStateException("durable onboarding requires JDBC");
        }
    }

    public record CreateCommand(
            String actorPrincipalId,
            String productionTargetId,
            String certificationLevel,
            UUID interfaceDefinitionVersionId,
            UUID transportProfileVersionId,
            String artifactDigest,
            UUID capabilityCertVersionId,
            UUID securityCertVersionId,
            UUID credentialProfileVersionId,
            UUID tlsProfileVersionId,
            UUID networkPolicyProfileVersionId,
            UUID endpointProfileVersionId,
            UUID bundleVersionId,
            String vendorSoftwareVersion,
            String environment
    ) {
    }
}
