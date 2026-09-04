package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.AttemptSendClass;
import com.simba.snip.npo.productionchange.protocol.MutationOutcome;

public final class AttemptSendClassifier {

    private AttemptSendClassifier() {
    }

    public static AttemptSendClass classify(boolean dispatched, boolean certifiedPositiveNotSentProof) {
        if (!dispatched && certifiedPositiveNotSentProof) {
            return AttemptSendClass.POSITIVE_NOT_SENT;
        }
        return AttemptSendClass.MAY_HAVE_SENT;
    }

    public static AttemptSendClass afterDispatch(MutationOutcome outcome, boolean timeout, boolean connectionLoss, boolean responseLoss) {
        if (timeout || connectionLoss || responseLoss) {
            return AttemptSendClass.MAY_HAVE_SENT;
        }
        if (outcome == MutationOutcome.NOT_SENT) {
            return AttemptSendClass.POSITIVE_NOT_SENT;
        }
        return AttemptSendClass.MAY_HAVE_SENT;
    }
}
