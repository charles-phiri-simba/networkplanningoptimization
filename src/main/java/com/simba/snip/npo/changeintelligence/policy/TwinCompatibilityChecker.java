package com.simba.snip.npo.changeintelligence.policy;

import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.persist.NetworkTwinEntity;
import com.simba.snip.npo.persist.NetworkTwinRepository;
import com.simba.snip.npo.persist.NetworkTwinVersionEntity;
import com.simba.snip.npo.twin.TwinFreshness;
import com.simba.snip.npo.twin.TwinScopeType;
import com.simba.snip.npo.twin.TwinSynchronizationService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class TwinCompatibilityChecker {

    private final NetworkTwinRepository twinRepository;
    private final TwinSynchronizationService twinSynchronizationService;

    public TwinCompatibilityChecker(
            NetworkTwinRepository twinRepository,
            TwinSynchronizationService twinSynchronizationService
    ) {
        this.twinRepository = twinRepository;
        this.twinSynchronizationService = twinSynchronizationService;
    }

    public record CompatibilityResult(
            boolean compatible,
            ChangeProposalFailureCode failureCode,
            String reason,
            NetworkTwinEntity twin,
            NetworkTwinVersionEntity version
    ) {
        public static CompatibilityResult ok(NetworkTwinEntity twin, NetworkTwinVersionEntity version) {
            return new CompatibilityResult(true, null, null, twin, version);
        }

        public static CompatibilityResult fail(ChangeProposalFailureCode code, String reason) {
            return new CompatibilityResult(false, code, reason, null, null);
        }
    }

    public CompatibilityResult check(String cellId, BigDecimal expectedCurrentValue) {
        Optional<NetworkTwinEntity> twinOpt = twinRepository.findByScopeTypeAndScopeId(
                TwinScopeType.CELL.name(), cellId);
        if (twinOpt.isEmpty()) {
            return CompatibilityResult.fail(ChangeProposalFailureCode.TWIN_STATE_UNAVAILABLE, "no twin for cell");
        }
        NetworkTwinEntity twin = twinOpt.get();
        if (twin.getLatestVersion() < 1) {
            return CompatibilityResult.fail(ChangeProposalFailureCode.TWIN_STATE_UNAVAILABLE, "twin has no version");
        }
        NetworkTwinVersionEntity version = twinSynchronizationService.requireVersion(twin.getId(), twin.getLatestVersion());
        TwinFreshness freshness = twinSynchronizationService.freshness(version);
        if (freshness != TwinFreshness.CURRENT) {
            return CompatibilityResult.fail(
                    ChangeProposalFailureCode.TWIN_STATE_STALE,
                    "twin freshness=" + freshness.name()
            );
        }
        return CompatibilityResult.ok(twin, version);
    }
}
