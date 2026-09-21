package com.simba.snip.npo.productioncampaign.service;

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
import java.util.UUID;

@Service
public class CampaignRecoveryService {

    private final NamedParameterJdbcTemplate jdbc;
    private final ForwardProgressionClosureService closureService;
    private final CampaignAuditService auditService;
    private final CampaignSafetyService safetyService;
    private final CampaignMutationSlotService slotService;
    private final CampaignHandoffService handoffService;

    public CampaignRecoveryService(
            NamedParameterJdbcTemplate jdbc,
            ForwardProgressionClosureService closureService,
            CampaignAuditService auditService,
            CampaignSafetyService safetyService,
            CampaignMutationSlotService slotService,
            CampaignHandoffService handoffService
    ) {
        this.jdbc = jdbc;
        this.closureService = closureService;
        this.auditService = auditService;
        this.safetyService = safetyService;
        this.slotService = slotService;
        this.handoffService = handoffService;
    }

    @Transactional
    public void request(UUID campaignId, AuthenticatedActor actor) {
        require(actor, CampaignPermission.CAMPAIGN_RECOVERY_REQUEST);
        closureService.assertClosed(campaignId);
        assertNotCampaignAuthorizer(campaignId, actor.actorId());
        int updated = jdbc.update(
                """
                UPDATE campaign_recovery
                   SET state = 'REQUESTED', requester_principal_id = :actor, requested_at = :now, version = version + 1
                 WHERE campaign_id = :id AND state = 'REQUIRED'
                """,
                params(campaignId, actor.actorId())
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_RECOVERY_TRANSITION,
                    "recovery request expected REQUIRED and affected zero rows"
            );
        }
        auditService.append(campaignId, "RECOVERY_REQUESTED", actor.actorId(), Map.of("state", "REQUESTED"));
    }

    @Transactional
    public void startReview(UUID campaignId, AuthenticatedActor actor) {
        require(actor, CampaignPermission.CAMPAIGN_RECOVERY_REVIEW);
        closureService.assertClosed(campaignId);
        assertNotCampaignAuthorizer(campaignId, actor.actorId());
        Integer ok = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_recovery
                 WHERE campaign_id = :id AND state = 'REQUESTED'
                   AND requester_principal_id IS DISTINCT FROM :actor
                """,
                params(campaignId, actor.actorId()),
                Integer.class
        );
        if (ok == null || ok != 1) {
            throw new CampaignException(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, "recovery reviewer SoD failed");
        }
        int updated = jdbc.update(
                """
                UPDATE campaign_recovery
                   SET state = 'UNDER_REVIEW', reviewer_principal_id = :actor, version = version + 1
                 WHERE campaign_id = :id AND state = 'REQUESTED'
                """,
                params(campaignId, actor.actorId())
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_RECOVERY_TRANSITION,
                    "recovery review expected REQUESTED and affected zero rows"
            );
        }
        auditService.append(campaignId, "RECOVERY_UNDER_REVIEW", actor.actorId(), Map.of());
    }

    @Transactional
    public void authorize(UUID campaignId, AuthenticatedActor actor) {
        require(actor, CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE);
        closureService.assertClosed(campaignId);
        assertNotCampaignAuthorizer(campaignId, actor.actorId());
        assertCrossLayerSod(campaignId, actor.actorId());
        Integer ok = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_recovery
                 WHERE campaign_id = :id AND state IN ('UNDER_REVIEW', 'REVIEWED')
                   AND requester_principal_id IS DISTINCT FROM :actor
                   AND reviewer_principal_id IS DISTINCT FROM :actor
                """,
                params(campaignId, actor.actorId()),
                Integer.class
        );
        if (ok == null || ok != 1) {
            throw new CampaignException(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, "recovery authorizer SoD failed");
        }
        int updated = jdbc.update(
                """
                UPDATE campaign_recovery
                   SET state = 'AUTHORIZED', authorizer_principal_id = :actor, authorized_at = :now, version = version + 1
                 WHERE campaign_id = :id AND state IN ('UNDER_REVIEW', 'REVIEWED')
                """,
                params(campaignId, actor.actorId())
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_RECOVERY_TRANSITION,
                    "recovery authorize expected UNDER_REVIEW/REVIEWED and affected zero rows"
            );
        }
        auditService.append(campaignId, "RECOVERY_AUTHORIZED", actor.actorId(), Map.of());
    }

    /**
     * After campaign recovery AUTHORIZE and inherited P16 rollback authorization:
     * reserve RecoverySafetyBudget and acquire/rebind the combined slot.
     * Does not mint a P16 grant.
     */
    @Transactional
    public UUID beginSerializedRecovery(UUID campaignId, UUID revisionId, UUID itemId, AuthenticatedActor actor, long fence, long controlGeneration) {
        assertRecoveryHandoffPermitted(campaignId, actor);
        Integer p16Authorized = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_execution_rollback r
                  JOIN production_network_change n ON n.production_change_id = r.production_change_id
                 WHERE r.status IN ('AUTHORIZED')
                   AND (
                        n.campaign_handoff_id IN (
                            SELECT campaign_handoff_id FROM campaign_execution_handoff WHERE campaign_id = :id
                        )
                     OR n.production_change_id IN (
                            SELECT production_change_id FROM campaign_execution_item
                             WHERE campaign_id = :id AND production_change_id IS NOT NULL
                        )
                   )
                """,
                params(campaignId, actor.actorId()),
                Integer.class
        );
        if (p16Authorized == null || p16Authorized < 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_RECOVERY_TRANSITION,
                    "recovery mutation requires separately governed P16 rollback AUTHORIZED"
            );
        }
        var snap = slotService.snapshot(campaignId);
        UUID reservationId;
        if ("FORWARD".equals(snap.holderType()) && ("ACQUIRED".equals(snap.state()) || "HELD_MAY_HAVE_SENT".equals(snap.state()))) {
            rebindHolderToRecovery(campaignId, revisionId, actor, fence, controlGeneration);
            reservationId = slotService.snapshot(campaignId).reservationId();
        } else {
            reservationId = slotService.acquireRecovery(campaignId, revisionId, itemId, fence, controlGeneration);
        }
        if (itemId != null) {
            handoffService.createOrReturnRecoveryHandoff(campaignId, itemId);
        }
        return reservationId;
    }

    @Transactional
    public void rebindHolderToRecovery(UUID campaignId, UUID revisionId, AuthenticatedActor actor, long fence, long controlGeneration) {
        assertRecoveryHandoffPermitted(campaignId, actor);
        Integer p16Authorized = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_execution_rollback r
                  JOIN production_network_change n ON n.production_change_id = r.production_change_id
                 WHERE r.status = 'AUTHORIZED'
                   AND (
                        n.campaign_handoff_id IN (
                            SELECT campaign_handoff_id FROM campaign_execution_handoff WHERE campaign_id = :id
                        )
                     OR n.production_change_id IN (
                            SELECT production_change_id FROM campaign_execution_item
                             WHERE campaign_id = :id AND production_change_id IS NOT NULL
                        )
                   )
                """,
                params(campaignId, actor.actorId()),
                Integer.class
        );
        if (p16Authorized == null || p16Authorized < 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_RECOVERY_TRANSITION,
                    "recovery rebind requires separately governed P16 rollback authorization"
            );
        }
        UUID reservationId = safetyService.reserve(campaignId, revisionId, null, "RECOVERY");
        slotService.rebindHolderToRecovery(campaignId, reservationId, fence, controlGeneration);
        auditService.append(campaignId, "SLOT_REBOUND_RECOVERY", actor.actorId(), Map.of());
    }

    public boolean isAuthorized(UUID campaignId) {
        String state = jdbc.queryForObject(
                "SELECT state FROM campaign_recovery WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                String.class
        );
        return "AUTHORIZED".equals(state);
    }

    public void assertRecoveryHandoffPermitted(UUID campaignId, AuthenticatedActor actor) {
        closureService.assertClosed(campaignId);
        if (!isAuthorized(campaignId)) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_RECOVERY_TRANSITION,
                    "recovery handoff requires campaign recovery AUTHORIZED"
            );
        }
        assertCrossLayerSod(campaignId, actor == null ? null : actor.actorId());
    }

    public void assertCrossLayerSod(UUID campaignId, String actorId) {
        if (actorId == null) {
            return;
        }
        Integer p16 = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_execution_rollback r
                  JOIN production_network_change n ON n.production_change_id = r.production_change_id
                 WHERE (r.authorizer_principal_id = :actor OR n.executor_principal_id = :actor
                        OR n.authorizer_principal_id = :actor)
                   AND (
                        n.campaign_handoff_id IN (
                            SELECT campaign_handoff_id FROM campaign_execution_handoff WHERE campaign_id = :id
                        )
                     OR n.production_change_id IN (
                            SELECT production_change_id FROM campaign_execution_item
                             WHERE campaign_id = :id AND production_change_id IS NOT NULL
                        )
                   )
                """,
                params(campaignId, actorId),
                Integer.class
        );
        if (p16 != null && p16 > 0) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                    "campaign recovery authorizer cannot be P16 rollback authorizer or executor"
            );
        }
        Integer releaser = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_cohort_release
                 WHERE campaign_id = :id AND releaser_principal_id = :actor
                """,
                params(campaignId, actorId),
                Integer.class
        );
        Integer p16Authorizer = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_execution_rollback r
                  JOIN production_network_change n ON n.production_change_id = r.production_change_id
                 WHERE r.authorizer_principal_id = :actor
                   AND n.campaign_handoff_id IN (
                        SELECT campaign_handoff_id FROM campaign_execution_handoff WHERE campaign_id = :id
                   )
                """,
                params(campaignId, actorId),
                Integer.class
        );
        if (releaser != null && releaser > 0 && p16Authorizer != null && p16Authorizer > 0) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                    "cohort releaser cannot be P16 rollback authorizer for the same recovery lineage"
            );
        }
    }

    private void assertNotCampaignAuthorizer(UUID campaignId, String actorId) {
        Integer same = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_change_campaign
                 WHERE campaign_id = :id AND authorizer_principal_id = :actor
                """,
                params(campaignId, actorId),
                Integer.class
        );
        if (same != null && same > 0) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                    "campaign authorizer cannot participate in recovery request/review/authorize"
            );
        }
    }

    private static void require(AuthenticatedActor actor, CampaignPermission permission) {
        if (actor == null || !actor.authenticated() || !actor.authorities().contains(permission)) {
            throw new CampaignException(
                    ProductionReasonCode.UNAUTHENTICATED_HUMAN_ACTOR,
                    "campaign recovery authority is absent"
            );
        }
    }

    private static MapSqlParameterSource params(UUID campaignId, String actor) {
        return new MapSqlParameterSource()
                .addValue("id", campaignId)
                .addValue("actor", actor)
                .addValue("now", Timestamp.from(Instant.now()));
    }
}
