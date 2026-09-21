package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.service.CampaignRecoveryService;
import com.simba.snip.npo.productioncampaign.service.ForwardProgressionClosureService;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CampaignRecoveryRebindIT extends ProductionCampaignITSupport {

    @Autowired CampaignRecoveryService recoveryService;
    @Autowired ForwardProgressionClosureService closureService;
    @Autowired com.simba.snip.npo.productioncampaign.service.CampaignMutationSlotService slotService;

    @Test
    void requestedRollbackCannotRebind() {
        var campaign = newCampaign("CELL-RBND-1", "idem-rbnd-1");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        slotService.acquireForward(campaignId, revisionId, itemId, 1L, 1L);
        jdbc.update("UPDATE campaign_execution_item SET state = 'MAY_HAVE_SENT' WHERE item_id = ?", itemId);
        closureService.determineRecoveryRequired(campaignId, itemId);
        recoveryService.request(campaignId, actor("req-r", CampaignPermission.CAMPAIGN_RECOVERY_REQUEST));
        recoveryService.startReview(campaignId, actor("rev-r", CampaignPermission.CAMPAIGN_RECOVERY_REVIEW));
        recoveryService.authorize(campaignId, actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE));
        insertP16Rollback(campaignId, "REQUESTED");
        CampaignException requested = assertThrows(CampaignException.class, () ->
                recoveryService.rebindHolderToRecovery(
                        campaignId, revisionId, actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE), 1L, 1L));
        assertEquals(ProductionReasonCode.INVALID_RECOVERY_TRANSITION, requested.reasonCode());
        jdbc.update("UPDATE production_execution_rollback SET status = 'AUTHORIZED' WHERE production_change_id IN (SELECT production_change_id FROM campaign_execution_item WHERE campaign_id = ?)", campaignId);
        recoveryService.rebindHolderToRecovery(
                campaignId, revisionId, actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE), 1L, 1L);
        assertEquals("RECOVERY", slotService.snapshot(campaignId).holderType());
    }

    @Test
    void openClosureDeniesRebindEvenIfAuthorized() {
        var campaign = newCampaign("CELL-RBND-2", "idem-rbnd-2");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        insertP16Rollback(campaignId, "AUTHORIZED");
        CampaignException open = assertThrows(CampaignException.class, () ->
                recoveryService.rebindHolderToRecovery(
                        campaignId, revisionId, actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE), 1L, 1L));
        assertEquals(ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE, open.reasonCode());
    }

    private void insertP16Rollback(UUID campaignId, String status) {
        UUID changeId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, rollback_expected_value, rollback_desired_value,
                    requester_principal_id, authorizer_principal_id, executor_principal_id,
                    audit_chain_integrity, created_at, updated_at, version)
                SELECT ?, ?, production_target_id, 'CC-RBND', 'AUTHORIZED', ?, 1, 'CELL-RBND', 'txPower',
                       46, 43, 46, 43, 'p16-req', 'p16-auth', 'p16-exec', 'VALID', NOW(), NOW(), 0
                  FROM production_change_campaign WHERE campaign_id = ?
                """,
                changeId, UUID.randomUUID(), "c".repeat(64), campaignId
        );
        jdbc.update("UPDATE campaign_execution_item SET production_change_id = ? WHERE campaign_id = ?", changeId, campaignId);
        jdbc.update(
                """
                INSERT INTO production_execution_rollback (
                    rollback_id, production_change_id, status, rollback_fingerprint, authorization_generation,
                    requester_principal_id, reviewer_principal_id, authorizer_principal_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 'p16-req', 'p16-rev', 'p16-auth', NOW(), NOW())
                """,
                UUID.randomUUID(), changeId, status, "d".repeat(64)
        );
    }
}
