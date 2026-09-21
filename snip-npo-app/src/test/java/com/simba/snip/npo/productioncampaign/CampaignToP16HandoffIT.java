package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.domain.ActorType;
import com.simba.snip.npo.productioncampaign.domain.AuthenticatedActor;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.service.CampaignHandoffOrchestrationService;
import com.simba.snip.npo.productioncampaign.service.CampaignItemLifecycleService;
import com.simba.snip.npo.productioncampaign.service.CampaignOriginAdmissionService;
import com.simba.snip.npo.productioncampaign.service.ProductionChangeCampaignService;
import com.simba.snip.npo.productionchange.ProductionChangeITSupport;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.vendorcertification.Phase17GraphCleanup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignToP16HandoffIT extends ProductionChangeITSupport {

    private static final String CAMPAIGN_SOURCE = "TEST_INGRESS";

    @DynamicPropertySource
    static void campaignProps(DynamicPropertyRegistry registry) {
        registry.add("snip.production-campaign.trusted-ingress-guarantee", () -> "true");
        registry.add("snip.production-campaign.trusted-authentication-sources[0]", () -> CAMPAIGN_SOURCE);
        registry.add("snip.production-campaign.configured-policy-maximum", () -> "25");
    }

    @Autowired ProductionChangeCampaignService campaignService;
    @Autowired CampaignHandoffOrchestrationService orchestration;
    @Autowired CampaignOriginAdmissionService originAdmission;
    @Autowired CampaignItemLifecycleService itemLifecycle;
    @Autowired com.simba.snip.npo.productioncampaign.service.CampaignHandoffService handoffService;

    @AfterEach
    void cleanupCampaignRowsBeforeTargetDelete() {
        jdbc.update("DELETE FROM campaign_external_interference");
        jdbc.update("DELETE FROM campaign_audit_event");
        jdbc.update("DELETE FROM campaign_governance_idempotency");
        jdbc.update("DELETE FROM campaign_resumption");
        jdbc.update("DELETE FROM campaign_suspension");
        jdbc.update("DELETE FROM campaign_recovery");
        jdbc.update("DELETE FROM campaign_observation_boundary");
        jdbc.update("DELETE FROM campaign_execution_handoff");
        jdbc.update("DELETE FROM campaign_execution_binding");
        jdbc.update("DELETE FROM campaign_exposed_cell");
        jdbc.update("DELETE FROM campaign_safety_budget_reservation");
        jdbc.update("DELETE FROM campaign_safety_exposure");
        jdbc.update("DELETE FROM campaign_mutation_slot");
        jdbc.update("DELETE FROM campaign_forward_progression_closure");
        jdbc.update("DELETE FROM campaign_cohort_release");
        jdbc.update("UPDATE campaign_cohort SET state = 'PLANNED'");
        jdbc.update("DELETE FROM campaign_release_generation");
        jdbc.update("DELETE FROM campaign_control_generation");
        jdbc.update("DELETE FROM campaign_lease");
        jdbc.update("DELETE FROM campaign_execution_item");
        jdbc.update("DELETE FROM campaign_cohort");
        jdbc.update("DELETE FROM campaign_revision");
        jdbc.update("DELETE FROM production_change_campaign");
        Phase17GraphCleanup.deleteAll(jdbc);
    }

    @Test
    void executeForwardHandoffCreatesCampaignOriginP16AndIsReplaySafe() {
        UUID phase15 = verifiedPhase15ExecutionId();
        Map<String, Object> execution = jdbc.queryForMap(
                "SELECT plan_id, plan_fingerprint, execution_fingerprint, cell_id FROM network_change_execution WHERE id = ?",
                phase15
        );
        Map<String, Object> operation = jdbc.queryForMap(
                "SELECT expected_current_value, desired_value FROM network_change_execution_operation WHERE execution_id = ?",
                phase15
        );
        String cellId = String.valueOf(execution.get("cell_id"));
        BigDecimal expected = new BigDecimal(String.valueOf(operation.get("expected_current_value")));
        BigDecimal desired = new BigDecimal(String.valueOf(operation.get("desired_value")));
        AuthenticatedActor creator = actor("creator-1", CampaignPermission.CAMPAIGN_CREATE);
        var created = campaignService.create(
                TARGET_ID,
                "campaign p16 handoff",
                "CC-P18-HO-" + UUID.randomUUID(),
                List.of(new ProductionChangeCampaignService.PlannedItem(
                        (UUID) execution.get("plan_id"),
                        String.valueOf(execution.get("plan_fingerprint")).toLowerCase(),
                        phase15,
                        String.valueOf(execution.get("execution_fingerprint")).toLowerCase(),
                        cellId,
                        expected,
                        desired,
                        expected,
                        "dBm",
                        "CELL",
                        "txPower"
                )),
                creator,
                "idem-p16-ho-" + UUID.randomUUID()
        );
        UUID campaignId = created.getCampaignId();
        campaignService.transition(campaignId, "SUBMIT_FOR_REVIEW", creator, "idem-p16-ho-s");
        campaignService.transition(campaignId, "REVIEW_APPROVE", actor("reviewer-1", CampaignPermission.CAMPAIGN_REVIEW), "idem-p16-ho-r");
        campaignService.transition(campaignId, "AUTHORIZE_CAMPAIGN", actor("authorizer-1", CampaignPermission.CAMPAIGN_AUTHORIZE), "idem-p16-ho-a");
        campaignService.evaluateReady(campaignId);
        campaignService.transition(campaignId, "RELEASE_CANARY", actor("releaser-1", CampaignPermission.RELEASE_COHORT), "idem-p16-ho-rel");
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);

        String handoffId = orchestration.executeForwardHandoff(campaignId, itemId);
        assertEquals(64, handoffId.length());
        UUID p16 = jdbc.queryForObject(
                "SELECT production_change_id FROM production_network_change WHERE campaign_handoff_id = ?",
                UUID.class,
                handoffId
        );
        assertEquals("PRODUCTION_CAMPAIGN", jdbc.queryForObject(
                "SELECT execution_origin FROM production_network_change WHERE production_change_id = ?",
                String.class,
                p16
        ));
        assertEquals(handoffId, jdbc.queryForObject(
                "SELECT campaign_handoff_id FROM production_network_change WHERE production_change_id = ?",
                String.class,
                p16
        ));
        assertEquals(p16, jdbc.queryForObject(
                "SELECT production_change_id FROM campaign_execution_handoff WHERE campaign_handoff_id = ?",
                UUID.class,
                handoffId
        ));
        assertEquals(p16, jdbc.queryForObject(
                "SELECT production_change_id FROM campaign_execution_binding WHERE item_id = ?",
                UUID.class,
                itemId
        ));
        assertEquals("HANDED_OFF", itemLifecycle.currentState(itemId));

        String replay = orchestration.executeForwardHandoff(campaignId, itemId);
        assertEquals(handoffId, replay);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_network_change WHERE campaign_handoff_id = ?",
                Integer.class,
                handoffId
        ));
        assertEquals(p16, jdbc.queryForObject(
                "SELECT production_change_id FROM production_network_change WHERE campaign_handoff_id = ?",
                UUID.class,
                handoffId
        ));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ?",
                Integer.class,
                p16
        ));
    }

    @Test
    void crashAfterP16CreateBeforeBindRecoversSameRow() {
        UUID phase15 = verifiedPhase15ExecutionId();
        Map<String, Object> execution = jdbc.queryForMap(
                "SELECT plan_id, plan_fingerprint, execution_fingerprint, cell_id FROM network_change_execution WHERE id = ?",
                phase15
        );
        Map<String, Object> operation = jdbc.queryForMap(
                "SELECT expected_current_value, desired_value FROM network_change_execution_operation WHERE execution_id = ?",
                phase15
        );
        AuthenticatedActor creator = actor("creator-1", CampaignPermission.CAMPAIGN_CREATE);
        var created = campaignService.create(
                TARGET_ID,
                "campaign p16 crash",
                "CC-P18-CR-" + UUID.randomUUID(),
                List.of(new ProductionChangeCampaignService.PlannedItem(
                        (UUID) execution.get("plan_id"),
                        String.valueOf(execution.get("plan_fingerprint")).toLowerCase(),
                        phase15,
                        String.valueOf(execution.get("execution_fingerprint")).toLowerCase(),
                        String.valueOf(execution.get("cell_id")),
                        new BigDecimal(String.valueOf(operation.get("expected_current_value"))),
                        new BigDecimal(String.valueOf(operation.get("desired_value"))),
                        new BigDecimal(String.valueOf(operation.get("expected_current_value"))),
                        "dBm",
                        "CELL",
                        "txPower"
                )),
                creator,
                "idem-p16-cr-" + UUID.randomUUID()
        );
        UUID campaignId = created.getCampaignId();
        campaignService.transition(campaignId, "SUBMIT_FOR_REVIEW", creator, "idem-p16-cr-s");
        campaignService.transition(campaignId, "REVIEW_APPROVE", actor("reviewer-1", CampaignPermission.CAMPAIGN_REVIEW), "idem-p16-cr-r");
        campaignService.transition(campaignId, "AUTHORIZE_CAMPAIGN", actor("authorizer-1", CampaignPermission.CAMPAIGN_AUTHORIZE), "idem-p16-cr-a");
        campaignService.evaluateReady(campaignId);
        campaignService.transition(campaignId, "RELEASE_CANARY", actor("releaser-1", CampaignPermission.RELEASE_COHORT), "idem-p16-cr-rel");
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        String handoffId = orchestration.commitForwardHandoff(campaignId, itemId);
        originAdmission.createOrReturn(
                handoffId,
                phase15,
                TARGET_ID,
                new com.simba.snip.npo.productionchange.service.ProductionChangeControlService.ChangeControlReference(
                        "MANUAL",
                        "CC-P18-CR",
                        "VALIDATED",
                        "SYSTEM:CHANGE_CONTROL",
                        java.time.Instant.now(),
                        java.time.Instant.now().plusSeconds(3600)
                ),
                com.simba.snip.npo.productionchange.domain.ActorPrincipal.of("SYSTEM:CAMPAIGN_HANDOFF")
        );
        jdbc.update("UPDATE campaign_execution_handoff SET production_change_id = NULL WHERE campaign_handoff_id = ?", handoffId);
        jdbc.update("UPDATE campaign_execution_item SET state = 'HANDOFF_PENDING', production_change_id = NULL WHERE item_id = ?", itemId);
        UUID existing = jdbc.queryForObject(
                "SELECT production_change_id FROM production_network_change WHERE campaign_handoff_id = ?",
                UUID.class,
                handoffId
        );
        String recovered = orchestration.executeForwardHandoff(campaignId, itemId);
        assertEquals(handoffId, recovered);
        assertEquals(existing, jdbc.queryForObject(
                "SELECT production_change_id FROM campaign_execution_handoff WHERE campaign_handoff_id = ?",
                UUID.class,
                handoffId
        ));
        assertEquals("HANDED_OFF", itemLifecycle.currentState(itemId));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_network_change WHERE campaign_handoff_id = ?",
                Integer.class,
                handoffId
        ));
    }

    @Test
    void standaloneOriginOnCampaignHandoffIsDenied() {
        UUID phase15 = verifiedPhase15ExecutionId();
        Map<String, Object> execution = jdbc.queryForMap(
                "SELECT plan_id, plan_fingerprint, execution_fingerprint, cell_id FROM network_change_execution WHERE id = ?",
                phase15
        );
        Map<String, Object> operation = jdbc.queryForMap(
                "SELECT expected_current_value, desired_value FROM network_change_execution_operation WHERE execution_id = ?",
                phase15
        );
        AuthenticatedActor creator = actor("creator-1", CampaignPermission.CAMPAIGN_CREATE);
        var created = campaignService.create(
                TARGET_ID,
                "campaign p16 standalone",
                "CC-P18-SD-" + UUID.randomUUID(),
                List.of(new ProductionChangeCampaignService.PlannedItem(
                        (UUID) execution.get("plan_id"),
                        String.valueOf(execution.get("plan_fingerprint")).toLowerCase(),
                        phase15,
                        String.valueOf(execution.get("execution_fingerprint")).toLowerCase(),
                        String.valueOf(execution.get("cell_id")),
                        new BigDecimal(String.valueOf(operation.get("expected_current_value"))),
                        new BigDecimal(String.valueOf(operation.get("desired_value"))),
                        new BigDecimal(String.valueOf(operation.get("expected_current_value"))),
                        "dBm",
                        "CELL",
                        "txPower"
                )),
                creator,
                "idem-p16-sd-" + UUID.randomUUID()
        );
        UUID campaignId = created.getCampaignId();
        campaignService.transition(campaignId, "SUBMIT_FOR_REVIEW", creator, "idem-p16-sd-s");
        campaignService.transition(campaignId, "REVIEW_APPROVE", actor("reviewer-1", CampaignPermission.CAMPAIGN_REVIEW), "idem-p16-sd-r");
        campaignService.transition(campaignId, "AUTHORIZE_CAMPAIGN", actor("authorizer-1", CampaignPermission.CAMPAIGN_AUTHORIZE), "idem-p16-sd-a");
        campaignService.evaluateReady(campaignId);
        campaignService.transition(campaignId, "RELEASE_CANARY", actor("releaser-1", CampaignPermission.RELEASE_COHORT), "idem-p16-sd-rel");
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        String handoffId = orchestration.commitForwardHandoff(campaignId, itemId);
        org.springframework.dao.DataIntegrityViolationException durable = assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update(
                        """
                        INSERT INTO production_network_change (
                            production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                            status, production_fingerprint, authorization_generation, cell_id, parameter,
                            expected_value, desired_value, requester_principal_id, audit_chain_integrity,
                            created_at, updated_at, version, execution_origin, campaign_handoff_id)
                        VALUES (?, ?, ?, 'CC-SD', 'REQUESTED', ?, 0, ?, 'txPower', 46, 43, 'req', 'VALID',
                                NOW(), NOW(), 0, 'STANDALONE', ?)
                        """,
                        UUID.randomUUID(), phase15, TARGET_ID, "e".repeat(64), execution.get("cell_id"), handoffId
                )
        );
        assertTrue(durable.getMostSpecificCause().getMessage().contains("production_network_change_origin_handoff_chk"));
        jdbc.update(
                "UPDATE campaign_execution_handoff SET binding_digest = ? WHERE campaign_handoff_id = ?",
                "f".repeat(64),
                handoffId
        );
        CampaignException conflict = assertThrows(CampaignException.class, () -> orchestration.executeForwardHandoff(campaignId, itemId));
        assertEquals(ProductionReasonCode.HANDOFF_IDEMPOTENCY_CONFLICT, conflict.reasonCode());
        CampaignException standalone = assertThrows(CampaignException.class, () ->
                handoffService.denyStandaloneDowngrade("PRODUCTION_CAMPAIGN"));
        assertEquals(ProductionReasonCode.STANDALONE_DOWNGRADE_DENIED, standalone.reasonCode());
    }

    private static AuthenticatedActor actor(String id, CampaignPermission permission) {
        return new AuthenticatedActor(id, ActorType.HUMAN, EnumSet.of(permission), CAMPAIGN_SOURCE, true);
    }
}
