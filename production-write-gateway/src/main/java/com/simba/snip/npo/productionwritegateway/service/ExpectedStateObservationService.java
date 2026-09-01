package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.adapter.ObservationStatus;
import com.simba.snip.npo.productionwritegateway.adapter.PostMutationObservation;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import com.simba.snip.npo.productionwritegateway.transport.EricssonObservationRequest;
import com.simba.snip.npo.productionwritegateway.transport.EricssonWriteTransport;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ExpectedStateObservationService {

    private final EricssonWriteTransport transport;

    public ExpectedStateObservationService(EricssonWriteTransport transport) {
        this.transport = transport;
    }

    public PostMutationObservation observeExpected(ProductionNetworkChangeEntity change, GrantType grantType) {
        BigDecimal expected = grantType == GrantType.ROLLBACK
                ? change.getRollbackExpectedValue()
                : change.getExpectedValue();
        PostMutationObservation observation = transport.observeParameter(
                new EricssonObservationRequest(change.getCellId(), change.getParameter(), expected));
        return classify(observation, expected);
    }

    public PostMutationObservation observeDesired(ProductionNetworkChangeEntity change, GrantType grantType) {
        BigDecimal desired = grantType == GrantType.ROLLBACK
                ? change.getRollbackDesiredValue()
                : change.getDesiredValue();
        return transport.observeParameter(
                new EricssonObservationRequest(change.getCellId(), change.getParameter(), desired));
    }

    public void requireMatch(PostMutationObservation observation, UUID grantId, UUID changeId) {
        if (observation.status() != ObservationStatus.MATCH) {
            throw GatewayDeniedException.deny(reasonFor(observation.status()), grantId, changeId);
        }
    }

    private PostMutationObservation classify(PostMutationObservation observation, BigDecimal expected) {
        if (observation.status() != ObservationStatus.MATCH
                && observation.status() != ObservationStatus.MISMATCH
                && observation.observedValue() != null
                && expected != null
                && observation.observedValue().compareTo(expected) == 0
                && observation.status() != ObservationStatus.STALE
                && observation.status() != ObservationStatus.TIMEOUT
                && observation.status() != ObservationStatus.SOURCE_UNAVAILABLE
                && observation.status() != ObservationStatus.UNKNOWN) {
            return new PostMutationObservation(ObservationStatus.MATCH, observation.observedValue(), observation.observedAt());
        }
        if (observation.status() == ObservationStatus.MATCH
                || observation.status() == ObservationStatus.MISMATCH
                || observation.status() == ObservationStatus.UNKNOWN
                || observation.status() == ObservationStatus.TIMEOUT
                || observation.status() == ObservationStatus.SOURCE_UNAVAILABLE
                || observation.status() == ObservationStatus.STALE) {
            if (observation.status() == ObservationStatus.MISMATCH
                    || (observation.observedValue() != null
                    && expected != null
                    && observation.observedValue().compareTo(expected) != 0
                    && observation.status() != ObservationStatus.TIMEOUT
                    && observation.status() != ObservationStatus.SOURCE_UNAVAILABLE
                    && observation.status() != ObservationStatus.STALE
                    && observation.status() != ObservationStatus.UNKNOWN)) {
                return new PostMutationObservation(
                        ObservationStatus.MISMATCH, observation.observedValue(), observation.observedAt());
            }
            if (observation.observedValue() != null
                    && expected != null
                    && observation.observedValue().compareTo(expected) == 0
                    && observation.status() != ObservationStatus.STALE
                    && observation.status() != ObservationStatus.TIMEOUT
                    && observation.status() != ObservationStatus.SOURCE_UNAVAILABLE
                    && observation.status() != ObservationStatus.UNKNOWN) {
                return new PostMutationObservation(
                        ObservationStatus.MATCH, observation.observedValue(), observation.observedAt());
            }
            return observation;
        }
        return observation;
    }

    private static ProductionReasonCode reasonFor(ObservationStatus status) {
        return switch (status) {
            case MISMATCH -> ProductionReasonCode.PRODUCTION_EXPECTED_STATE_MISMATCH;
            case UNKNOWN, STALE -> ProductionReasonCode.PRODUCTION_EXPECTED_STATE_UNKNOWN;
            case TIMEOUT -> ProductionReasonCode.PRODUCTION_VERIFICATION_TIMEOUT;
            case SOURCE_UNAVAILABLE -> ProductionReasonCode.PRODUCTION_VERIFICATION_UNAVAILABLE;
            case MATCH -> ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED;
        };
    }
}
