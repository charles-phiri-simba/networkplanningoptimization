package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.domain.ExpectedStateGuardStrength;
import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.targetonboarding.service.ProductionTargetOnboardingService;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import com.simba.snip.npo.vendorcertification.service.VendorCapabilityCertificationService;
import com.simba.snip.npo.vendorcertification.service.VendorVersionCompatibilityService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17BehavioralCatalogTest {

    @Test
    void expectedStateEnumIsGuardStrength() {
        assertEquals("READ_THEN_WRITE", ExpectedStateGuardStrength.READ_THEN_WRITE.name());
        assertEquals("ATOMIC", ExpectedStateGuardStrength.ATOMIC.name());
    }

    @Test
    void capabilityOnlyCellTxPower() {
        VendorCapabilityCertificationService service =
                new VendorCapabilityCertificationService(new Phase17SeparationOfDutiesPolicy());
        service.requireCellTxPower("CELL", "txPower", "cap-1");
        assertEquals(Phase17DenialCode.P17_CAPABILITY_NOT_CERTIFIED,
                assertThrows(Phase17Exception.class, () -> service.requireCellTxPower("CELL", "retT", "cap-1")).denialCode());
    }

    @Test
    void vendorVersionExplicitPredicate() {
        VendorVersionCompatibilityService service = new VendorVersionCompatibilityService();
        service.requireExplicitPredicate("ENM-22", "EXPLICIT:ENM-22");
        assertThrows(Phase17Exception.class, () -> service.requireExplicitPredicate(null, "EXPLICIT:ENM-22"));
        assertThrows(Phase17Exception.class, () -> service.requireExplicitPredicate("ENM-23", "1.2.3-semver"));
        assertThrows(Phase17Exception.class, () -> service.requireExplicitPredicate("ENM-99", "EXPLICIT:ENM-22"));
    }

    @Test
    void onboardingSoDAndNoStandingL4() {
        ProductionTargetOnboardingService service =
                new ProductionTargetOnboardingService(new Phase17SeparationOfDutiesPolicy());
        service.requireCreateReviewApproveDistinct("c", "r", "a", "e");
        assertThrows(Phase17Exception.class, () -> service.requireCreateReviewApproveDistinct("c", "c", "a", "e"));
        assertThrows(Phase17Exception.class, () -> service.requireCreateReviewApproveDistinct("c", "r", "a", "c"));
        assertEquals(Phase17DenialCode.P17_LEVEL3_NOT_LEVEL4,
                assertThrows(Phase17Exception.class, () -> service.denyStandingL4("L4")).denialCode());
        service.denyStandingL4("L3");
    }

    @Test
    void nullBlankPrincipalsDeniedInPolicy() {
        Phase17SeparationOfDutiesPolicy sod = new Phase17SeparationOfDutiesPolicy();
        assertThrows(Phase17Exception.class, () -> sod.requirePrincipal(null, "x"));
        assertThrows(Phase17Exception.class, () -> sod.requirePrincipal("", "x"));
        assertThrows(Phase17Exception.class, () -> sod.requirePrincipal(" ", "x"));
        assertTrue(Phase17DenialCode.values().length >= 20);
    }
}
