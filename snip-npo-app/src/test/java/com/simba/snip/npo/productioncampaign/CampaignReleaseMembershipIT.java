package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.service.CampaignReleaseFingerprintFactory;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignReleaseMembershipIT extends ProductionCampaignITSupport {

    @Test
    void releasedMembershipInsertAndIdentityMutationFailClosed() {
        var released = authorizeReadyAndRelease("CELL-REL-IMM-1", "idem-rel-imm");
        UUID campaignId = released.getCampaignId();
        UUID cohortId = jdbc.queryForObject(
                "SELECT cohort_id FROM campaign_cohort WHERE campaign_id = ?", UUID.class, campaignId);
        UUID revisionId = jdbc.queryForObject(
                "SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        UUID existing = jdbc.queryForObject(
                "SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        String fp = jdbc.queryForObject(
                "SELECT release_fingerprint FROM campaign_cohort_release WHERE campaign_id = ?", String.class, campaignId);
        assertEquals(64, fp.length());

        assertThrows(DataAccessException.class, () -> jdbc.update(
                """
                INSERT INTO campaign_execution_item (
                    item_id, campaign_id, revision_id, cohort_id, item_sequence, production_target_id, object_type,
                    cell_id, parameter, expected_value, desired_value, rollback_value, unit, item_fingerprint,
                    state, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, 99, ?, 'CELL', 'CELL-EXTRA', 'txPower', 46, 43, 46, 'dBm', ?, 'PLANNED', NOW(), NOW(), 0)
                """,
                UUID.randomUUID(), campaignId, revisionId, cohortId, TARGET_ID, "a".repeat(64)
        ));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE campaign_execution_item SET item_fingerprint = ? WHERE item_id = ?",
                "b".repeat(64), existing
        ));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE campaign_execution_item SET item_sequence = 9 WHERE item_id = ?", existing
        ));
        jdbc.update("UPDATE campaign_execution_item SET state = 'ELIGIBLE' WHERE item_id = ?", existing);
        assertEquals("ELIGIBLE", jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = ?", String.class, existing));
    }

    @Test
    void durableReleaseFingerprintDiffersWhenRevisionOrAuthGenerationChanges() {
        var first = authorizeReadyAndRelease("CELL-REL-FP-1", "idem-rel-fp-1");
        String fp1 = jdbc.queryForObject(
                "SELECT release_fingerprint FROM campaign_cohort_release WHERE campaign_id = ?",
                String.class,
                first.getCampaignId()
        );
        var second = authorizeReadyAndRelease("CELL-REL-FP-2", "idem-rel-fp-2");
        jdbc.update("UPDATE campaign_revision SET authorization_generation = 99 WHERE campaign_id = ?", second.getCampaignId());
        jdbc.update("UPDATE production_change_campaign SET fingerprint = ? WHERE campaign_id = ?",
                "c".repeat(64), second.getCampaignId());
        String fp2 = jdbc.queryForObject(
                "SELECT release_fingerprint FROM campaign_cohort_release WHERE campaign_id = ?",
                String.class,
                second.getCampaignId()
        );
        assertNotEquals(fp1, fp2);
        assertTrue(fp1.matches("^[0-9a-f]{64}$"));
        assertEquals(CampaignReleaseFingerprintFactory.NONE_DIGEST.length(), 64);
    }
}
