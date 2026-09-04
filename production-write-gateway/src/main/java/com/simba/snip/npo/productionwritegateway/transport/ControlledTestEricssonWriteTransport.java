package com.simba.snip.npo.productionwritegateway.transport;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.adapter.ObservationStatus;
import com.simba.snip.npo.productionwritegateway.adapter.PostMutationObservation;
import com.simba.snip.npo.productionwritegateway.adapter.VendorMutationResult;
import com.simba.snip.npo.productionwritegateway.vendortransport.DestinationTrustValidator;
import com.simba.snip.npo.productionwritegateway.vendortransport.ObservedVendorSessionIdentityProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test/lab transport only. Impossible when production-runtime=true.
 * Do not use as a production Ericsson protocol implementation.
 */
@Component
@Primary
@ConditionalOnExpression("'${snip.production-change.test-transport-enabled:false}'=='true' && '${snip.integration.security.production-runtime:false}'!='true'")
public class ControlledTestEricssonWriteTransport implements EricssonWriteTransport, ObservedVendorSessionIdentityProvider {

    public enum FailureMode {
        NONE,
        TIMEOUT_AFTER_APPLY,
        CONNECTION_LOST_AFTER_APPLY,
        APPLY_WRONG_VALUE,
        REJECT,
        RESPONSE_LOST,
        APPLY_SUCCESS,
        OBSERVE_MISMATCH,
        OBSERVE_UNAVAILABLE,
        OBSERVE_TIMEOUT,
        OBSERVE_STALE,
        THIRD_VALUE,
        RETURN_EXPECTED_AFTER_APPLY
    }

    private final ConcurrentHashMap<String, BigDecimal> cellTxPower = new ConcurrentHashMap<>();
    private final AtomicInteger mutationInvocationCounter = new AtomicInteger();
    private volatile FailureMode failureMode = FailureMode.NONE;
    private volatile FailureMode observeMode = FailureMode.NONE;
    private volatile int observeCalls;
    private volatile boolean atomicSupported;
    private volatile DestinationTrustValidator.ObservedDestination observedDestination =
            new DestinationTrustValidator.ObservedDestination(
                    "enm.lab.invalid", 443, "enm.lab.invalid", true, true, "LAB", "zone-a");

    public AtomicInteger getMutationInvocationCounter() {
        return mutationInvocationCounter;
    }

    public void seedCell(String cellId, BigDecimal txPower) {
        cellTxPower.put(cellId, txPower);
    }

    public void setFailureMode(FailureMode failureMode) {
        this.failureMode = failureMode;
        this.observeMode = failureMode;
        this.observeCalls = 0;
    }

    public void setAtomicSupported(boolean atomicSupported) {
        this.atomicSupported = atomicSupported;
    }

    public void reset() {
        cellTxPower.clear();
        mutationInvocationCounter.set(0);
        failureMode = FailureMode.NONE;
        observeMode = FailureMode.NONE;
        observeCalls = 0;
        atomicSupported = false;
        observedDestination = new DestinationTrustValidator.ObservedDestination(
                "enm.lab.invalid", 443, "enm.lab.invalid", true, true, "LAB", "zone-a");
    }

    public void setObservedDestination(DestinationTrustValidator.ObservedDestination observedDestination) {
        this.observedDestination = observedDestination;
    }

    @Override
    public Optional<DestinationTrustValidator.ObservedDestination> currentObserved() {
        return Optional.ofNullable(observedDestination);
    }

    @Override
    public VendorMutationResult transmitMutation(EricssonMutationRequest request) {
        mutationInvocationCounter.incrementAndGet();
        FailureMode mode = failureMode;
        return switch (mode) {
            case REJECT -> VendorMutationResult.rejected(ProductionReasonCode.PRODUCTION_VENDOR_REJECTION);
            case APPLY_WRONG_VALUE -> {
                cellTxPower.put(request.cellId(), request.desiredValue().add(BigDecimal.ONE));
                yield VendorMutationResult.accepted();
            }
            case TIMEOUT_AFTER_APPLY, CONNECTION_LOST_AFTER_APPLY, RESPONSE_LOST -> {
                cellTxPower.put(request.cellId(), request.desiredValue());
                yield VendorMutationResult.unknown(ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN, true);
            }
            case RETURN_EXPECTED_AFTER_APPLY -> {
                cellTxPower.put(request.cellId(), request.expectedValue());
                yield VendorMutationResult.unknown(ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN, true);
            }
            case THIRD_VALUE -> {
                cellTxPower.put(request.cellId(), request.desiredValue().add(new BigDecimal("3")));
                yield VendorMutationResult.unknown(ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN, true);
            }
            default -> {
                cellTxPower.put(request.cellId(), request.desiredValue());
                yield VendorMutationResult.accepted();
            }
        };
    }

    @Override
    public PostMutationObservation observeParameter(EricssonObservationRequest request) {
        observeCalls++;
        FailureMode mode = observeMode;
        if (observeCalls == 1) {
            if (mode == FailureMode.OBSERVE_MISMATCH) {
                return new PostMutationObservation(
                        ObservationStatus.MISMATCH, request.compareValue().add(BigDecimal.ONE), Instant.now());
            }
            if (mode == FailureMode.OBSERVE_UNAVAILABLE) {
                return new PostMutationObservation(ObservationStatus.SOURCE_UNAVAILABLE, null, Instant.now());
            }
            if (mode == FailureMode.OBSERVE_TIMEOUT) {
                return new PostMutationObservation(ObservationStatus.TIMEOUT, null, Instant.now());
            }
            if (mode == FailureMode.OBSERVE_STALE) {
                return new PostMutationObservation(ObservationStatus.STALE, request.compareValue(), Instant.now());
            }
        }
        BigDecimal actual = cellTxPower.get(request.cellId());
        if (actual == null) {
            return new PostMutationObservation(ObservationStatus.SOURCE_UNAVAILABLE, null, Instant.now());
        }
        ObservationStatus status = actual.compareTo(request.compareValue()) == 0
                ? ObservationStatus.MATCH
                : ObservationStatus.MISMATCH;
        return new PostMutationObservation(status, actual, Instant.now());
    }

    @Override
    public boolean supportsAtomicCompareAndSet() {
        return atomicSupported;
    }
}
