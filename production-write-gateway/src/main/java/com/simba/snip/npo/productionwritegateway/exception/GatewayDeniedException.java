package com.simba.snip.npo.productionwritegateway.exception;

import com.simba.snip.npo.productionchange.protocol.GatewayAttemptStatus;
import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;

import java.util.UUID;

public class GatewayDeniedException extends RuntimeException {

    private final ProductionReasonCode reasonCode;
    private final MutationOutcome mutationOutcome;
    private final GatewayAttemptStatus attemptStatus;
    private final UUID attemptId;
    private final UUID grantId;
    private final UUID productionChangeId;
    private final String productionChangeStatus;

    public GatewayDeniedException(
            ProductionReasonCode reasonCode,
            MutationOutcome mutationOutcome,
            GatewayAttemptStatus attemptStatus,
            UUID attemptId,
            UUID grantId,
            UUID productionChangeId,
            String productionChangeStatus
    ) {
        super(reasonCode.name());
        this.reasonCode = reasonCode;
        this.mutationOutcome = mutationOutcome;
        this.attemptStatus = attemptStatus;
        this.attemptId = attemptId;
        this.grantId = grantId;
        this.productionChangeId = productionChangeId;
        this.productionChangeStatus = productionChangeStatus;
    }

    public static GatewayDeniedException deny(
            ProductionReasonCode reasonCode,
            UUID grantId,
            UUID productionChangeId
    ) {
        return new GatewayDeniedException(
                reasonCode,
                MutationOutcome.NOT_SENT,
                null,
                null,
                grantId,
                productionChangeId,
                null
        );
    }

    public ProductionReasonCode reasonCode() {
        return reasonCode;
    }

    public MutationOutcome mutationOutcome() {
        return mutationOutcome;
    }

    public GatewayAttemptStatus attemptStatus() {
        return attemptStatus;
    }

    public UUID attemptId() {
        return attemptId;
    }

    public UUID grantId() {
        return grantId;
    }

    public UUID productionChangeId() {
        return productionChangeId;
    }

    public String productionChangeStatus() {
        return productionChangeStatus;
    }
}
