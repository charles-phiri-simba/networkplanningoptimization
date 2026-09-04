package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.AttemptSendClass;
import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttemptSendClassifierTest {

    @Test
    void positiveNotSentRequiresProof() {
        assertEquals(AttemptSendClass.POSITIVE_NOT_SENT, AttemptSendClassifier.classify(false, true));
        assertEquals(AttemptSendClass.MAY_HAVE_SENT, AttemptSendClassifier.classify(false, false));
        assertEquals(AttemptSendClass.MAY_HAVE_SENT, AttemptSendClassifier.classify(true, true));
    }

    @Test
    void timeoutConnectionLossResponseLossAreMayHaveSent() {
        assertEquals(AttemptSendClass.MAY_HAVE_SENT,
                AttemptSendClassifier.afterDispatch(MutationOutcome.OUTCOME_UNKNOWN, true, false, false));
        assertEquals(AttemptSendClass.MAY_HAVE_SENT,
                AttemptSendClassifier.afterDispatch(MutationOutcome.OUTCOME_UNKNOWN, false, true, false));
        assertEquals(AttemptSendClass.MAY_HAVE_SENT,
                AttemptSendClassifier.afterDispatch(MutationOutcome.OUTCOME_UNKNOWN, false, false, true));
        assertEquals(AttemptSendClass.POSITIVE_NOT_SENT,
                AttemptSendClassifier.afterDispatch(MutationOutcome.NOT_SENT, false, false, false));
    }
}
