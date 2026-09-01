package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.policy.ProductionSeparationOfDutiesPolicy;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionSeparationOfDutiesPolicyTest {

    private final ProductionSeparationOfDutiesPolicy policy = new ProductionSeparationOfDutiesPolicy();

    @Test
    void nullRequesterDenied() {
        ProductionChangeException ex = assertThrows(
                ProductionChangeException.class,
                () -> policy.requesterMustNotAuthorize(null, "authorizer-1"));
        assertEquals(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, ex.reasonCode());
    }

    @Test
    void nullReviewerDenied() {
        ProductionChangeException ex = assertThrows(
                ProductionChangeException.class,
                () -> policy.reviewerMustNotAuthorize(null, "authorizer-1"));
        assertEquals(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, ex.reasonCode());
    }

    @Test
    void nullAuthorizerDenied() {
        ProductionChangeException ex = assertThrows(
                ProductionChangeException.class,
                () -> policy.requesterMustNotAuthorize("requester-1", null));
        assertEquals(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, ex.reasonCode());
    }

    @Test
    void nullExecutorDenied() {
        ProductionChangeException ex = assertThrows(
                ProductionChangeException.class,
                () -> policy.authorizerMustNotExecute("authorizer-1", null));
        assertEquals(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, ex.reasonCode());
    }

    @Test
    void blankIdDenied() {
        ProductionChangeException blankRequester = assertThrows(
                ProductionChangeException.class,
                () -> policy.requesterMustNotAuthorize("  ", "authorizer-1"));
        assertEquals(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, blankRequester.reasonCode());
        ProductionChangeException blankExecutor = assertThrows(
                ProductionChangeException.class,
                () -> policy.authorizerMustNotExecute("authorizer-1", ""));
        assertEquals(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, blankExecutor.reasonCode());
        ProductionChangeException blankReviewer = assertThrows(
                ProductionChangeException.class,
                () -> policy.reviewerMustNotAuthorize("\t", "authorizer-1"));
        assertEquals(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, blankReviewer.reasonCode());
    }

    @Test
    void identifiedDistinctPrincipalsPass() {
        policy.requesterMustNotAuthorize("requester-1", "authorizer-1");
        policy.reviewerMustNotAuthorize("reviewer-1", "authorizer-1");
        policy.authorizerMustNotExecute("authorizer-1", "executor-1");
    }
}
