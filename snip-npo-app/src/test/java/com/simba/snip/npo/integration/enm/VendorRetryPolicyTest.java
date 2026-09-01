package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.ImportFailureCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendorRetryPolicyTest {

    @Test
    void retryableCodesAreBounded() {
        VendorRetryPolicy policy = new VendorRetryPolicy(3, Duration.ofMillis(10), Duration.ofMillis(100));
        assertTrue(policy.retryable(ImportFailureCode.VENDOR_RATE_LIMITED));
        assertTrue(policy.retryable(ImportFailureCode.VENDOR_TIMEOUT));
        assertTrue(policy.retryable(ImportFailureCode.VENDOR_UNAVAILABLE));
        assertFalse(policy.retryable(ImportFailureCode.VENDOR_AUTHENTICATION_FAILED));
        assertFalse(policy.retryable(ImportFailureCode.VENDOR_AUTHORIZATION_DENIED));
        assertFalse(policy.retryable(ImportFailureCode.VENDOR_RESPONSE_INVALID));
        assertFalse(policy.retryable(ImportFailureCode.VENDOR_PAGINATION_INVALID));
        assertFalse(policy.retryable(ImportFailureCode.SNAPSHOT_LIMIT_EXCEEDED));
        assertFalse(policy.retryable(ImportFailureCode.CONNECTOR_CANCELLED));
        assertFalse(policy.retryable(ImportFailureCode.LEASE_LOST));
        assertFalse(policy.retryable(ImportFailureCode.PRODUCTION_TRANSPORT_NOT_CONFIGURED));
    }

    @Test
    void retryAfterIsCappedAtMaxBackoff() {
        VendorRetryPolicy policy = new VendorRetryPolicy(3, Duration.ofMillis(10), Duration.ofMillis(50));
        assertEquals(Duration.ofMillis(50), policy.backoff(0, Duration.ofMillis(500)));
        assertEquals(Duration.ofMillis(20), policy.backoff(0, Duration.ofMillis(20)));
    }
}
