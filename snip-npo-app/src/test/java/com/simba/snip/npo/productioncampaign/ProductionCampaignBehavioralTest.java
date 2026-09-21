package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.config.ProductionCampaignProperties;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.security.HeaderAuthenticatedActorProvider;
import com.simba.snip.npo.productioncampaign.service.CampaignItemLifecycleService;
import com.simba.snip.npo.productioncampaign.service.CampaignObservationService;
import com.simba.snip.npo.productioncampaign.service.CanonicalReconciliationReader;
import com.simba.snip.npo.productioncampaign.service.TxPowerUnitValidator;
import com.simba.snip.npo.productioncampaign.service.VendorRejectedSemantics;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionCampaignBehavioralTest {

    @ParameterizedTest
    @ValueSource(ints = {26, 100, Integer.MAX_VALUE, 0, -1})
    void hardMaxCampaignItemsCannotBeRaised(int configured) {
        ProductionCampaignProperties properties = new ProductionCampaignProperties();
        properties.setConfiguredPolicyMaximum(configured);
        CampaignException ex = assertThrows(CampaignException.class, properties::validate);
        assertEquals(ProductionReasonCode.CAMPAIGN_SIZE_LIMIT, ex.reasonCode());
    }

    @Test
    void malformedConfiguredMaximumFailsClosed() {
        ProductionCampaignProperties properties = new ProductionCampaignProperties();
        properties.setConfiguredPolicyMaximum(0);
        assertThrows(CampaignException.class, properties::effectiveMaximumItems);
    }

    @Test
    void effectiveMaximumIsMinOfHardAndPolicy() {
        ProductionCampaignProperties properties = new ProductionCampaignProperties();
        properties.setConfiguredPolicyMaximum(10);
        assertEquals(10, properties.effectiveMaximumItems());
        properties.setConfiguredPolicyMaximum(25);
        assertEquals(25, properties.effectiveMaximumItems());
    }

    @Test
    void automaticProgressionFailsClosed() {
        ProductionCampaignProperties properties = new ProductionCampaignProperties();
        properties.setAutomaticProgressionEnabled(true);
        CampaignException ex = assertThrows(CampaignException.class, properties::validate);
        assertEquals(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, ex.reasonCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"W", "mW", "dbm", "dB", "", "DBM"})
    void unsupportedTxPowerUnitRejected(String unit) {
        String candidate = unit.isEmpty() ? null : unit;
        CampaignException ex = assertThrows(CampaignException.class, () -> TxPowerUnitValidator.requireCanonicalDbm(candidate));
        assertEquals(ProductionReasonCode.UNSUPPORTED_PARAMETER_UNIT, ex.reasonCode());
    }

    @Test
    void untrustedActorHeadersDenied() {
        ProductionCampaignProperties properties = new ProductionCampaignProperties();
        properties.setTrustedIngressGuarantee(false);
        HeaderAuthenticatedActorProvider provider = new HeaderAuthenticatedActorProvider(properties);
        provider.bindRequest("CAMPAIGN_CREATE", "human-1", "TEST_INGRESS", "HUMAN");
        CampaignException ex = assertThrows(
                CampaignException.class,
                () -> provider.requireHuman(CampaignPermission.CAMPAIGN_CREATE)
        );
        assertEquals(ProductionReasonCode.UNTRUSTED_ACTOR_SOURCE, ex.reasonCode());
    }

    @Test
    void unspecifiedItemTransitionsFailClosed() {
        assertTrue(CampaignItemLifecycleService.destinationFor("COMPLETED", "COMPLETE_ITEM") == null);
        assertTrue(CampaignItemLifecycleService.destinationFor("HANDED_OFF", "COMPLETE_ITEM") == null);
        assertTrue(CampaignItemLifecycleService.destinationFor("MAY_HAVE_SENT", "PRE_SEND_READY") == null);
        assertTrue(CampaignItemLifecycleService.destinationFor("VENDOR_ACCEPTED", "COMPLETE_ITEM") == null);
        assertTrue(CampaignItemLifecycleService.destinationFor("OBSERVING", "COMPLETE_ITEM") == null);
        assertTrue(CampaignItemLifecycleService.destinationFor("PLANNED", "GATEWAY_SEND_BOUNDARY") == null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AGENT", "MCP", "SERVICE", "SCHEDULER", "EVENT", "UNKNOWN", "", " "})
    void nonHumanIdentitiesCannotSatisfyHumanAuthority(String actorType) {
        ProductionCampaignProperties properties = trusted();
        HeaderAuthenticatedActorProvider provider = new HeaderAuthenticatedActorProvider(properties);
        provider.bindRequest("CAMPAIGN_AUTHORIZE", "non-human-1", "TEST_INGRESS", actorType);
        CampaignException ex = assertThrows(
                CampaignException.class,
                () -> provider.requireHuman(CampaignPermission.CAMPAIGN_AUTHORIZE)
        );
        assertTrue(ex.reasonCode() == ProductionReasonCode.UNAUTHENTICATED_HUMAN_ACTOR
                || ex.reasonCode() == ProductionReasonCode.UNTRUSTED_ACTOR_SOURCE);
    }

    @Test
    void historicalHealthySafeIsNotFutureAdmissibility() {
        CampaignObservationService service = new CampaignObservationService();
        CampaignException ex = assertThrows(CampaignException.class, () -> service.assertProgressionAdmissible(
                new CampaignObservationService.ObservationEvidence(
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:01Z"),
                        Instant.parse("2026-01-01T00:00:01Z"),
                        "kpi-source",
                        "WATERMARK",
                        "w1",
                        1000L,
                        "ckpt-1",
                        CampaignObservationService.NetworkObservationHealth.HEALTHY,
                        CampaignObservationService.OperationalSafetyHealth.SAFE,
                        true
                )
        ));
        assertEquals(ProductionReasonCode.OBSERVATION_NOT_HEALTHY, ex.reasonCode());
    }

    @Test
    void ingestionTimeAloneIsInsufficientProvenance() {
        CampaignObservationService service = new CampaignObservationService();
        CampaignException ex = assertThrows(CampaignException.class, () -> service.assertProgressionAdmissible(
                new CampaignObservationService.ObservationEvidence(
                        Instant.now(), Instant.now(), null, "src", null, null, null, null,
                        CampaignObservationService.NetworkObservationHealth.HEALTHY,
                        CampaignObservationService.OperationalSafetyHealth.SAFE,
                        false
                )
        ));
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, ex.reasonCode());
    }

    @Test
    void unknownOrStaleObservationDeniesProgression() {
        CampaignObservationService service = new CampaignObservationService();
        CampaignException ex = assertThrows(CampaignException.class, () -> service.assertProgressionAdmissible(
                new CampaignObservationService.ObservationEvidence(
                        Instant.now(), Instant.now(), Instant.now(), "src", "WM", "v", 1L, "ckpt",
                        CampaignObservationService.NetworkObservationHealth.STALE,
                        CampaignObservationService.OperationalSafetyHealth.SAFE,
                        false
                )
        ));
        assertEquals(ProductionReasonCode.OBSERVATION_NOT_HEALTHY, ex.reasonCode());
    }

    @Test
    void vendorRejectedAfterMayHaveSentDoesNotManufactureNotSent() {
        VendorRejectedSemantics semantics = new VendorRejectedSemantics();
        CampaignException ex = assertThrows(CampaignException.class, () -> semantics.apply(true, false));
        assertEquals(ProductionReasonCode.PRODUCTION_OUTCOME_UNRESOLVED, ex.reasonCode());
    }

    @Test
    void proofFormARequiredForCanonicalReconciled() {
        CanonicalReconciliationReader reader = new CanonicalReconciliationReader(null);
        CampaignException ex = assertThrows(CampaignException.class, () -> reader.assertCanonicalReconciled(
                "CELL-001",
                "txPower",
                java.math.BigDecimal.ONE,
                Instant.now(),
                UUID.randomUUID(),
                "ckpt"
        ));
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, ex.reasonCode());
    }

    private static ProductionCampaignProperties trusted() {
        ProductionCampaignProperties properties = new ProductionCampaignProperties();
        properties.setTrustedIngressGuarantee(true);
        properties.setTrustedAuthenticationSources(List.of("TEST_INGRESS"));
        return properties;
    }
}
