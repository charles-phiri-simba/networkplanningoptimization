package com.simba.snip.npo.productionchange.policy;

import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.stereotype.Component;

@Component
public class ProductionSeparationOfDutiesPolicy {

    public void requireIdentified(String principalId, String role) {
        if (principalId == null || principalId.isBlank()) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                    role + " principal is required"
            );
        }
    }

    public void requireDistinct(String leftPrincipalId, String rightPrincipalId, String rule) {
        requireIdentified(leftPrincipalId, "left");
        requireIdentified(rightPrincipalId, "right");
        if (leftPrincipalId.strip().equals(rightPrincipalId.strip())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                    rule
            );
        }
    }

    public void requesterMustNotAuthorize(String requesterPrincipalId, String authorizerPrincipalId) {
        requireDistinct(requesterPrincipalId, authorizerPrincipalId, "requester must not be production authorizer");
    }

    public void authorizerMustNotExecute(String authorizerPrincipalId, String executorPrincipalId) {
        requireDistinct(authorizerPrincipalId, executorPrincipalId, "production authorizer must not be executor");
    }

    public void changeControlValidatorMustNotBeRequester(String validatorPrincipalId, String requesterPrincipalId) {
        requireDistinct(validatorPrincipalId, requesterPrincipalId, "change-control validator must not be requester");
    }

    public void reviewerMustNotAuthorize(String reviewerPrincipalId, String authorizerPrincipalId) {
        requireDistinct(reviewerPrincipalId, authorizerPrincipalId, "reviewer must not be production authorizer");
    }
}
