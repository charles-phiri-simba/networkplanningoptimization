package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.persist.NetworkSourceReferenceEntity;
import com.simba.snip.npo.persist.NetworkSourceReferenceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class VendorIncrementalRemoveApplier {

    private final NetworkSourceReferenceRepository sourceReferenceRepository;

    public VendorIncrementalRemoveApplier(NetworkSourceReferenceRepository sourceReferenceRepository) {
        this.sourceReferenceRepository = sourceReferenceRepository;
    }

    @Transactional
    public int applyRemoves(VendorIncrementalBatch batch, UUID executionId, Instant now) {
        if (batch == null || batch.changes().isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (VendorIncrementalChange change : batch.changes()) {
            if (change.changeType() != VendorIncrementalChangeType.REMOVE) {
                continue;
            }
            NetworkSourceReferenceEntity reference = sourceReferenceRepository
                    .findByCanonicalEntityTypeAndCanonicalEntityIdAndAuthoritativeTrue(
                            change.entityType().name(),
                            change.canonicalEntityId()
                    )
                    .orElse(null);
            if (reference != null) {
                reference.markMissing(now, executionId);
                sourceReferenceRepository.save(reference);
                removed++;
            }
        }
        return removed;
    }
}
