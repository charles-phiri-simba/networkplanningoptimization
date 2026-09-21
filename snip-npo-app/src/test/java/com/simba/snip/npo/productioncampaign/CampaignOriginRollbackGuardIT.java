package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.LeaseHandle;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import com.simba.snip.npo.productionchange.service.ProductionExecutionGrantService;
import com.simba.snip.npo.productionchange.service.ProductionRollbackAuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CampaignOriginRollbackGuardIT extends ProductionCampaignITSupport {

    @Autowired ProductionRollbackAuthorizationService rollbackAuthorizationService;
    @Autowired ProductionExecutionGrantService grantService;
    @Autowired ProductionNetworkChangeRepository changeRepository;
    @Autowired com.simba.snip.npo.productioncampaign.service.CampaignHandoffOrchestrationService orchestration;
    @Autowired com.simba.snip.npo.productioncampaign.service.CampaignRecoveryService recoveryService;
    @Autowired com.simba.snip.npo.productioncampaign.service.ForwardProgressionClosureService closureService;

    @Test
    void campaignOriginOpenClosureDeniesRollbackAuthorizeAndGrant() {
        var released = authorizeReadyAndRelease("CELL-RB-OPEN-1", "idem-rb-open");
        UUID campaignId = released.getCampaignId();
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        String handoffId = orchestration.commitForwardHandoff(campaignId, itemId);
        UUID changeId = insertCampaignOriginChange(campaignId, handoffId, "CELL-RB-OPEN-1", "ROLLBACK_REVIEWED");
        insertRollback(changeId, "REVIEWED");
        ProductionChangeException authorize = assertThrows(ProductionChangeException.class, () ->
                rollbackAuthorizationService.authorize(changeId, ActorPrincipal.of("p16-auth")));
        assertEquals(ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE, authorize.reasonCode());
        ProductionNetworkChangeEntity change = changeRepository.findById(changeId).orElseThrow();
        ProductionChangeException grant = assertThrows(ProductionChangeException.class, () ->
                grantService.issue(change, lease(change), GrantType.ROLLBACK, ActorPrincipal.of("p16-exec")));
        assertEquals(ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE, grant.reasonCode());
    }

    @Test
    void closedWithoutCampaignRecoveryStillDeniesThenAuthorizedPathProceeds() {
        var released = authorizeReadyAndRelease("CELL-RB-CL-1", "idem-rb-cl");
        UUID campaignId = released.getCampaignId();
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        String handoffId = orchestration.commitForwardHandoff(campaignId, itemId);
        jdbc.update("UPDATE campaign_execution_item SET state = 'MAY_HAVE_SENT' WHERE item_id = ?", itemId);
        closureService.determineRecoveryRequired(campaignId, itemId);
        UUID changeId = insertCampaignOriginChange(campaignId, handoffId, "CELL-RB-CL-1", "ROLLBACK_REVIEWED");
        insertRollback(changeId, "REVIEWED");
        ProductionChangeException missingRecovery = assertThrows(ProductionChangeException.class, () ->
                rollbackAuthorizationService.authorize(changeId, ActorPrincipal.of("p16-auth")));
        assertEquals(ProductionReasonCode.INVALID_RECOVERY_TRANSITION, missingRecovery.reasonCode());
        recoveryService.request(campaignId, actor("req-r", CampaignPermission.CAMPAIGN_RECOVERY_REQUEST));
        recoveryService.startReview(campaignId, actor("rev-r", CampaignPermission.CAMPAIGN_RECOVERY_REVIEW));
        recoveryService.authorize(campaignId, actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE));
        rollbackAuthorizationService.authorize(changeId, ActorPrincipal.of("p16-auth"));
        assertEquals("ROLLBACK_AUTHORIZED", jdbc.queryForObject(
                "SELECT status FROM production_network_change WHERE production_change_id = ?", String.class, changeId));
        assertThrows(Exception.class, () -> jdbc.update(
                "UPDATE campaign_forward_progression_closure SET state = 'OPEN' WHERE campaign_id = ? AND state = 'CLOSED'",
                campaignId
        ));
        assertEquals("CLOSED", jdbc.queryForObject(
                "SELECT state FROM campaign_forward_progression_closure WHERE campaign_id = ?", String.class, campaignId));
    }

    @Test
    void standaloneRollbackAuthorizeUnchanged() {
        UUID changeId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, rollback_expected_value, rollback_desired_value,
                    requester_principal_id, audit_chain_integrity, created_at, updated_at, version,
                    execution_origin)
                VALUES (?, ?, ?, 'CC-SA', 'ROLLBACK_REVIEWED', ?, 1, 'CELL-SA-1', 'txPower',
                        46, 43, 46, 43, 'p16-req', 'VALID', NOW(), NOW(), 0, 'STANDALONE')
                """,
                changeId, UUID.randomUUID(), TARGET_ID, "e".repeat(64)
        );
        insertRollback(changeId, "REVIEWED");
        rollbackAuthorizationService.authorize(changeId, ActorPrincipal.of("p16-auth"));
        assertEquals("ROLLBACK_AUTHORIZED", jdbc.queryForObject(
                "SELECT status FROM production_network_change WHERE production_change_id = ?", String.class, changeId));
    }

    private UUID insertCampaignOriginChange(UUID campaignId, String handoffId, String cellId, String status) {
        UUID changeId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, rollback_expected_value, rollback_desired_value,
                    requester_principal_id, audit_chain_integrity, created_at, updated_at, version,
                    execution_origin, campaign_handoff_id)
                SELECT ?, ?, production_target_id, 'CC-RB', ?, ?, 1, ?, 'txPower',
                       46, 43, 46, 43, 'p16-req', 'VALID', NOW(), NOW(), 0, 'PRODUCTION_CAMPAIGN', ?
                  FROM production_change_campaign WHERE campaign_id = ?
                """,
                changeId, UUID.randomUUID(), status, "e".repeat(64), cellId, handoffId, campaignId
        );
        jdbc.update("UPDATE campaign_execution_item SET production_change_id = ? WHERE campaign_id = ?", changeId, campaignId);
        return changeId;
    }

    private void insertRollback(UUID changeId, String status) {
        jdbc.update(
                """
                INSERT INTO production_execution_rollback (
                    rollback_id, production_change_id, status, rollback_fingerprint, authorization_generation,
                    requester_principal_id, reviewer_principal_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 'p16-req', 'p16-rev', NOW(), NOW())
                """,
                UUID.randomUUID(), changeId, status, "d".repeat(64)
        );
    }

    private static LeaseHandle lease(ProductionNetworkChangeEntity change) {
        Instant now = Instant.now();
        return new LeaseHandle(
                UUID.randomUUID(),
                change.getProductionTargetId(),
                change.getCellId(),
                change.getParameter(),
                change.getProductionChangeId().toString(),
                1L,
                now,
                now.plusSeconds(60)
        );
    }
}
