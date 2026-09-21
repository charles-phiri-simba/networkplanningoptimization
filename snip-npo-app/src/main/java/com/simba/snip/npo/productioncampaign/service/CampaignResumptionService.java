package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.domain.ActorType;
import com.simba.snip.npo.productioncampaign.domain.AuthenticatedActor;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CampaignResumptionService {

    private static final Set<String> FORWARD_DESTINATIONS = Set.of(
            "READY", "CANARY_ACTIVE", "CANARY_OBSERVING", "CANARY_VERIFIED",
            "PAUSED_FOR_RELEASE", "COHORT_ACTIVE", "COHORT_OBSERVING", "COHORT_VERIFIED",
            "FINAL_OBSERVATION", "COMPLETED"
    );
    private static final Set<String> OBSERVING_STATES = Set.of(
            "CANARY_OBSERVING", "COHORT_OBSERVING", "FINAL_OBSERVATION"
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final ForwardProgressionClosureService closureService;
    private final CampaignAuditService auditService;
    private final CampaignObservationService observationService;

    public CampaignResumptionService(
            NamedParameterJdbcTemplate jdbc,
            ForwardProgressionClosureService closureService,
            CampaignAuditService auditService,
            CampaignObservationService observationService
    ) {
        this.jdbc = jdbc;
        this.closureService = closureService;
        this.auditService = auditService;
        this.observationService = observationService;
    }

    @Transactional
    public void request(UUID campaignId, AuthenticatedActor actor) {
        if (actor == null || !actor.authenticated()
                || (!actor.authorities().contains(CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW)
                && !actor.authorities().contains(CampaignPermission.CAMPAIGN_PAUSE))) {
            throw new CampaignException(
                    ProductionReasonCode.UNAUTHENTICATED_HUMAN_ACTOR,
                    "resumption request requires CAMPAIGN_RESUMPTION_REVIEW or PAUSE holder"
            );
        }
        requireHuman(actor);
        lockCampaign(campaignId);
        requireSuspended(campaignId);
        UUID revisionId = currentRevisionId(campaignId);
        int inserted = jdbc.update(
                """
                INSERT INTO campaign_resumption (
                    resumption_id, campaign_id, revision_id, state, requester_principal_id, created_at, updated_at)
                VALUES (:rid, :id, :revisionId, 'REQUESTED', :actor, :now, :now)
                """,
                params(campaignId, actor.actorId()).addValue("rid", UUID.randomUUID()).addValue("revisionId", revisionId)
        );
        if (inserted != 1) {
            throw new CampaignException(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, "resumption request was not persisted");
        }
        auditService.append(campaignId, "RESUMPTION_REQUESTED", actor.actorId(), Map.of("state", "REQUESTED"));
    }

    @Transactional
    public void startReview(UUID campaignId, AuthenticatedActor actor) {
        requirePermission(actor, CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW);
        lockCampaign(campaignId);
        int updated = jdbc.update(
                """
                UPDATE campaign_resumption
                   SET state = 'UNDER_REVIEW', reviewer_principal_id = :actor, updated_at = :now
                 WHERE campaign_id = :id AND state = 'REQUESTED'
                """,
                params(campaignId, actor.actorId())
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "resumption START_REVIEW expected REQUESTED"
            );
        }
        auditService.append(campaignId, "RESUMPTION_UNDER_REVIEW", actor.actorId(), Map.of());
    }

    @Transactional
    public void approveReview(UUID campaignId, AuthenticatedActor actor) {
        requirePermission(actor, CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW);
        lockCampaign(campaignId);
        assertFingerprintCurrent(campaignId);
        int updated = jdbc.update(
                """
                UPDATE campaign_resumption
                   SET state = 'REVIEWED', reviewer_principal_id = :actor, updated_at = :now
                 WHERE campaign_id = :id AND state = 'UNDER_REVIEW'
                """,
                params(campaignId, actor.actorId())
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "resumption REVIEW_APPROVE expected UNDER_REVIEW"
            );
        }
        auditService.append(campaignId, "RESUMPTION_REVIEWED", actor.actorId(), Map.of());
    }

    @Transactional
    public void rejectReview(UUID campaignId, AuthenticatedActor actor) {
        requirePermission(actor, CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW);
        lockCampaign(campaignId);
        int updated = jdbc.update(
                """
                UPDATE campaign_resumption
                   SET state = 'REJECTED', reviewer_principal_id = :actor, updated_at = :now
                 WHERE campaign_id = :id AND state = 'UNDER_REVIEW'
                """,
                params(campaignId, actor.actorId())
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "resumption REVIEW_REJECT expected UNDER_REVIEW"
            );
        }
        auditService.append(campaignId, "RESUMPTION_REJECTED", actor.actorId(), Map.of());
    }

    @Transactional
    public void authorize(UUID campaignId, AuthenticatedActor actor) {
        requirePermission(actor, CampaignPermission.CAMPAIGN_RESUMPTION_AUTHORIZE);
        lockCampaign(campaignId);
        Integer sod = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_resumption
                 WHERE campaign_id = :id AND state = 'REVIEWED'
                   AND reviewer_principal_id IS DISTINCT FROM :actor
                """,
                params(campaignId, actor.actorId()),
                Integer.class
        );
        if (sod == null || sod != 1) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                    "resumption reviewer must differ from resumption authorizer"
            );
        }
        assertFullCurrentness(campaignId);
        int updated = jdbc.update(
                """
                UPDATE campaign_resumption
                   SET state = 'AUTHORIZED', authorizer_principal_id = :actor, updated_at = :now
                 WHERE campaign_id = :id AND state = 'REVIEWED'
                """,
                params(campaignId, actor.actorId())
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "resumption AUTHORIZE expected REVIEWED"
            );
        }
        auditService.append(campaignId, "RESUMPTION_AUTHORIZED", actor.actorId(), Map.of());
    }

    @Transactional
    public String makeEffective(UUID campaignId, AuthenticatedActor actor) {
        if (actor == null || actor.actorType() != ActorType.SYSTEM) {
            throw new CampaignException(
                    ProductionReasonCode.SYSTEM_TRANSITION_DENIED,
                    "MAKE_EFFECTIVE is a SYSTEM transition; humans cannot bypass AUTHORIZED"
            );
        }
        jdbc.queryForObject(
                "SELECT campaign_id FROM production_change_campaign WHERE campaign_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", campaignId),
                UUID.class
        );
        Integer authorized = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_resumption
                 WHERE campaign_id = :id AND state = 'AUTHORIZED'
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if (authorized == null || authorized != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "MAKE_EFFECTIVE requires AUTHORIZED resumption"
            );
        }
        denyUnresolvedMayHaveSent(campaignId);
        String closure = jdbc.queryForObject(
                "SELECT state FROM campaign_forward_progression_closure WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                String.class
        );
        if ("CLOSED".equals(closure)) {
            throw new CampaignException(
                    ProductionReasonCode.FORWARD_PROGRESSION_CLOSED,
                    "CLOSED prohibits any forward-progressing resumption destination"
            );
        }
        Map<String, Object> campaign = jdbc.queryForMap(
                """
                SELECT state, fingerprint, enabled, abort_state
                  FROM production_change_campaign WHERE campaign_id = :id
                """,
                new MapSqlParameterSource("id", campaignId)
        );
        if (!Boolean.TRUE.equals(campaign.get("enabled"))
                || "STALE".equals(String.valueOf(campaign.get("state")))
                || "ABORTED".equals(String.valueOf(campaign.get("abort_state")))) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_STALE,
                    "stale campaign authority cannot be repaired by resumption"
            );
        }
        assertFullCurrentness(campaignId);
        Map<String, Object> suspension = jdbc.queryForMap(
                """
                SELECT pre_state, suspension_type FROM campaign_suspension
                 WHERE campaign_id = :id
                 ORDER BY created_at DESC
                 LIMIT 1
                """,
                new MapSqlParameterSource("id", campaignId)
        );
        String preState = String.valueOf(suspension.get("pre_state"));
        boolean unusedReleaseStale = unusedReleaseStale(campaignId);
        boolean releaseConsumed = releaseConsumed(campaignId);
        boolean canaryTerminallyAccounted = itemsTerminallyAccounted(campaignId, "CANARY");
        boolean cohortTerminallyAccounted = itemsTerminallyAccounted(campaignId, "STANDARD");
        boolean observationRequired = OBSERVING_STATES.contains(preState)
                || ("CANARY_ACTIVE".equals(preState) && releaseConsumed && canaryTerminallyAccounted)
                || ("COHORT_ACTIVE".equals(preState) && releaseConsumed && cohortTerminallyAccounted);
        if (observationRequired && !observationBoundaryStillValid(campaignId)) {
            throw new CampaignException(
                    ProductionReasonCode.NEW_OBSERVATION_BOUNDARY_REQUIRED,
                    "existing observation boundary is no longer valid for resumption"
            );
        }
        String destination = destination(
                preState,
                unusedReleaseStale,
                releaseConsumed,
                canaryTerminallyAccounted,
                cohortTerminallyAccounted
        );
        if (FORWARD_DESTINATIONS.contains(destination)) {
            closureService.assertOpen(campaignId);
        }
        Integer activeBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_cohort_release WHERE campaign_id = :id AND state = 'ACTIVE'",
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        String closureBefore = closure;
        int updated = jdbc.update(
                """
                UPDATE production_change_campaign
                   SET state = :dest, updated_at = :now, version = version + 1
                 WHERE campaign_id = :id AND state = 'SUSPENDED'
                """,
                new MapSqlParameterSource()
                        .addValue("id", campaignId)
                        .addValue("dest", destination)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "resumption EFFECTIVE requires durable SUSPENDED campaign"
            );
        }
        jdbc.update(
                """
                UPDATE campaign_resumption
                   SET state = 'EFFECTIVE', destination_state = :dest, updated_at = :now
                 WHERE campaign_id = :id AND state = 'AUTHORIZED'
                """,
                new MapSqlParameterSource()
                        .addValue("id", campaignId)
                        .addValue("dest", destination)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
        Integer activeAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_cohort_release WHERE campaign_id = :id AND state = 'ACTIVE'",
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if (activeAfter != null && activeBefore != null && activeAfter > activeBefore) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "resumption MUST NOT release a cohort"
            );
        }
        String closureAfter = jdbc.queryForObject(
                "SELECT state FROM campaign_forward_progression_closure WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                String.class
        );
        if ("CLOSED".equals(closureBefore) && !"CLOSED".equals(closureAfter)) {
            throw new CampaignException(
                    ProductionReasonCode.FORWARD_PROGRESSION_CLOSED,
                    "resumption MUST NOT reopen forward closure"
            );
        }
        auditService.append(campaignId, "RESUMPTION_EFFECTIVE", actor.actorId(), Map.of(
                "destination", destination,
                "releaseResurrected", false,
                "newReleaseMandatory", unusedReleaseStale && Set.of("CANARY_ACTIVE", "COHORT_ACTIVE", "PAUSED_FOR_RELEASE").contains(preState)
        ));
        return destination;
    }

    private void denyUnresolvedMayHaveSent(UUID campaignId) {
        Integer unresolved = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_mutation_slot
                 WHERE campaign_id = :id AND state = 'HELD_MAY_HAVE_SENT'
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        Integer unresolvedItems = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_execution_item
                 WHERE campaign_id = :id AND state IN ('MAY_HAVE_SENT', 'OUTCOME_UNRESOLVED')
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if ((unresolved != null && unresolved > 0) || (unresolvedItems != null && unresolvedItems > 0)) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_OUTCOME_UNRESOLVED,
                    "unresolved MAY_HAVE_SENT cannot be bypassed"
            );
        }
    }

    private void assertFullCurrentness(UUID campaignId) {
        assertFingerprintCurrent(campaignId);
        Map<String, Object> campaign = jdbc.queryForMap(
                """
                SELECT fingerprint, enabled, abort_state, production_target_id, current_revision_number
                  FROM production_change_campaign WHERE campaign_id = :id
                """,
                new MapSqlParameterSource("id", campaignId)
        );
        if (!Boolean.TRUE.equals(campaign.get("enabled"))
                || "ABORTED".equals(String.valueOf(campaign.get("abort_state")))) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "campaign currentness failed");
        }
        Integer authGen = jdbc.queryForObject(
                """
                SELECT r.authorization_generation
                  FROM campaign_revision r
                  JOIN production_change_campaign c ON c.campaign_id = r.campaign_id
                   AND r.revision_number = c.current_revision_number
                 WHERE c.campaign_id = :id
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if (authGen == null || authGen < 1) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "stale authorization generation");
        }
        String targetId = String.valueOf(campaign.get("production_target_id"));
        Integer approved = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_target_onboarding
                 WHERE production_target_id = :target AND status = 'APPROVED'
                """,
                new MapSqlParameterSource("target", targetId),
                Integer.class
        );
        if (approved == null || approved < 1) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "P17 onboarding is not current");
        }
        Integer revoked = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_target_onboarding
                 WHERE production_target_id = :target AND status IN ('REVOKED', 'SUSPENDED', 'INVALID')
                """,
                new MapSqlParameterSource("target", targetId),
                Integer.class
        );
        if (revoked != null && revoked > 0) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "P17 onboarding is revoked or stale");
        }
        Integer certRevoked = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_target_certification
                 WHERE production_target_id = :target AND status IN ('REVOKED', 'EXPIRED', 'INVALID', 'SUSPENDED')
                """,
                new MapSqlParameterSource("target", targetId),
                Integer.class
        );
        Integer certCurrent = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_target_certification
                 WHERE production_target_id = :target AND status = 'CURRENT'
                """,
                new MapSqlParameterSource("target", targetId),
                Integer.class
        );
        if ((certRevoked != null && certRevoked > 0) || certCurrent == null || certCurrent < 1) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "P17 transport certification is not current");
        }
    }

    private void assertFingerprintCurrent(UUID campaignId) {
        Integer mismatch = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_change_campaign c
                  JOIN campaign_revision r ON r.campaign_id = c.campaign_id
                   AND r.revision_number = c.current_revision_number
                 WHERE c.campaign_id = :id
                   AND c.fingerprint IS DISTINCT FROM r.fingerprint
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if (mismatch != null && mismatch > 0) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "stale campaign fingerprint or revision");
        }
    }

    private boolean observationBoundaryStillValid(UUID campaignId) {
        try {
            observationService.assertCurrentHealthyAndSafe(campaignId);
            return true;
        } catch (CampaignException ex) {
            return false;
        }
    }

    private boolean unusedReleaseStale(UUID campaignId) {
        Integer stale = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_cohort_release
                 WHERE campaign_id = :id AND state = 'STALE'
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        Integer active = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_cohort_release
                 WHERE campaign_id = :id AND state = 'ACTIVE'
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        return stale != null && stale > 0 && (active == null || active == 0);
    }

    private boolean releaseConsumed(UUID campaignId) {
        Integer consumed = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_cohort_release
                 WHERE campaign_id = :id AND state = 'CONSUMED'
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        Integer active = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_cohort_release
                 WHERE campaign_id = :id AND state = 'ACTIVE'
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        return consumed != null && consumed > 0 && (active == null || active == 0);
    }

    private boolean itemsTerminallyAccounted(UUID campaignId, String cohortType) {
        Integer remaining = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_execution_item i
                  JOIN campaign_cohort c ON c.cohort_id = i.cohort_id
                 WHERE i.campaign_id = :id
                   AND c.cohort_type = :type
                   AND i.state NOT IN ('COMPLETED', 'NOT_SENT', 'VENDOR_REJECTED')
                """,
                new MapSqlParameterSource().addValue("id", campaignId).addValue("type", cohortType),
                Integer.class
        );
        Integer total = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_execution_item i
                  JOIN campaign_cohort c ON c.cohort_id = i.cohort_id
                 WHERE i.campaign_id = :id AND c.cohort_type = :type
                """,
                new MapSqlParameterSource().addValue("id", campaignId).addValue("type", cohortType),
                Integer.class
        );
        return total != null && total > 0 && remaining != null && remaining == 0;
    }

    private static String destination(
            String preState,
            boolean unusedReleaseStale,
            boolean releaseConsumed,
            boolean canaryTerminallyAccounted,
            boolean cohortTerminallyAccounted
    ) {
        if (preState == null || "null".equals(preState)) {
            throw new CampaignException(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, "resumption pre-state required");
        }
        return switch (preState) {
            case "READY" -> "READY";
            case "CANARY_ACTIVE" -> {
                if (unusedReleaseStale) {
                    yield "PAUSED_FOR_RELEASE";
                }
                if (releaseConsumed && canaryTerminallyAccounted) {
                    yield "CANARY_VERIFIED";
                }
                yield "PAUSED_FOR_RELEASE";
            }
            case "CANARY_OBSERVING" -> "CANARY_OBSERVING";
            case "CANARY_VERIFIED" -> "CANARY_VERIFIED";
            case "PAUSED_FOR_RELEASE" -> "PAUSED_FOR_RELEASE";
            case "COHORT_ACTIVE" -> {
                if (unusedReleaseStale) {
                    yield "PAUSED_FOR_RELEASE";
                }
                if (releaseConsumed && cohortTerminallyAccounted) {
                    yield "COHORT_VERIFIED";
                }
                yield "PAUSED_FOR_RELEASE";
            }
            case "COHORT_OBSERVING" -> "COHORT_OBSERVING";
            case "COHORT_VERIFIED" -> "COHORT_VERIFIED";
            case "FINAL_OBSERVATION" -> "FINAL_OBSERVATION";
            default -> throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "no generic resume-previous-state transition"
            );
        };
    }

    private void lockCampaign(UUID campaignId) {
        jdbc.queryForObject(
                "SELECT campaign_id FROM production_change_campaign WHERE campaign_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", campaignId),
                UUID.class
        );
    }

    private void requireSuspended(UUID campaignId) {
        String state = jdbc.queryForObject(
                "SELECT state FROM production_change_campaign WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                String.class
        );
        if (!"SUSPENDED".equals(state)) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "resumption request requires active suspension"
            );
        }
    }

    private UUID currentRevisionId(UUID campaignId) {
        return jdbc.queryForObject(
                """
                SELECT r.revision_id FROM campaign_revision r
                  JOIN production_change_campaign c ON c.campaign_id = r.campaign_id
                   AND r.revision_number = c.current_revision_number
                 WHERE c.campaign_id = :id
                """,
                new MapSqlParameterSource("id", campaignId),
                UUID.class
        );
    }

    private static void requireHuman(AuthenticatedActor actor) {
        if (actor.actorType() != ActorType.HUMAN) {
            throw new CampaignException(
                    ProductionReasonCode.UNAUTHENTICATED_HUMAN_ACTOR,
                    "resumption governance is human-only until MAKE_EFFECTIVE"
            );
        }
    }

    private static void requirePermission(AuthenticatedActor actor, CampaignPermission permission) {
        if (actor == null || !actor.authenticated() || !actor.authorities().contains(permission)) {
            throw new CampaignException(
                    ProductionReasonCode.UNAUTHENTICATED_HUMAN_ACTOR,
                    "required resumption authority is absent"
            );
        }
        requireHuman(actor);
    }

    private static MapSqlParameterSource params(UUID campaignId, String actor) {
        return new MapSqlParameterSource()
                .addValue("id", campaignId)
                .addValue("actor", actor)
                .addValue("now", Timestamp.from(Instant.now()));
    }
}
