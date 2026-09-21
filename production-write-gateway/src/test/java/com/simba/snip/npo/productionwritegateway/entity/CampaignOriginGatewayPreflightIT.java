package com.simba.snip.npo.productionwritegateway.entity;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.AbstractGatewayPostgresIT;
import com.simba.snip.npo.productionwritegateway.GatewayTestFixture;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import com.simba.snip.npo.productionwritegateway.service.CampaignOriginGatewayPreflightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CampaignOriginGatewayPreflightIT extends AbstractGatewayPostgresIT {

    @Autowired CampaignOriginGatewayPreflightService preflight;
    @Autowired JdbcTemplate jdbc;

    @Test
    void closureClosedDeniesForwardSend() {
        SeededCampaign seeded = seed("CELL-GW-1");
        jdbc.update("UPDATE campaign_forward_progression_closure SET state = 'CLOSED', closed_at = NOW() WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.FORWARD_PROGRESSION_CLOSED, ex.reasonCode());
    }

    @Test
    void missingSlotDenies() {
        SeededCampaign seeded = seed("CELL-GW-2");
        jdbc.update("UPDATE campaign_mutation_slot SET state = 'EMPTY', holder_type = 'NONE', holder_item_id = NULL, reservation_id = NULL WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE, ex.reasonCode());
    }

    @Test
    void wrongSlotHolderDenies() {
        SeededCampaign seeded = seed("CELL-GW-3");
        jdbc.update("UPDATE campaign_mutation_slot SET holder_item_id = ? WHERE campaign_id = ?", UUID.randomUUID(), seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE, ex.reasonCode());
    }

    @Test
    void missingSafetyReservationDenies() {
        SeededCampaign seeded = seed("CELL-GW-4");
        jdbc.update("UPDATE campaign_safety_budget_reservation SET state = 'STALE' WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.SAFETY_RESERVATION_STALE, ex.reasonCode());
    }

    @Test
    void staleControlGenerationDenies() {
        SeededCampaign seeded = seed("CELL-GW-5");
        jdbc.update("UPDATE campaign_control_generation SET current_generation = current_generation + 1 WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, ex.reasonCode());
    }

    @Test
    void staleReleaseGenerationDenies() {
        SeededCampaign seeded = seed("CELL-GW-6");
        jdbc.update("UPDATE campaign_release_generation SET current_generation = current_generation + 1 WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, ex.reasonCode());
    }

    @Test
    void staleFenceDenies() {
        SeededCampaign seeded = seed("CELL-GW-7");
        jdbc.update("UPDATE campaign_lease SET fencing_token = fencing_token + 1 WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_LEASE_UNAVAILABLE, ex.reasonCode());
    }

    @Test
    void inactiveReleaseDenies() {
        SeededCampaign seeded = seed("CELL-GW-8");
        jdbc.update("UPDATE campaign_cohort_release SET state = 'STALE' WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, ex.reasonCode());
    }

    @Test
    void ineligibleItemDenies() {
        SeededCampaign seeded = seed("CELL-GW-9");
        jdbc.update("UPDATE campaign_execution_item SET state = 'BLOCKED' WHERE item_id = ?", seeded.itemId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, ex.reasonCode());
    }

    @Test
    void bindingRollbackMismatchDenies() {
        SeededCampaign seeded = seed("CELL-GW-10");
        seeded.change.setRollbackExpectedValue(new BigDecimal("1"));
        seeded.change.setRollbackDesiredValue(new BigDecimal("1"));
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH, ex.reasonCode());
    }

    @Test
    void bindingUnitMismatchDenies() {
        SeededCampaign seeded = seed("CELL-GW-11");
        jdbc.update("UPDATE campaign_execution_binding SET unit = 'W' WHERE item_id = ?", seeded.itemId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH, ex.reasonCode());
    }

    @Test
    void fingerprintMismatchDenies() {
        SeededCampaign seeded = seed("CELL-GW-12");
        jdbc.update("UPDATE production_change_campaign SET fingerprint = ? WHERE campaign_id = ?", "a".repeat(64), seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.PRODUCTION_FINGERPRINT_MISMATCH, ex.reasonCode());
    }

    @Test
    void dbCurrentnessFailureDenies() {
        ProductionNetworkChangeEntity change = new ProductionNetworkChangeEntity();
        change.setProductionChangeId(UUID.randomUUID());
        change.setCellId("CELL-MISSING");
        change.setParameter("txPower");
        change.setExpectedValue(new BigDecimal("46"));
        change.setDesiredValue(new BigDecimal("43"));
        change.setRollbackExpectedValue(new BigDecimal("46"));
        change.bindPersistedOrigin("PRODUCTION_CAMPAIGN", "b".repeat(64));
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(change, UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH, ex.reasonCode());
    }

    @Test
    void expiredLeaseDenies() {
        SeededCampaign seeded = seed("CELL-GW-13");
        jdbc.update("UPDATE campaign_lease SET expires_at = NOW() - INTERVAL '1 hour' WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_LEASE_UNAVAILABLE, ex.reasonCode());
    }

    @Test
    void abortedCampaignDeniesOnSendPath() {
        SeededCampaign seeded = seed("CELL-GW-14");
        jdbc.update("UPDATE production_change_campaign SET abort_state = 'ABORTED' WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, ex.reasonCode());
    }

    @Test
    void cohortKillDeniesIndependently() {
        SeededCampaign seeded = seed("CELL-GW-15");
        jdbc.update("UPDATE campaign_cohort SET state = 'STALE' WHERE campaign_id = ?", seeded.campaignId);
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, ex.reasonCode());
    }

    @Test
    void targetKillDeniesIndependently() {
        SeededCampaign seeded = seed("CELL-GW-16");
        jdbc.update(
                "UPDATE production_network_target SET enabled = FALSE WHERE target_id = (SELECT production_target_id FROM production_change_campaign WHERE campaign_id = ?)",
                seeded.campaignId
        );
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.PRODUCTION_TARGET_DISABLED, ex.reasonCode());
    }

    @Test
    void revokedOnboardingDenies() {
        SeededCampaign seeded = seed("CELL-GW-17");
        jdbc.update(
                "UPDATE production_target_onboarding SET status = 'REVOKED' WHERE production_target_id = (SELECT production_target_id FROM production_change_campaign WHERE campaign_id = ?)",
                seeded.campaignId
        );
        GatewayDeniedException ex = assertThrows(GatewayDeniedException.class, () -> preflight.validate(seeded.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, ex.reasonCode());
    }

    @Test
    void validPreflightAdvancesHandedOffItemToPreSend() {
        SeededCampaign seeded = seed("CELL-GW-18");
        preflight.validate(seeded.change(), UUID.randomUUID());
        assertEquals("PRE_SEND", jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = ?",
                String.class,
                seeded.itemId
        ));
        preflight.recordMayHaveSent(seeded.change());
        assertEquals("MAY_HAVE_SENT", jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = ?",
                String.class,
                seeded.itemId
        ));
    }

    @Test
    void eligibleAndHandoffPendingDenied() {
        SeededCampaign eligible = seed("CELL-GW-19");
        jdbc.update("UPDATE campaign_execution_item SET state = 'ELIGIBLE' WHERE item_id = ?", eligible.itemId);
        GatewayDeniedException eligibleEx = assertThrows(GatewayDeniedException.class,
                () -> preflight.validate(eligible.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, eligibleEx.reasonCode());
        SeededCampaign pending = seed("CELL-GW-20");
        jdbc.update("UPDATE campaign_execution_item SET state = 'HANDOFF_PENDING' WHERE item_id = ?", pending.itemId);
        GatewayDeniedException pendingEx = assertThrows(GatewayDeniedException.class,
                () -> preflight.validate(pending.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, pendingEx.reasonCode());
    }

    @Test
    void releaseFingerprintAndRevisionAndItemFingerprintMismatchDenied() {
        SeededCampaign fp = seed("CELL-GW-21");
        jdbc.update("UPDATE campaign_cohort_release SET release_fingerprint = ? WHERE campaign_id = ?",
                "a".repeat(64), fp.campaignId);
        GatewayDeniedException releaseEx = assertThrows(GatewayDeniedException.class,
                () -> preflight.validate(fp.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.PRODUCTION_FINGERPRINT_MISMATCH, releaseEx.reasonCode());
        SeededCampaign rev = seed("CELL-GW-22");
        jdbc.update("UPDATE production_change_campaign SET current_revision_number = 2 WHERE campaign_id = ?", rev.campaignId);
        GatewayDeniedException revEx = assertThrows(GatewayDeniedException.class,
                () -> preflight.validate(rev.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, revEx.reasonCode());
        SeededCampaign item = seed("CELL-GW-23");
        jdbc.update("UPDATE campaign_execution_binding SET item_fingerprint = ? WHERE item_id = ?",
                "b".repeat(64), item.itemId);
        GatewayDeniedException itemEx = assertThrows(GatewayDeniedException.class,
                () -> preflight.validate(item.change(), UUID.randomUUID()));
        assertEquals(ProductionReasonCode.PRODUCTION_FINGERPRINT_MISMATCH, itemEx.reasonCode());
    }

    @Test
    void recordMayHaveSentFromHandedOffIsImpossible() {
        SeededCampaign seeded = seed("CELL-GW-24");
        assertEquals("HANDED_OFF", jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = ?", String.class, seeded.itemId));
        preflight.recordMayHaveSent(seeded.change());
        assertEquals("HANDED_OFF", jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = ?", String.class, seeded.itemId));
    }

    private SeededCampaign seed(String cellId) {
        String targetId = "target-" + UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID cohortId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        String fp = Integer.toHexString(cellId.hashCode());
        fp = (fp + "c".repeat(64)).substring(0, 64);
        String handoff = Integer.toHexString(campaignId.hashCode());
        handoff = (handoff + "d".repeat(64)).substring(0, 64);
        Instant now = Instant.now();
        Timestamp ts = Timestamp.from(now);
        jdbc.update("""
                INSERT INTO production_network_target (
                    target_id, vendor, platform, environment, adapter_profile_id, capability_profile_version,
                    security_profile_id, credential_profile_id, allowed_object_types, allowed_parameters,
                    change_window_policy, verification_policy, certification_level, enabled, target_state,
                    target_fingerprint, expected_state_guard_strength, created_at, updated_at, version)
                VALUES (?, 'ERICSSON', 'ENM', 'LAB', 'ERICSSON_ENM_LAB_ADAPTER', '1',
                    'STRICT_TLS', 'ericsson-enm-lab-write', 'CELL', 'txPower',
                    'ALWAYS_OPEN', 'ALLOW_READ_THEN_WRITE', 'L0', TRUE, 'ACTIVE',
                    ?, 'READ_THEN_WRITE', ?, ?, 0)
                """, targetId, GatewayTestFixture.HASH_A, ts, ts);
        jdbc.update("""
                INSERT INTO production_change_campaign (
                    campaign_id, production_target_id, vendor, platform, environment, objective,
                    change_control_reference, state, enabled, creator_principal_id, current_revision_number,
                    fingerprint, abort_state, created_at, updated_at, version)
                VALUES (?, ?, 'ERICSSON', 'ENM', 'LAB', 'obj', 'CC', 'CANARY_ACTIVE', TRUE, 'creator', 1, ?, 'NOT_ABORTED', ?, ?, 0)
                """, campaignId, targetId, fp, ts, ts);
        jdbc.update("""
                INSERT INTO campaign_revision (revision_id, campaign_id, revision_number, fingerprint, authorization_generation, state, policy_versions, created_at, updated_at, version)
                VALUES (?, ?, 1, ?, 1, 'AUTHORIZED', '{}', ?, ?, 0)
                """, revisionId, campaignId, fp, ts, ts);
        jdbc.update("""
                INSERT INTO campaign_cohort (cohort_id, campaign_id, revision_id, cohort_sequence, cohort_type, state, created_at, updated_at, version)
                VALUES (?, ?, ?, 1, 'CANARY', 'PLANNED', ?, ?, 0)
                """, cohortId, campaignId, revisionId, ts, ts);
        jdbc.update("""
                INSERT INTO campaign_execution_item (
                    item_id, campaign_id, revision_id, cohort_id, item_sequence, production_target_id, object_type,
                    cell_id, parameter, expected_value, desired_value, rollback_value, unit, item_fingerprint,
                    state, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, 1, ?, 'CELL', ?, 'txPower', 46, 43, 46, 'dBm', ?, 'HANDED_OFF', ?, ?, 0)
                """, itemId, campaignId, revisionId, cohortId, targetId, cellId, fp, ts, ts);
        jdbc.update("UPDATE campaign_cohort SET state = 'RELEASED' WHERE cohort_id = ?", cohortId);
        jdbc.update("INSERT INTO campaign_control_generation (campaign_id, current_generation, updated_at, version) VALUES (?, 1, ?, 0)", campaignId, ts);
        jdbc.update("INSERT INTO campaign_release_generation (campaign_id, current_generation, updated_at, version) VALUES (?, 1, ?, 0)", campaignId, ts);
        jdbc.update("INSERT INTO campaign_lease (campaign_id, fencing_token, status, version) VALUES (?, 7, 'HELD', 0)", campaignId);
        jdbc.update("""
                INSERT INTO production_target_onboarding (
                    onboarding_id, production_target_id, status, certification_level,
                    created_by, reviewed_by, approved_by, created_at, updated_at)
                VALUES (?, ?, 'APPROVED', 'L0', 'onb-create', 'onb-review', 'onb-approve', ?, ?)
                """, UUID.randomUUID(), targetId, ts, ts);
        jdbc.update("INSERT INTO campaign_forward_progression_closure (campaign_id, revision_id, state, version) VALUES (?, ?, 'OPEN', 0)", campaignId, revisionId);
        jdbc.update("""
                INSERT INTO campaign_cohort_release (
                    release_id, campaign_id, revision_id, cohort_id, release_generation, control_generation,
                    campaign_fence, release_fingerprint, state, releaser_principal_id, released_at)
                VALUES (?, ?, ?, ?, 1, 1, 7, ?, 'ACTIVE', 'releaser', ?)
                """, releaseId, campaignId, revisionId, cohortId, fp, ts);
        jdbc.update("""
                INSERT INTO campaign_safety_budget_reservation (
                    reservation_id, campaign_id, revision_id, item_id, budget_type, state, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, 'FORWARD', 'RESERVED', ?, ?, 0)
                """, reservationId, campaignId, revisionId, itemId, ts, ts);
        jdbc.update("""
                INSERT INTO campaign_mutation_slot (
                    campaign_id, holder_type, holder_item_id, reservation_id, campaign_fencing_token,
                    campaign_control_generation, state, version)
                VALUES (?, 'FORWARD', ?, ?, 7, 1, 'ACQUIRED', 0)
                """, campaignId, itemId, reservationId);
        jdbc.update("""
                INSERT INTO campaign_execution_binding (
                    binding_id, campaign_id, revision_id, revision_number, campaign_fingerprint, control_generation,
                    cohort_id, release_fingerprint, release_generation, item_id, item_sequence, item_fingerprint,
                    campaign_fence, production_target_id, cell_id, parameter, expected_value, desired_value,
                    rollback_value, unit, binding_digest, created_at)
                VALUES (?, ?, ?, 1, ?, 1, ?, ?, 1, ?, 1, ?, 7, ?, ?, 'txPower', 46, 43, 46, 'dBm', ?, ?)
                """, bindingId, campaignId, revisionId, fp, cohortId, fp, itemId, fp, targetId, cellId, fp, ts);
        jdbc.update("""
                INSERT INTO campaign_execution_handoff (
                    campaign_handoff_id, campaign_id, revision_id, item_id, binding_id, binding_digest, lineage_type, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'FORWARD', ?)
                """, handoff, campaignId, revisionId, itemId, bindingId, fp, ts);
        ProductionNetworkChangeEntity change = new ProductionNetworkChangeEntity();
        change.setProductionChangeId(UUID.randomUUID());
        change.setCellId(cellId);
        change.setParameter("txPower");
        change.setExpectedValue(new BigDecimal("46"));
        change.setDesiredValue(new BigDecimal("43"));
        change.setRollbackExpectedValue(new BigDecimal("46"));
        change.bindPersistedOrigin("PRODUCTION_CAMPAIGN", handoff);
        return new SeededCampaign(campaignId, itemId, change);
    }

    private record SeededCampaign(UUID campaignId, UUID itemId, ProductionNetworkChangeEntity change) {
    }
}
