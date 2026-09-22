package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.productioncampaign.domain.ActorType;
import com.simba.snip.npo.productioncampaign.domain.AuthenticatedActor;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.entity.ProductionChangeCampaignEntity;
import com.simba.snip.npo.productioncampaign.service.CampaignResumptionService;
import com.simba.snip.npo.productioncampaign.service.ProductionChangeCampaignService;
import com.simba.snip.npo.productionchange.service.ProductionTargetRegistry;
import com.simba.snip.npo.vendorcertification.Phase17CertificationGraphSeeder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ProductionCampaignITSupport extends AbstractPostgresIT {

    public static final String TARGET_ID = ProductionTargetRegistry.DEFAULT_L0_TARGET_ID;
    public static final String SOURCE = "TEST_INGRESS";

    @DynamicPropertySource
    static void campaignProps(DynamicPropertyRegistry registry) {
        registry.add("snip.production-campaign.trusted-ingress-guarantee", () -> "true");
        registry.add("snip.production-campaign.trusted-authentication-sources[0]", () -> SOURCE);
        registry.add("snip.production-campaign.configured-policy-maximum", () -> "25");
        registry.add("snip.production-change.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected ProductionTargetRegistry targetRegistry;
    @Autowired protected ProductionChangeCampaignService campaignService;
    @Autowired protected CampaignResumptionService resumptionService;

    @BeforeEach
    void seedTarget() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_ID));
        restoreFixtureOwnedP17State();
    }

    @AfterEach
    void cleanupCampaigns() {
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
        jdbc.update("""
                DELETE FROM production_execution_rollback
                 WHERE production_change_id IN (
                    SELECT production_change_id FROM production_network_change WHERE execution_origin = 'PRODUCTION_CAMPAIGN'
                 )
                """);
        jdbc.update("""
                DELETE FROM production_execution_grant
                 WHERE production_change_id IN (
                    SELECT production_change_id FROM production_network_change WHERE execution_origin = 'PRODUCTION_CAMPAIGN'
                 )
                """);
        jdbc.update("""
                DELETE FROM production_change_authorization
                 WHERE production_change_id IN (
                    SELECT production_change_id FROM production_network_change WHERE execution_origin = 'PRODUCTION_CAMPAIGN'
                 )
                """);
        jdbc.update("""
                DELETE FROM production_change_control
                 WHERE production_change_id IN (
                    SELECT production_change_id FROM production_network_change WHERE execution_origin = 'PRODUCTION_CAMPAIGN'
                 )
                """);
        jdbc.update("""
                DELETE FROM production_change_audit_event
                 WHERE production_change_id IN (
                    SELECT production_change_id FROM production_network_change WHERE execution_origin = 'PRODUCTION_CAMPAIGN'
                 )
                """);
        jdbc.update("DELETE FROM production_network_change WHERE execution_origin = 'PRODUCTION_CAMPAIGN'");
        restoreFixtureOwnedP17State();
    }

    protected AuthenticatedActor actor(String id, CampaignPermission permission) {
        return new AuthenticatedActor(id, ActorType.HUMAN, EnumSet.of(permission), SOURCE, true);
    }

    protected AuthenticatedActor systemResumptionActor() {
        return new AuthenticatedActor("SYSTEM:RESUMPTION", ActorType.SYSTEM, EnumSet.noneOf(CampaignPermission.class), SOURCE, true);
    }

    /**
     * Restores fixture-owned P17 onboarding/certification for {@link #TARGET_ID} only.
     * Conflicting leftover rows from other test classes are rewritten to the
     * deterministic current state this fixture depends on.
     */
    protected void restoreFixtureOwnedP17State() {
        jdbc.update("""
                UPDATE production_target_onboarding
                   SET status = 'APPROVED', updated_at = NOW()
                 WHERE production_target_id = ?
                   AND status IS DISTINCT FROM 'APPROVED'
                """, TARGET_ID);
        Integer onboarded = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_target_onboarding WHERE production_target_id = ? AND status = 'APPROVED'",
                Integer.class,
                TARGET_ID
        );
        if (onboarded == null || onboarded < 1) {
            jdbc.update("""
                    INSERT INTO production_target_onboarding (
                        onboarding_id, production_target_id, status, certification_level,
                        created_by, reviewed_by, approved_by, created_at, updated_at)
                    VALUES (?, ?, 'APPROVED', 'L0', 'onb-create', 'onb-review', 'onb-approve', NOW(), NOW())
                    """, UUID.randomUUID(), TARGET_ID);
        }
        jdbc.update("""
                UPDATE production_target_certification
                   SET status = 'CURRENT'
                 WHERE production_target_id = ?
                   AND status IN ('REVOKED', 'EXPIRED', 'INVALID', 'SUSPENDED')
                """, TARGET_ID);
        Integer current = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_target_certification WHERE production_target_id = ? AND status = 'CURRENT'",
                Integer.class,
                TARGET_ID
        );
        if (current == null || current < 1) {
            Phase17CertificationGraphSeeder.seed(jdbc, TARGET_ID, "p18-" + UUID.randomUUID());
        }
    }

    protected void ensureP17Current() {
        restoreFixtureOwnedP17State();
    }

    protected void governResumptionToAuthorized(UUID campaignId) {
        ensureP17Current();
        resumptionService.request(campaignId, actor("resume-req", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        resumptionService.startReview(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        resumptionService.approveReview(campaignId, actor("resume-rev", CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
        resumptionService.authorize(campaignId, actor("resume-auth", CampaignPermission.CAMPAIGN_RESUMPTION_AUTHORIZE));
    }

    protected ProductionChangeCampaignEntity newCampaign(String cellId, String idempotencyKey) {
        return campaignService.create(
                TARGET_ID,
                "progressive txPower campaign",
                "CC-P18-" + UUID.randomUUID(),
                List.of(item(cellId)),
                actor("creator-1", CampaignPermission.CAMPAIGN_CREATE),
                idempotencyKey
        );
    }

    protected ProductionChangeCampaignEntity authorizeReadyAndRelease(String cellId, String prefix) {
        var created = newCampaign(cellId, prefix + "-c");
        campaignService.transition(created.getCampaignId(), "SUBMIT_FOR_REVIEW", actor("creator-1", CampaignPermission.CAMPAIGN_CREATE), prefix + "-s");
        campaignService.transition(created.getCampaignId(), "REVIEW_APPROVE", actor("reviewer-1", CampaignPermission.CAMPAIGN_REVIEW), prefix + "-r");
        campaignService.transition(created.getCampaignId(), "AUTHORIZE_CAMPAIGN", actor("authorizer-1", CampaignPermission.CAMPAIGN_AUTHORIZE), prefix + "-a");
        campaignService.evaluateReady(created.getCampaignId());
        return campaignService.transition(created.getCampaignId(), "RELEASE_CANARY", actor("releaser-1", CampaignPermission.RELEASE_COHORT), prefix + "-rel");
    }

    protected ProductionChangeCampaignService.PlannedItem item(String cellId) {
        return new ProductionChangeCampaignService.PlannedItem(
                UUID.randomUUID(),
                "a".repeat(64),
                UUID.randomUUID(),
                "b".repeat(64),
                cellId,
                new BigDecimal("46"),
                new BigDecimal("43"),
                new BigDecimal("46"),
                "dBm",
                "CELL",
                "txPower"
        );
    }
}
