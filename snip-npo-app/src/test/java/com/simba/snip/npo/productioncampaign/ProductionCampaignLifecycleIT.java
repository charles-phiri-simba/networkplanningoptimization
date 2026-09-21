package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.api.ProductionCampaignDto;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.security.HeaderAuthenticatedActorProvider;
import com.simba.snip.npo.productioncampaign.service.CampaignHandoffService;
import com.simba.snip.npo.productioncampaign.service.CampaignRecoveryService;
import com.simba.snip.npo.productioncampaign.service.ForwardProgressionClosureService;
import com.simba.snip.npo.productioncampaign.service.ProductionChangeCampaignService;
import com.simba.snip.npo.productioncampaign.service.TxPowerUnitValidator;
import com.simba.snip.npo.productioncampaign.service.VendorRejectedSemantics;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionCampaignLifecycleIT extends ProductionCampaignITSupport {

    @Autowired TestRestTemplate http;
    @Autowired CampaignHandoffService handoffService;
    @Autowired CampaignRecoveryService recoveryService;
    @Autowired ForwardProgressionClosureService closureService;
    @Autowired com.simba.snip.npo.productioncampaign.service.CampaignSafetyService safety;

    @Test
    void campaignCreateReviewAuthorizeReleaseAndAbort() {
        var created = newCampaign("CELL-LIFE-1", "idem-life-1");
        assertEquals("DRAFT", created.getState());
        campaignService.transition(created.getCampaignId(), "SUBMIT_FOR_REVIEW", actor("creator-1", CampaignPermission.CAMPAIGN_CREATE), "idem-life-1-s");
        campaignService.transition(created.getCampaignId(), "REVIEW_APPROVE", actor("reviewer-1", CampaignPermission.CAMPAIGN_REVIEW), "idem-life-1-r");
        campaignService.transition(created.getCampaignId(), "AUTHORIZE_CAMPAIGN", actor("authorizer-1", CampaignPermission.CAMPAIGN_AUTHORIZE), "idem-life-1-a");
        campaignService.evaluateReady(created.getCampaignId());
        var released = campaignService.transition(created.getCampaignId(), "RELEASE_CANARY", actor("releaser-1", CampaignPermission.RELEASE_COHORT), "idem-life-1-rel");
        assertEquals("CANARY_ACTIVE", released.getState());
        var aborted = campaignService.transition(created.getCampaignId(), "ABORT_CAMPAIGN", actor("abort-1", CampaignPermission.CAMPAIGN_ABORT), "idem-life-1-ab");
        assertEquals("ABORTED", aborted.getState());
        assertEquals("ABORTED", aborted.getAbortState());
    }

    @Test
    void authoritySeparationDoesNotMintGrant() {
        var created = newCampaign("CELL-LIFE-2", "idem-life-2");
        campaignService.transition(created.getCampaignId(), "SUBMIT_FOR_REVIEW", actor("creator-1", CampaignPermission.CAMPAIGN_CREATE), "idem-life-2-s");
        campaignService.transition(created.getCampaignId(), "REVIEW_APPROVE", actor("reviewer-1", CampaignPermission.CAMPAIGN_REVIEW), "idem-life-2-r");
        var authorized = campaignService.transition(created.getCampaignId(), "AUTHORIZE_CAMPAIGN", actor("authorizer-1", CampaignPermission.CAMPAIGN_AUTHORIZE), "idem-life-2-a");
        assertEquals("AUTHORIZED", authorized.getState());
        Integer grants = jdbc.queryForObject("SELECT COUNT(*) FROM production_execution_grant", Integer.class);
        assertEquals(0, grants == null ? 0 : grants);
        Integer changes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_network_change WHERE execution_origin = 'PRODUCTION_CAMPAIGN'",
                Integer.class
        );
        assertEquals(0, changes == null ? 0 : changes);
    }

    @Test
    void untrustedHeadersCannotCreateCampaign() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.add(HeaderAuthenticatedActorProvider.PERMISSION_HEADER, "CAMPAIGN_CREATE");
        headers.add(HeaderAuthenticatedActorProvider.ACTOR_HEADER, "human-1");
        headers.add(HeaderAuthenticatedActorProvider.SOURCE_HEADER, "UNTRUSTED");
        headers.add(HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, "HUMAN");
        headers.add("Idempotency-Key", "idem-untrusted");
        Map<String, Object> item = new java.util.LinkedHashMap<>();
        item.put("phase14PlanId", UUID.randomUUID().toString());
        item.put("phase14PlanFingerprint", "a".repeat(64));
        item.put("phase15ExecutionId", UUID.randomUUID().toString());
        item.put("phase15ExecutionFingerprint", "b".repeat(64));
        item.put("cellId", "CELL-UNTRUSTED");
        item.put("expectedValue", 46);
        item.put("desiredValue", 43);
        item.put("rollbackValue", 46);
        item.put("unit", "dBm");
        item.put("objectType", "CELL");
        item.put("parameter", "txPower");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("productionTargetId", TARGET_ID);
        body.put("objective", "obj");
        body.put("changeControlReference", "CC-1");
        body.put("items", List.of(item));
        ResponseEntity<String> response = http.exchange(
                "/api/v1/production-campaigns",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
        assertTrue(response.getStatusCode().is4xxClientError());
        assertTrue(response.getBody().contains(ProductionReasonCode.UNTRUSTED_ACTOR_SOURCE.name()));
    }

    @Test
    void handoffCreateOrReturnAndConflict() {
        var campaign = newCampaign("CELL-HAND-1", "idem-hand-1");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        UUID cohortId = jdbc.queryForObject("SELECT cohort_id FROM campaign_cohort WHERE campaign_id = ?", UUID.class, campaignId);
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        String itemFp = jdbc.queryForObject("SELECT item_fingerprint FROM campaign_execution_item WHERE item_id = ?", String.class, itemId);
        String first = handoffService.createOrReturnForwardHandoff(
                campaignId, revisionId, 1, itemId, cohortId, 1L, 1L, 1L,
                campaign.getFingerprint(), "e".repeat(64), itemFp, TARGET_ID,
                UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(), "b".repeat(64),
                "CELL-HAND-1", new BigDecimal("46"), new BigDecimal("43"), new BigDecimal("46")
        );
        String second = handoffService.createOrReturnForwardHandoff(
                campaignId, revisionId, 1, itemId, cohortId, 1L, 1L, 1L,
                campaign.getFingerprint(), "e".repeat(64), itemFp, TARGET_ID,
                UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(), "b".repeat(64),
                "CELL-HAND-1", new BigDecimal("46"), new BigDecimal("43"), new BigDecimal("46")
        );
        assertEquals(first, second);
        CampaignException ex = assertThrows(CampaignException.class, () ->
                handoffService.createOrReturnForwardHandoff(
                        campaignId, revisionId, 1, itemId, cohortId, 1L, 99L, 1L,
                        campaign.getFingerprint(), "f".repeat(64), itemFp, TARGET_ID,
                        UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(), "b".repeat(64),
                        "CELL-HAND-1", new BigDecimal("46"), new BigDecimal("40"), new BigDecimal("46")
                ));
        assertEquals(ProductionReasonCode.HANDOFF_IDEMPOTENCY_CONFLICT, ex.reasonCode());
    }

    @Test
    void standaloneDowngradeDenied() {
        CampaignException ex = assertThrows(CampaignException.class, () ->
                handoffService.denyStandaloneDowngrade("PRODUCTION_CAMPAIGN"));
        assertEquals(ProductionReasonCode.STANDALONE_DOWNGRADE_DENIED, ex.reasonCode());
        handoffService.denyStandaloneDowngrade("STANDALONE");
    }

    @Test
    void vendorRejectedAfterMayHaveSentPreservesHistory() {
        VendorRejectedSemantics semantics = new VendorRejectedSemantics();
        CampaignException ex = assertThrows(CampaignException.class, () -> semantics.apply(true, true));
        assertEquals(ProductionReasonCode.PRODUCTION_OUTCOME_UNRESOLVED, ex.reasonCode());
        semantics.apply(false, true);
    }

    @Test
    void atomicSafetyExposureConsumeOnce() {
        var campaign = newCampaign("CELL-SAFE-1", "idem-safe-1");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        UUID reservation = jdbc.queryForObject(
                """
                INSERT INTO campaign_safety_budget_reservation
                    (reservation_id, campaign_id, revision_id, budget_type, state, created_at, updated_at, version)
                VALUES (?, ?, ?, 'FORWARD', 'RESERVED', NOW(), NOW(), 0)
                RETURNING reservation_id
                """,
                UUID.class,
                UUID.randomUUID(), campaignId, revisionId
        );
        safety.consumeMayHaveSent(campaignId, 1, reservation, "FORWARD", "CELL-SAFE-1", true);
        CampaignException ex = assertThrows(CampaignException.class, () ->
                safety.consumeMayHaveSent(campaignId, 1, reservation, "FORWARD", "CELL-SAFE-1", true));
        assertEquals(ProductionReasonCode.SAFETY_EXPOSURE_ALREADY_ACCOUNTED, ex.reasonCode());
        assertEquals(1, safety.distinctCellsExposed(campaignId, 1));
    }

    @Test
    void closureBeforeRecoveryAuthorization() {
        var campaign = newCampaign("CELL-CLOSE-1", "idem-close-1");
        UUID campaignId = campaign.getCampaignId();
        CampaignException open = assertThrows(CampaignException.class, () ->
                recoveryService.authorize(campaignId, actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE)));
        assertEquals(ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE, open.reasonCode());
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        jdbc.update("UPDATE campaign_execution_item SET state = 'MAY_HAVE_SENT' WHERE item_id = ?", itemId);
        closureService.determineRecoveryRequired(campaignId, itemId);
        String closure = jdbc.queryForObject(
                "SELECT state FROM campaign_forward_progression_closure WHERE campaign_id = ?",
                String.class,
                campaignId
        );
        assertEquals("CLOSED", closure);
        assertThrows(Exception.class, () ->
                jdbc.update("UPDATE campaign_forward_progression_closure SET state = 'OPEN' WHERE campaign_id = ?", campaignId));
        Integer stillClosed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_forward_progression_closure WHERE campaign_id = ? AND state = 'CLOSED'",
                Integer.class,
                campaignId
        );
        assertEquals(1, stillClosed);
        recoveryService.request(campaignId, actor("req-r", CampaignPermission.CAMPAIGN_RECOVERY_REQUEST));
        recoveryService.startReview(campaignId, actor("rev-r", CampaignPermission.CAMPAIGN_RECOVERY_REVIEW));
        recoveryService.authorize(campaignId, actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE));
        assertTrue(recoveryService.isAuthorized(campaignId));
        CampaignException stillClosedForward = assertThrows(CampaignException.class, () -> closureService.assertOpen(campaignId));
        assertEquals(ProductionReasonCode.FORWARD_PROGRESSION_CLOSED, stillClosedForward.reasonCode());
    }

    @Test
    void unsupportedUnitRejectedAtBoundary() {
        CampaignException ex = assertThrows(CampaignException.class, () -> campaignService.create(
                TARGET_ID,
                "bad unit",
                "CC-UNIT",
                List.of(new ProductionChangeCampaignService.PlannedItem(
                        UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(), "b".repeat(64),
                        "CELL-UNIT", new BigDecimal("46"), new BigDecimal("43"), new BigDecimal("46"),
                        "W", "CELL", "txPower"
                )),
                actor("creator-1", CampaignPermission.CAMPAIGN_CREATE),
                "idem-unit"
        ));
        assertEquals(ProductionReasonCode.UNSUPPORTED_PARAMETER_UNIT, ex.reasonCode());
        TxPowerUnitValidator.requireCanonicalDbm("dBm");
    }

    @Test
    void campaignHttpCreateUsesTrustedIngress() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.add(HeaderAuthenticatedActorProvider.PERMISSION_HEADER, "CAMPAIGN_CREATE");
        headers.add(HeaderAuthenticatedActorProvider.ACTOR_HEADER, "human-create");
        headers.add(HeaderAuthenticatedActorProvider.SOURCE_HEADER, SOURCE);
        headers.add(HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, "HUMAN");
        headers.add("Idempotency-Key", "idem-http-create");
        Map<String, Object> item = new java.util.LinkedHashMap<>();
        item.put("phase14PlanId", UUID.randomUUID().toString());
        item.put("phase14PlanFingerprint", "a".repeat(64));
        item.put("phase15ExecutionId", UUID.randomUUID().toString());
        item.put("phase15ExecutionFingerprint", "b".repeat(64));
        item.put("cellId", "CELL-HTTP-1");
        item.put("expectedValue", 46);
        item.put("desiredValue", 43);
        item.put("rollbackValue", 46);
        item.put("unit", "dBm");
        item.put("objectType", "CELL");
        item.put("parameter", "txPower");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("productionTargetId", TARGET_ID);
        body.put("objective", "http create");
        body.put("changeControlReference", "CC-HTTP");
        body.put("items", List.of(item));
        ResponseEntity<ProductionCampaignDto> response = http.exchange(
                "/api/v1/production-campaigns",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                ProductionCampaignDto.class
        );
        assertEquals(200, response.getStatusCode().value());
        assertEquals("DRAFT", response.getBody().state());
        assertNotEquals(null, response.getBody().campaignId());
    }
}
