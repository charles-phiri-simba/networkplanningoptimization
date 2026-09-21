package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.service.CampaignGovernedProgressionService;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CampaignProofFormAIT extends ProductionCampaignITSupport {

    @Autowired CampaignGovernedProgressionService governedProgression;

    @Test
    void verifiedValueRequiredAndComparedToCanonical() {
        var campaign = newCampaign("CELL-PFA-1", "idem-pfa-1");
        UUID campaignId = campaign.getCampaignId();
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        UUID cohortId = jdbc.queryForObject("SELECT cohort_id FROM campaign_cohort WHERE campaign_id = ?", UUID.class, campaignId);
        jdbc.update("UPDATE campaign_execution_item SET state = 'RECONCILIATION_PENDING' WHERE item_id = ?", itemId);

        Instant verifiedAt = Instant.parse("2026-06-01T00:00:00Z");
        insertBoundary(campaignId, revisionId, cohortId, itemId, verifiedAt, null);
        CampaignException missing = assertThrows(CampaignException.class, () -> governedProgression.applyProofFormA(itemId));
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, missing.reasonCode());
        assertEquals("RECONCILIATION_PENDING", jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = ?", String.class, itemId));

        jdbc.update("DELETE FROM campaign_observation_boundary WHERE item_id = ?", itemId);
        insertBoundary(campaignId, revisionId, cohortId, itemId, verifiedAt, new BigDecimal("43.000"));
        CampaignException noCanonical = assertThrows(CampaignException.class, () -> governedProgression.applyProofFormA(itemId));
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, noCanonical.reasonCode());

        UUID gnb = jdbc.queryForObject("SELECT gnb_id FROM cell WHERE cell_id = 'CELL-001'", UUID.class);
        UUID cellPk = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO cell (id, cell_id, name, gnb_id, technology, band, status) VALUES (?, 'CELL-PFA-1', 'pfa', ?, 'NR', 'n78', 'ACTIVE')",
                cellPk, gnb
        );
        jdbc.update(
                "INSERT INTO radio_configuration (id, cell_id, parameter_name, parameter_value, unit, effective_from) VALUES (?, ?, 'txPower', '40', 'dBm', ?)",
                UUID.randomUUID(), cellPk, Timestamp.from(verifiedAt.plusSeconds(10))
        );
        CampaignException mismatch = assertThrows(CampaignException.class, () -> governedProgression.applyProofFormA(itemId));
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, mismatch.reasonCode());

        jdbc.update("UPDATE radio_configuration SET parameter_value = '43.00' WHERE cell_id = ?", cellPk);
        UUID checkpointId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO synchronization_checkpoint (
                    id, source_system, connector_id, synchronization_scope, checkpoint_type, checkpoint_value,
                    fencing_token, status, created_at, updated_at, last_observed_at)
                VALUES (?, 'ERICSSON_ENM', 'p18', 'CELL-PFA-1', 'INCREMENTAL', 'v1', 1, 'VALID', NOW(), NOW(), ?)
                """,
                checkpointId, Timestamp.from(verifiedAt.plusSeconds(5))
        );
        jdbc.update("UPDATE campaign_observation_boundary SET required_canonical_checkpoint = ? WHERE item_id = ?",
                checkpointId.toString(), itemId);
        assertEquals("CANONICAL_RECONCILED", governedProgression.applyProofFormA(itemId));

        jdbc.update("UPDATE campaign_execution_item SET state = 'RECONCILIATION_PENDING' WHERE item_id = ?", itemId);
        jdbc.update("UPDATE synchronization_checkpoint SET status = 'EXPIRED' WHERE id = ?", checkpointId);
        CampaignException stale = assertThrows(CampaignException.class, () -> governedProgression.applyProofFormA(itemId));
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, stale.reasonCode());
        jdbc.update("DELETE FROM radio_configuration WHERE cell_id = ?", cellPk);
    }

    private void insertBoundary(
            UUID campaignId, UUID revisionId, UUID cohortId, UUID itemId, Instant verifiedAt, BigDecimal verified
    ) {
        jdbc.update(
                """
                INSERT INTO campaign_observation_boundary (
                    boundary_id, campaign_id, revision_id, cohort_id, item_id, cell_id, parameter, verified_value,
                    vendor_verified_at, observation_start, source, minimum_measurement_time, watermark_type,
                    watermark_value, required_canonical_checkpoint, policy_versions, network_observation_health,
                    operational_safety_health, boundary_digest, created_at)
                VALUES (?, ?, ?, ?, ?, 'CELL-PFA-1', 'txPower', ?, ?, ?, 'src', ?, 'PHASE12_CHECKPOINT', 'ckpt-pfa',
                        'ckpt-pfa', '{"health":"p18-health-v1"}', 'HEALTHY', 'SAFE', ?, NOW())
                """,
                UUID.randomUUID(), campaignId, revisionId, cohortId, itemId, verified,
                Timestamp.from(verifiedAt), Timestamp.from(verifiedAt.plusSeconds(1)),
                Timestamp.from(verifiedAt.plusSeconds(1)), "a".repeat(64)
        );
    }
}
