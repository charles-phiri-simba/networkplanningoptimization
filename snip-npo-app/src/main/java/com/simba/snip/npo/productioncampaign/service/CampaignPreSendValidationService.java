package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.domain.CampaignConstants;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class CampaignPreSendValidationService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CampaignGenerationService generationService;
    private final CampaignLeaseService leaseService;
    private final ForwardProgressionClosureService closureService;
    private final CampaignMutationSlotService slotService;
    private final CampaignKillHierarchyService killHierarchy;

    public CampaignPreSendValidationService(
            NamedParameterJdbcTemplate jdbc,
            CampaignGenerationService generationService,
            CampaignLeaseService leaseService,
            ForwardProgressionClosureService closureService,
            CampaignMutationSlotService slotService,
            CampaignKillHierarchyService killHierarchy
    ) {
        this.jdbc = jdbc;
        this.generationService = generationService;
        this.leaseService = leaseService;
        this.closureService = closureService;
        this.slotService = slotService;
        this.killHierarchy = killHierarchy;
    }

    public void validateCampaignOriginSend(String campaignHandoffId) {
        if (campaignHandoffId == null || campaignHandoffId.isBlank()) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH,
                    "campaign-originated send requires CampaignHandoffId"
            );
        }
        Map<String, Object> handoff = jdbc.queryForMap(
                """
                SELECT h.campaign_handoff_id, h.campaign_id, h.item_id, h.binding_digest, h.lineage_type,
                       b.control_generation, b.release_generation, b.campaign_fence, b.cell_id, b.parameter,
                       b.expected_value, b.desired_value, b.rollback_value, b.unit, b.campaign_fingerprint,
                       b.item_fingerprint, b.release_fingerprint
                  FROM campaign_execution_handoff h
                  JOIN campaign_execution_binding b ON b.binding_id = h.binding_id
                 WHERE h.campaign_handoff_id = :id
                """,
                new MapSqlParameterSource("id", campaignHandoffId)
        );
        UUID campaignId = (UUID) handoff.get("campaign_id");
        Map<String, Object> campaign = jdbc.queryForMap(
                """
                SELECT state, enabled, abort_state, fingerprint
                  FROM production_change_campaign
                 WHERE campaign_id = :id
                """,
                new MapSqlParameterSource("id", campaignId)
        );
        if (!Boolean.TRUE.equals(campaign.get("enabled"))) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "campaign is disabled");
        }
        String state = String.valueOf(campaign.get("state"));
        if ("ABORTED".equals(state) || "STALE".equals(state) || "EXPIRED".equals(state) || "SUSPENDED".equals(state)) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "campaign is not send-eligible: " + state);
        }
        if ("ABORTED".equals(String.valueOf(campaign.get("abort_state")))) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "aborted campaign cannot originate a new send");
        }
        if ("FORWARD".equals(String.valueOf(handoff.get("lineage_type")))) {
            closureService.assertOpen(campaignId);
        } else {
            closureService.assertClosed(campaignId);
        }
        long control = ((Number) handoff.get("control_generation")).longValue();
        long release = ((Number) handoff.get("release_generation")).longValue();
        long fence = ((Number) handoff.get("campaign_fence")).longValue();
        if (control != generationService.currentControlGeneration(campaignId)) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "campaign control generation is stale");
        }
        if (release != generationService.currentReleaseGeneration(campaignId)) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "campaign release generation is stale");
        }
        if (fence != leaseService.currentFence(campaignId)) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_LEASE_UNAVAILABLE, "campaign fencing token is stale");
        }
        slotService.assertCurrentFenceAndGeneration(campaignId, fence, control);
        CampaignMutationSlotService.SlotSnapshot snap = slotService.snapshot(campaignId);
        if (slotService.activeMutations(campaignId) != 1) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE,
                    "campaign mutation slot is not held for this send"
            );
        }
        UUID itemId = (UUID) handoff.get("item_id");
        if (snap.holderItemId() == null || !snap.holderItemId().equals(itemId)) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE,
                    "wrong mutation-slot holder"
            );
        }
        if (snap.reservationId() == null) {
            throw new CampaignException(ProductionReasonCode.SAFETY_RESERVATION_STALE, "safety reservation missing");
        }
        Integer reservationOk = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_safety_budget_reservation
                 WHERE reservation_id = :id AND state = 'RESERVED'
                """,
                new MapSqlParameterSource("id", snap.reservationId()),
                Integer.class
        );
        if (reservationOk == null || reservationOk != 1) {
            throw new CampaignException(ProductionReasonCode.SAFETY_RESERVATION_STALE, "safety reservation is not current");
        }
        if (!String.valueOf(campaign.get("fingerprint")).equals(String.valueOf(handoff.get("campaign_fingerprint")))) {
            throw new CampaignException(ProductionReasonCode.PRODUCTION_FINGERPRINT_MISMATCH, "campaign fingerprint mismatch");
        }
        Integer activeRelease = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_cohort_release
                 WHERE campaign_id = :id AND release_generation = :gen AND state = 'ACTIVE'
                """,
                new MapSqlParameterSource().addValue("id", campaignId).addValue("gen", release),
                Integer.class
        );
        if (activeRelease == null || activeRelease != 1) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "cohort release is inactive or stale");
        }
        String itemState = jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = :id",
                new MapSqlParameterSource("id", itemId),
                String.class
        );
        if (!java.util.Set.of("HANDED_OFF", "PRE_SEND").contains(itemState)) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "item is not send-eligible: " + itemState);
        }
        killHierarchy.assertSendAllowed(
                campaignId,
                null,
                itemId,
                String.valueOf(jdbc.queryForObject(
                        "SELECT production_target_id FROM production_change_campaign WHERE campaign_id = :id",
                        new MapSqlParameterSource("id", campaignId),
                        String.class
                ))
        );
        if (!CampaignConstants.PARAMETER_TX_POWER.equals(String.valueOf(handoff.get("parameter")))
                || !CampaignConstants.UNIT_DBM.equals(String.valueOf(handoff.get("unit")))) {
            throw new CampaignException(
                    ProductionReasonCode.UNSUPPORTED_OBJECT_OR_PARAMETER,
                    "campaign send is CELL/txPower/dBm only"
            );
        }
        assertP17Current(String.valueOf(jdbc.queryForObject(
                "SELECT production_target_id FROM production_change_campaign WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                String.class
        )));
    }

    public void assertBindingEqualsChange(
            String campaignHandoffId,
            String cellId,
            String parameter,
            BigDecimal expected,
            BigDecimal desired,
            BigDecimal rollback
    ) {
        Map<String, Object> binding = jdbc.queryForMap(
                """
                SELECT b.cell_id, b.parameter, b.expected_value, b.desired_value, b.rollback_value, b.unit
                  FROM campaign_execution_handoff h
                  JOIN campaign_execution_binding b ON b.binding_id = h.binding_id
                 WHERE h.campaign_handoff_id = :id
                """,
                new MapSqlParameterSource("id", campaignHandoffId)
        );
        if (!cellId.equals(String.valueOf(binding.get("cell_id")))
                || !parameter.equals(String.valueOf(binding.get("parameter")))
                || ((BigDecimal) binding.get("expected_value")).compareTo(expected) != 0
                || ((BigDecimal) binding.get("desired_value")).compareTo(desired) != 0
                || ((BigDecimal) binding.get("rollback_value")).compareTo(rollback) != 0
                || !CampaignConstants.UNIT_DBM.equals(String.valueOf(binding.get("unit")))) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH,
                    "campaign binding mutation does not equal Phase 16 mutation"
            );
        }
    }

    private void assertP17Current(String targetId) {
        try {
            Integer revoked = jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM production_target_onboarding
                     WHERE production_target_id = :target
                       AND status IN ('REVOKED', 'SUSPENDED', 'INVALID')
                    """,
                    new MapSqlParameterSource("target", targetId),
                    Integer.class
            );
            if (revoked != null && revoked > 0) {
                throw new CampaignException(
                        ProductionReasonCode.CAMPAIGN_STALE,
                        "Phase 17 onboarding/certification currentness is revoked or invalid"
                );
            }
        } catch (CampaignException ex) {
            throw ex;
        } catch (org.springframework.dao.DataAccessException ex) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_STALE,
                    "Phase 17 onboarding currentness cannot be resolved",
                    ex
            );
        }
    }
}
