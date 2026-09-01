package com.simba.snip.npo.integration.sync;

public enum SynchronizationSourceHealth {
    UNKNOWN,
    HEALTHY,
    SYNCHRONIZING,
    DEGRADED,
    STALE,
    UNREACHABLE,
    AUTHENTICATION_FAILED,
    AUTHORIZATION_FAILED,
    THROTTLED,
    RECOVERING,
    DISABLED
}
