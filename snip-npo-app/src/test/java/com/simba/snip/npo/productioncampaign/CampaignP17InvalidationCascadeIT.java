package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.service.CampaignHandoffOrchestrationService;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.vendorcertification.service.CertificationInvalidationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignP17InvalidationCascadeIT extends ProductionCampaignITSupport {

    @Autowired CertificationInvalidationService invalidationService;
    @Autowired CampaignHandoffOrchestrationService orchestration;
    @Autowired com.simba.snip.npo.productioncampaign.service.CampaignItemLifecycleService itemLifecycle;
    @Autowired com.simba.snip.npo.productioncampaign.service.CampaignGovernedProgressionService governedProgression;

    @ParameterizedTest
    @EnumSource(
            value = CertificationInvalidationService.TriggerType.class,
            names = {
                    "CERTIFICATION_REVOKED",
                    "CERTIFICATION_EXPIRED",
                    "INTERFACE_REVOKED",
                    "TARGET_ONBOARDING_CHANGED",
                    "SECURITY_PROFILE_CHANGED",
                    "CREDENTIAL_PROFILE_CHANGED",
                    "TARGET_SUSPENDED"
            }
    )
    void p17InvalidationSuspendsCampaignAndIsIdempotent(CertificationInvalidationService.TriggerType trigger) {
        String suffix = trigger.name().replace('_', '-');
        var released = authorizeReadyAndRelease("CELL-P17C-" + suffix, "idem-p17c-" + suffix);
        UUID campaignId = released.getCampaignId();
        long controlBefore = jdbc.queryForObject(
                "SELECT current_generation FROM campaign_control_generation WHERE campaign_id = ?", Long.class, campaignId);
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        jdbc.update("UPDATE campaign_execution_item SET state = 'MAY_HAVE_SENT' WHERE item_id = ?", itemId);
        CertificationInvalidationService.InvalidationResult first = invalidationService.invalidate(
                new CertificationInvalidationService.InvalidationCommand(
                        trigger,
                        "transport_certification",
                        UUID.randomUUID().toString(),
                        UUID.randomUUID(),
                        "REVOKED",
                        Instant.now(),
                        TARGET_ID,
                        com.simba.snip.npo.productionchange.domain.ActorPrincipal.of("p17-invalidator")
                )
        );
        assertTrue(first.applied());
        assertEquals("SUSPENDED", jdbc.queryForObject(
                "SELECT state FROM production_change_campaign WHERE campaign_id = ?", String.class, campaignId));
        assertEquals("SAFETY_SUSPENSION", jdbc.queryForObject(
                "SELECT suspension_type FROM campaign_suspension WHERE campaign_id = ? ORDER BY created_at DESC LIMIT 1",
                String.class, campaignId));
        long controlAfter = jdbc.queryForObject(
                "SELECT current_generation FROM campaign_control_generation WHERE campaign_id = ?", Long.class, campaignId);
        assertTrue(controlAfter > controlBefore);
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_cohort_release WHERE campaign_id = ? AND state = 'ACTIVE'",
                Integer.class, campaignId));
        assertTrue(jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_cohort_release WHERE campaign_id = ? AND state = 'STALE'",
                Integer.class, campaignId) >= 1);
        CampaignException releaseDenied = assertThrows(CampaignException.class, () ->
                campaignService.transition(campaignId, "RELEASE_CANARY", actor("releaser-1", CampaignPermission.RELEASE_COHORT), "idem-p17c-rel2-" + suffix));
        assertTrue(releaseDenied.reasonCode() == ProductionReasonCode.CAMPAIGN_STALE
                || releaseDenied.reasonCode() == ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION);
        CampaignException handoffDenied = assertThrows(CampaignException.class, () ->
                orchestration.commitForwardHandoff(campaignId, itemId));
        assertTrue(handoffDenied.reasonCode() == ProductionReasonCode.CAMPAIGN_STALE
                || handoffDenied.reasonCode() == ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION);
        assertEquals("MAY_HAVE_SENT", itemLifecycle.currentState(itemId));
        assertEquals("VENDOR_ACCEPTED", governedProgression.recordAcceptance(itemId));
        CertificationInvalidationService.InvalidationResult second = invalidationService.invalidate(
                new CertificationInvalidationService.InvalidationCommand(
                        trigger,
                        "transport_certification",
                        UUID.randomUUID().toString(),
                        UUID.randomUUID(),
                        "REVOKED",
                        Instant.now().plusSeconds(1),
                        TARGET_ID,
                        com.simba.snip.npo.productionchange.domain.ActorPrincipal.of("p17-invalidator")
                )
        );
        assertTrue(second.applied() || second.idempotentReplay());
        long controlRepeat = jdbc.queryForObject(
                "SELECT current_generation FROM campaign_control_generation WHERE campaign_id = ?", Long.class, campaignId);
        assertEquals(controlAfter, controlRepeat);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_suspension WHERE campaign_id = ? AND reason_code = 'P17_INVALIDATION'",
                Integer.class, campaignId));
    }
}
