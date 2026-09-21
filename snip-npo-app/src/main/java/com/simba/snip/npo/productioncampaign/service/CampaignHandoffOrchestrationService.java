package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.domain.AuthenticatedActor;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.service.ProductionChangeControlService;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Frozen §9.3 executable handoff: local HANDOFF_PENDING commit, then Phase 16
 * create-or-return. Campaign code does not mint a grant.
 */
@Service
public class CampaignHandoffOrchestrationService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CampaignHandoffService handoffService;
    private final CampaignOriginAdmissionService originAdmission;
    private final CampaignItemLifecycleService itemLifecycle;
    private final CampaignAuditService auditService;
    private final CampaignPreSendValidationService preSendValidation;
    private final CampaignHandoffOrchestrationService self;

    public CampaignHandoffOrchestrationService(
            NamedParameterJdbcTemplate jdbc,
            CampaignHandoffService handoffService,
            CampaignOriginAdmissionService originAdmission,
            CampaignItemLifecycleService itemLifecycle,
            CampaignAuditService auditService,
            CampaignPreSendValidationService preSendValidation,
            @Lazy CampaignHandoffOrchestrationService self
    ) {
        this.jdbc = jdbc;
        this.handoffService = handoffService;
        this.originAdmission = originAdmission;
        this.itemLifecycle = itemLifecycle;
        this.auditService = auditService;
        this.preSendValidation = preSendValidation;
        this.self = self;
    }

    @Transactional
    public String commitForwardHandoff(UUID campaignId, UUID itemId) {
        String state = itemLifecycle.currentState(itemId);
        if ("RELEASED".equals(state)) {
            itemLifecycle.transition(itemId, "RELEASED", "INDIVIDUAL_ELIGIBILITY");
            state = "ELIGIBLE";
        }
        if (!"ELIGIBLE".equals(state) && !"HANDOFF_PENDING".equals(state)) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "forward handoff requires ELIGIBLE item, was " + state
            );
        }
        Map<String, Object> item = jdbc.queryForMap(
                """
                SELECT i.revision_id, i.cohort_id, i.item_fingerprint, i.production_target_id,
                       i.phase14_plan_id, i.phase14_plan_fingerprint, i.phase15_execution_id,
                       i.phase15_execution_fingerprint, i.cell_id, i.expected_value, i.desired_value,
                       i.rollback_value, c.fingerprint, c.current_revision_number,
                       cg.current_generation AS control_generation,
                       rg.current_generation AS release_generation,
                       l.fencing_token,
                       rel.release_fingerprint
                  FROM campaign_execution_item i
                  JOIN production_change_campaign c ON c.campaign_id = i.campaign_id
                  JOIN campaign_control_generation cg ON cg.campaign_id = c.campaign_id
                  JOIN campaign_release_generation rg ON rg.campaign_id = c.campaign_id
                  JOIN campaign_lease l ON l.campaign_id = c.campaign_id
                  LEFT JOIN campaign_cohort_release rel
                    ON rel.cohort_id = i.cohort_id AND rel.state = 'ACTIVE'
                 WHERE i.item_id = :itemId AND i.campaign_id = :campaignId
                """,
                new MapSqlParameterSource().addValue("itemId", itemId).addValue("campaignId", campaignId)
        );
        String releaseFp = item.get("release_fingerprint") == null ? "none" : String.valueOf(item.get("release_fingerprint"));
        String handoffId = handoffService.createOrReturnForwardHandoff(
                campaignId,
                (UUID) item.get("revision_id"),
                ((Number) item.get("current_revision_number")).intValue(),
                itemId,
                (UUID) item.get("cohort_id"),
                ((Number) item.get("control_generation")).longValue(),
                ((Number) item.get("release_generation")).longValue(),
                item.get("fencing_token") == null ? 0L : ((Number) item.get("fencing_token")).longValue(),
                String.valueOf(item.get("fingerprint")),
                releaseFp,
                String.valueOf(item.get("item_fingerprint")),
                String.valueOf(item.get("production_target_id")),
                (UUID) item.get("phase14_plan_id"),
                item.get("phase14_plan_fingerprint") == null ? null : String.valueOf(item.get("phase14_plan_fingerprint")),
                (UUID) item.get("phase15_execution_id"),
                item.get("phase15_execution_fingerprint") == null ? null : String.valueOf(item.get("phase15_execution_fingerprint")),
                String.valueOf(item.get("cell_id")),
                (BigDecimal) item.get("expected_value"),
                (BigDecimal) item.get("desired_value"),
                (BigDecimal) item.get("rollback_value")
        );
        if ("ELIGIBLE".equals(itemLifecycle.currentState(itemId))) {
            itemLifecycle.transition(itemId, "ELIGIBLE", "START_HANDOFF");
        }
        return handoffId;
    }

    /**
     * After local commit: Phase 16 create-or-return. Must not hold Phase 18 locks.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProductionNetworkChangeEntity correlatePhase16(
            String campaignHandoffId,
            UUID phase15ExecutionId,
            String productionTargetId,
            ProductionChangeControlService.ChangeControlReference changeControl,
            ActorPrincipal requester
    ) {
        ProductionNetworkChangeEntity change = originAdmission.createOrReturn(
                campaignHandoffId, phase15ExecutionId, productionTargetId, changeControl, requester
        );
        jdbc.update(
                """
                UPDATE campaign_execution_handoff
                   SET production_change_id = :changeId
                 WHERE campaign_handoff_id = :handoff
                   AND production_change_id IS NULL
                """,
                new MapSqlParameterSource()
                        .addValue("changeId", change.getProductionChangeId())
                        .addValue("handoff", campaignHandoffId)
        );
        UUID itemId = jdbc.queryForObject(
                "SELECT item_id FROM campaign_execution_handoff WHERE campaign_handoff_id = :id",
                new MapSqlParameterSource("id", campaignHandoffId),
                UUID.class
        );
        if ("HANDOFF_PENDING".equals(itemLifecycle.currentState(itemId))) {
            itemLifecycle.transition(itemId, "HANDOFF_PENDING", "P16_LINEAGE_CORRELATED");
        }
        UUID campaignId = jdbc.queryForObject(
                "SELECT campaign_id FROM campaign_execution_handoff WHERE campaign_handoff_id = :id",
                new MapSqlParameterSource("id", campaignHandoffId),
                UUID.class
        );
        auditService.append(campaignId, "ITEM_HANDED_OFF", "SYSTEM", Map.of(
                "campaignHandoffId", campaignHandoffId,
                "productionChangeId", change.getProductionChangeId().toString()
        ));
        return change;
    }

    public String executeForwardHandoff(UUID campaignId, UUID itemId) {
        return executeForwardHandoff(campaignId, itemId, null);
    }

    /**
     * Local HANDOFF_PENDING commit, then P16 create-or-return / bind existing
     * production change. Campaign code does not mint a grant.
     */
    public String executeForwardHandoff(UUID campaignId, UUID itemId, ActorPrincipal requester) {
        String existingHandoffId = existingForwardHandoffId(itemId);
        String itemState = itemLifecycle.currentState(itemId);
        if (existingHandoffId != null && terminalOrBoundHandoffState(itemState)) {
            self.bindPhase16IfPresent(existingHandoffId);
            return existingHandoffId;
        }
        String handoffId = self.commitForwardHandoff(campaignId, itemId);
        if (self.bindPhase16IfPresent(handoffId)) {
            return handoffId;
        }
        Map<String, Object> item = jdbc.queryForMap(
                """
                SELECT i.phase15_execution_id, i.production_target_id, c.change_control_reference,
                       c.creator_principal_id
                  FROM campaign_execution_item i
                  JOIN production_change_campaign c ON c.campaign_id = i.campaign_id
                 WHERE i.item_id = :itemId
                """,
                new MapSqlParameterSource("itemId", itemId)
        );
        ActorPrincipal admissionActor = requester != null
                ? requester
                : ActorPrincipal.of("SYSTEM:CAMPAIGN_HANDOFF");
        String validator = requester != null
                ? String.valueOf(item.get("creator_principal_id"))
                : "SYSTEM:CHANGE_CONTROL";
        if (validator.equals(admissionActor.actorPrincipalId())) {
            validator = "SYSTEM:CHANGE_CONTROL";
        }
        ProductionChangeControlService.ChangeControlReference changeControl =
                new ProductionChangeControlService.ChangeControlReference(
                        "MANUAL",
                        String.valueOf(item.get("change_control_reference")),
                        "VALIDATED",
                        validator,
                        java.time.Instant.now(),
                        java.time.Instant.now().plusSeconds(3600)
                );
        originAdmission.createOrReturn(
                handoffId,
                (UUID) item.get("phase15_execution_id"),
                String.valueOf(item.get("production_target_id")),
                changeControl,
                admissionActor
        );
        if (!self.bindPhase16IfPresent(handoffId)) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH,
                    "P16 create-or-return did not yield a durable campaign-originated production change"
            );
        }
        return handoffId;
    }

    public boolean bindPhase16IfPresent(String campaignHandoffId) {
        UUID changeId = jdbc.query(
                """
                SELECT production_change_id FROM production_network_change
                 WHERE campaign_handoff_id = :id
                """,
                new MapSqlParameterSource("id", campaignHandoffId),
                rs -> rs.next() ? (UUID) rs.getObject(1) : null
        );
        if (changeId == null) {
            return false;
        }
        String origin = jdbc.queryForObject(
                "SELECT execution_origin FROM production_network_change WHERE production_change_id = :id",
                new MapSqlParameterSource("id", changeId),
                String.class
        );
        if (!"PRODUCTION_CAMPAIGN".equals(origin)) {
            throw new CampaignException(
                    ProductionReasonCode.STANDALONE_DOWNGRADE_DENIED,
                    "campaign-bound production change cannot be treated as standalone"
            );
        }
        String fingerprint = jdbc.queryForObject(
                "SELECT production_fingerprint FROM production_network_change WHERE production_change_id = :id",
                new MapSqlParameterSource("id", changeId),
                String.class
        );
        jdbc.update(
                """
                UPDATE campaign_execution_handoff
                   SET production_change_id = :changeId
                 WHERE campaign_handoff_id = :handoff
                """,
                new MapSqlParameterSource()
                        .addValue("changeId", changeId)
                        .addValue("handoff", campaignHandoffId)
        );
        jdbc.update(
                """
                UPDATE campaign_execution_binding
                   SET production_change_id = :changeId,
                       production_fingerprint = :fp
                 WHERE item_id = (
                    SELECT item_id FROM campaign_execution_handoff WHERE campaign_handoff_id = :handoff
                 )
                """,
                new MapSqlParameterSource()
                        .addValue("changeId", changeId)
                        .addValue("fp", fingerprint)
                        .addValue("handoff", campaignHandoffId)
        );
        jdbc.update(
                """
                UPDATE campaign_execution_item
                   SET production_change_id = :changeId,
                       production_fingerprint = :fp
                 WHERE item_id = (
                    SELECT item_id FROM campaign_execution_handoff WHERE campaign_handoff_id = :handoff
                 )
                """,
                new MapSqlParameterSource()
                        .addValue("changeId", changeId)
                        .addValue("fp", fingerprint)
                        .addValue("handoff", campaignHandoffId)
        );
        UUID itemId = jdbc.queryForObject(
                "SELECT item_id FROM campaign_execution_handoff WHERE campaign_handoff_id = :id",
                new MapSqlParameterSource("id", campaignHandoffId),
                UUID.class
        );
        if ("HANDOFF_PENDING".equals(itemLifecycle.currentState(itemId))) {
            itemLifecycle.transition(itemId, "HANDOFF_PENDING", "P16_LINEAGE_CORRELATED");
        }
        UUID campaignId = jdbc.queryForObject(
                "SELECT campaign_id FROM campaign_execution_handoff WHERE campaign_handoff_id = :id",
                new MapSqlParameterSource("id", campaignHandoffId),
                UUID.class
        );
        auditService.append(campaignId, "ITEM_HANDED_OFF", "SYSTEM", Map.of(
                "campaignHandoffId", campaignHandoffId,
                "productionChangeId", changeId.toString()
        ));
        return true;
    }

    public void assertRecoveryHandoffClosed(UUID campaignId, AuthenticatedActor actor, CampaignRecoveryService recovery) {
        recovery.assertRecoveryHandoffPermitted(campaignId, actor);
    }

    private String existingForwardHandoffId(UUID itemId) {
        return jdbc.query(
                """
                SELECT campaign_handoff_id FROM campaign_execution_handoff
                 WHERE item_id = :itemId AND lineage_type = 'FORWARD'
                 ORDER BY created_at DESC
                 LIMIT 1
                """,
                new MapSqlParameterSource("itemId", itemId),
                rs -> rs.next() ? rs.getString(1) : null
        );
    }

    private static boolean terminalOrBoundHandoffState(String state) {
        return "HANDED_OFF".equals(state)
                || "PRE_SEND".equals(state)
                || "MAY_HAVE_SENT".equals(state)
                || "OUTCOME_UNRESOLVED".equals(state)
                || "VENDOR_REJECTED".equals(state)
                || "COMPLETED".equals(state)
                || "RECONCILIATION_PENDING".equals(state)
                || "CANONICAL_RECONCILED".equals(state)
                || "FAILED_CLOSED".equals(state);
    }
}
