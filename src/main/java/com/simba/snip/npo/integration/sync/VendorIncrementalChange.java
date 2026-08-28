package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.integration.CanonicalEntityType;

public record VendorIncrementalChange(
        VendorIncrementalChangeType changeType,
        CanonicalEntityType entityType,
        String sourceEntityId,
        String canonicalEntityId
) {
}
