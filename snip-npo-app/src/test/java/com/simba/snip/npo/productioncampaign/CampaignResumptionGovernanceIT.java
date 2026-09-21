package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignResumptionGovernanceIT extends ProductionCampaignITSupport {

    @Autowired TestRestTemplate http;
    @Autowired com.simba.snip.npo.productioncampaign.service.CampaignObservationService observationService;

    @Test
    void httpMakeEffectiveIsSystemDenied() {
        var released = authorizeReadyAndRelease("CELL-RES-HTTP-1", "idem-res-http");
        campaignService.transition(released.getCampaignId(), "PAUSE_CAMPAIGN",
                actor("pauser", CampaignPermission.CAMPAIGN_PAUSE), "idem-res-http-p");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.add("X-SNIP-CAMPAIGN-PERMISSION", CampaignPermission.CAMPAIGN_RESUMPTION_AUTHORIZE.name());
        headers.add("X-SNIP-CAMPAIGN-ACTOR-ID", "resume-auth");
        headers.add("X-SNIP-CAMPAIGN-AUTH-SOURCE", SOURCE);
        headers.add("X-SNIP-CAMPAIGN-ACTOR-TYPE", "HUMAN");
        ResponseEntity<String> response = http.exchange(
                "/api/v1/production-campaigns/" + released.getCampaignId() + "/resumption/effective",
                HttpMethod.POST,
                new HttpEntity<>("{}", headers),
                String.class
        );
        assertTrue(response.getStatusCode().is4xxClientError());
        assertTrue(response.getBody().contains(ProductionReasonCode.SYSTEM_TRANSITION_DENIED.name()));
    }

    @Test
    void reviewerCannotAuthorize() {
        var released = authorizeReadyAndRelease("CELL-RES-SOD-1", "idem-res-sod");
        UUID campaignId = released.getCampaignId();
        campaignService.transition(campaignId, "PAUSE_CAMPAIGN", actor("pauser", CampaignPermission.CAMPAIGN_PAUSE), "idem-res-sod-p");
        ensureP17Current();
        resumptionService.request(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        resumptionService.startReview(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        resumptionService.approveReview(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        CampaignException sod = assertThrows(CampaignException.class, () ->
                resumptionService.authorize(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_AUTHORIZE)));
        assertEquals(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, sod.reasonCode());
    }

    @Test
    void staleFingerprintAndRevokedOnboardingDenyAuthorize() {
        var released = authorizeReadyAndRelease("CELL-RES-CUR-1", "idem-res-cur");
        UUID campaignId = released.getCampaignId();
        campaignService.transition(campaignId, "PAUSE_CAMPAIGN", actor("pauser", CampaignPermission.CAMPAIGN_PAUSE), "idem-res-cur-p");
        ensureP17Current();
        resumptionService.request(campaignId, actor("resume-req", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        resumptionService.startReview(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        resumptionService.approveReview(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        jdbc.update("UPDATE production_change_campaign SET fingerprint = ? WHERE campaign_id = ?", "a".repeat(64), campaignId);
        CampaignException stale = assertThrows(CampaignException.class, () ->
                resumptionService.authorize(campaignId, actor("resume-auth", CampaignPermission.CAMPAIGN_RESUMPTION_AUTHORIZE)));
        assertEquals(ProductionReasonCode.CAMPAIGN_STALE, stale.reasonCode());
        jdbc.update("UPDATE production_change_campaign SET fingerprint = (SELECT fingerprint FROM campaign_revision WHERE campaign_id = ? LIMIT 1) WHERE campaign_id = ?", campaignId, campaignId);
        jdbc.update("UPDATE production_target_onboarding SET status = 'REVOKED' WHERE production_target_id = ?", TARGET_ID);
        try {
            CampaignException onboard = assertThrows(CampaignException.class, () ->
                    resumptionService.authorize(campaignId, actor("resume-auth", CampaignPermission.CAMPAIGN_RESUMPTION_AUTHORIZE)));
            assertEquals(ProductionReasonCode.CAMPAIGN_STALE, onboard.reasonCode());
        } finally {
            jdbc.update("UPDATE production_target_onboarding SET status = 'APPROVED' WHERE production_target_id = ? AND status = 'REVOKED'", TARGET_ID);
        }
    }

    @Test
    void revokedCertificationDeniesAuthorize() {
        var released = authorizeReadyAndRelease("CELL-RES-CERT-1", "idem-res-cert");
        UUID campaignId = released.getCampaignId();
        campaignService.transition(campaignId, "PAUSE_CAMPAIGN", actor("pauser", CampaignPermission.CAMPAIGN_PAUSE), "idem-res-cert-p");
        ensureP17Current();
        resumptionService.request(campaignId, actor("resume-req", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        resumptionService.startReview(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        resumptionService.approveReview(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        jdbc.update("UPDATE production_target_certification SET status = 'REVOKED' WHERE production_target_id = ? AND status = 'CURRENT'", TARGET_ID);
        try {
            CampaignException ex = assertThrows(CampaignException.class, () ->
                    resumptionService.authorize(campaignId, actor("resume-auth", CampaignPermission.CAMPAIGN_RESUMPTION_AUTHORIZE)));
            assertEquals(ProductionReasonCode.CAMPAIGN_STALE, ex.reasonCode());
        } finally {
            jdbc.update("UPDATE production_target_certification SET status = 'CURRENT' WHERE production_target_id = ? AND status = 'REVOKED'", TARGET_ID);
        }
    }

    @Test
    void observingInvalidBoundaryRequiresNewObservationBoundary() {
        var released = authorizeReadyAndRelease("CELL-RES-OBS-1", "idem-res-obs");
        UUID campaignId = released.getCampaignId();
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        UUID cohortId = jdbc.queryForObject("SELECT cohort_id FROM campaign_cohort WHERE campaign_id = ?", UUID.class, campaignId);
        Instant verified = Instant.parse("2026-01-01T00:00:00Z");
        observationService.persistBoundary(
                campaignId, revisionId, cohortId, itemId, null, "CELL-RES-OBS-1", "txPower",
                new BigDecimal("43"), verified, verified.plusSeconds(1), verified.plusSeconds(60),
                "kpi-source", verified.plusSeconds(1), "PHASE12_CHECKPOINT", "ckpt-1", 1000L, "ckpt-1",
                "p18-health-v1", "p18-observation-v1",
                com.simba.snip.npo.productioncampaign.service.CampaignObservationService.NetworkObservationHealth.STALE,
                com.simba.snip.npo.productioncampaign.service.CampaignObservationService.OperationalSafetyHealth.SAFE
        );
        jdbc.update(
                """
                INSERT INTO campaign_suspension (suspension_id, campaign_id, revision_id, suspension_type, state, reason_code, pre_state, created_at)
                VALUES (?, ?, ?, 'OPERATOR_PAUSE', 'ACTIVE', 'PAUSE', 'CANARY_OBSERVING', NOW())
                """,
                UUID.randomUUID(), campaignId, revisionId
        );
        jdbc.update("UPDATE production_change_campaign SET state = 'SUSPENDED' WHERE campaign_id = ?", campaignId);
        governResumptionToAuthorized(campaignId);
        CampaignException ex = assertThrows(CampaignException.class, () ->
                resumptionService.makeEffective(campaignId, systemResumptionActor()));
        assertEquals(ProductionReasonCode.NEW_OBSERVATION_BOUNDARY_REQUIRED, ex.reasonCode());
    }

    @Test
    void effectiveDoesNotReleaseCohortOrReopenClosureOrResurrectStaleRelease() {
        var released = authorizeReadyAndRelease("CELL-RES-SAFE-1", "idem-res-safe");
        UUID campaignId = released.getCampaignId();
        jdbc.update("UPDATE campaign_cohort_release SET state = 'STALE' WHERE campaign_id = ?", campaignId);
        campaignService.transition(campaignId, "PAUSE_CAMPAIGN", actor("pauser", CampaignPermission.CAMPAIGN_PAUSE), "idem-res-safe-p");
        int releasesBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_cohort_release WHERE campaign_id = ?", Integer.class, campaignId);
        governResumptionToAuthorized(campaignId);
        String dest = resumptionService.makeEffective(campaignId, systemResumptionActor());
        assertEquals("PAUSED_FOR_RELEASE", dest);
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_cohort_release WHERE campaign_id = ? AND state = 'ACTIVE'",
                Integer.class, campaignId));
        assertEquals(releasesBefore, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_cohort_release WHERE campaign_id = ?", Integer.class, campaignId));
        assertEquals("OPEN", jdbc.queryForObject(
                "SELECT state FROM campaign_forward_progression_closure WHERE campaign_id = ?", String.class, campaignId));
    }
}
