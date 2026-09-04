package com.simba.snip.npo.productionwritegateway.exception;

import com.simba.snip.npo.productionchange.protocol.GatewayAttemptStatus;
import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
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
    private final Phase17DenialCode phase17DenialCode;

    public GatewayDeniedException(
            ProductionReasonCode reasonCode,
            MutationOutcome mutationOutcome,
            GatewayAttemptStatus attemptStatus,
            UUID attemptId,
            UUID grantId,
            UUID productionChangeId,
            String productionChangeStatus
    ) {
        this(reasonCode, mutationOutcome, attemptStatus, attemptId, grantId, productionChangeId,
                productionChangeStatus, null);
    }

    public GatewayDeniedException(
            ProductionReasonCode reasonCode,
            MutationOutcome mutationOutcome,
            GatewayAttemptStatus attemptStatus,
            UUID attemptId,
            UUID grantId,
            UUID productionChangeId,
            String productionChangeStatus,
            Phase17DenialCode phase17DenialCode
    ) {
        super(phase17DenialCode == null ? reasonCode.name() : phase17DenialCode.name());
        this.reasonCode = reasonCode;
        this.mutationOutcome = mutationOutcome;
        this.attemptStatus = attemptStatus;
        this.attemptId = attemptId;
        this.grantId = grantId;
        this.productionChangeId = productionChangeId;
        this.productionChangeStatus = productionChangeStatus;
        this.phase17DenialCode = phase17DenialCode;
    }

    public static GatewayDeniedException denyPhase17(
            Phase17DenialCode denialCode,
            UUID grantId,
            UUID productionChangeId
    ) {
        return new GatewayDeniedException(
                ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED,
                MutationOutcome.NOT_SENT,
                GatewayAttemptStatus.PRE_SEND,
                null,
                grantId,
                productionChangeId,
                ProductionChangeStatus.PREFLIGHT_DENIED.name(),
                denialCode
        );
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

    public Phase17DenialCode phase17DenialCode() {
        return phase17DenialCode;
    }
}
