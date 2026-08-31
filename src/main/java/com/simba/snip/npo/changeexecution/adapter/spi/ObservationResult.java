package com.simba.snip.npo.changeexecution.adapter.spi;

import com.simba.snip.npo.changeexecution.domain.VerificationOutcome;

import java.time.Instant;

public record ObservationResult(
        VerificationOutcome outcome,
        String observedValue,
        Long targetRevision,
        Instant observedAt,
        String reasonCode,
        String evidenceSummary
) {
    public static ObservationResult verified(String observedValue, Long revision, Instant observedAt) {
        return new ObservationResult(
                VerificationOutcome.VERIFIED,
                observedValue,
                revision,
                observedAt,
                null,
                "independent readback matched expected value"
        );
    }

    public static ObservationResult mismatch(String observedValue, Long revision, Instant observedAt) {
        return new ObservationResult(
                VerificationOutcome.MISMATCH,
                observedValue,
                revision,
                observedAt,
                "EXECUTION_VERIFICATION_MISMATCH",
                "observed value differs from expected"
        );
    }

    public static ObservationResult stale(String observedValue, Long revision, Instant observedAt) {
        return new ObservationResult(
                VerificationOutcome.STALE_OBSERVATION,
                observedValue,
                revision,
                observedAt,
                "EXECUTION_VERIFICATION_UNKNOWN",
                "observation revision is stale"
        );
    }

    public static ObservationResult timeout(String reasonCode) {
        return new ObservationResult(
                VerificationOutcome.TIMEOUT,
                null,
                null,
                Instant.now(),
                reasonCode,
                "readback timed out"
        );
    }

    public static ObservationResult unknown(String reasonCode, String summary) {
        return new ObservationResult(
                VerificationOutcome.UNKNOWN,
                null,
                null,
                Instant.now(),
                reasonCode,
                summary
        );
    }
}
