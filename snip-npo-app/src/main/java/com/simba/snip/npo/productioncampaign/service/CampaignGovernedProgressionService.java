package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Production writer for post-handoff item states. Uses the frozen matrix only.
 */
@Service
public class CampaignGovernedProgressionService {

    private final CampaignItemLifecycleService itemLifecycle;
    private final CanonicalReconciliationReader reconciliationReader;
    private final CampaignObservationService observationService;
    private final NamedParameterJdbcTemplate jdbc;

    public CampaignGovernedProgressionService(
            CampaignItemLifecycleService itemLifecycle,
            CanonicalReconciliationReader reconciliationReader,
            CampaignObservationService observationService,
            NamedParameterJdbcTemplate jdbc
    ) {
        this.itemLifecycle = itemLifecycle;
        this.reconciliationReader = reconciliationReader;
        this.observationService = observationService;
        this.jdbc = jdbc;
    }

    @Transactional
    public String markPreSend(UUID itemId) {
        return itemLifecycle.transition(itemId, "HANDED_OFF", "PRE_SEND_READY");
    }

    @Transactional
    public String recordGatewaySendBoundary(UUID itemId) {
        return itemLifecycle.transition(itemId, "PRE_SEND", "GATEWAY_SEND_BOUNDARY");
    }

    @Transactional
    public String recordAcceptance(UUID itemId) {
        return itemLifecycle.transition(itemId, "MAY_HAVE_SENT", "ACCEPTANCE_EVIDENCE");
    }

    @Transactional
    public String startVerifyFromAccepted(UUID itemId) {
        return itemLifecycle.transition(itemId, "VENDOR_ACCEPTED", "START_VERIFY");
    }

    @Transactional
    public String recordDesiredReadback(UUID itemId) {
        return itemLifecycle.transition(itemId, "VERIFYING", "DIRECT_DESIRED_READBACK");
    }

    @Transactional
    public String beginReconcile(UUID itemId) {
        return itemLifecycle.transition(itemId, "PRODUCTION_VERIFIED", "BEGIN_RECONCILE");
    }

    @Transactional
    public String applyProofFormA(UUID itemId) {
        Map<String, Object> item = jdbc.queryForMap(
                """
                SELECT i.cell_id, i.parameter, i.desired_value, i.production_change_id,
                       b.vendor_verified_at, b.verified_value, b.required_canonical_checkpoint
                  FROM campaign_execution_item i
                  LEFT JOIN campaign_observation_boundary b ON b.item_id = i.item_id
                 WHERE i.item_id = :id
                 ORDER BY b.created_at DESC NULLS LAST
                 LIMIT 1
                """,
                new MapSqlParameterSource("id", itemId)
        );
        Instant vendorVerifiedAt = toInstant(item.get("vendor_verified_at"));
        BigDecimal verified = item.get("verified_value") instanceof BigDecimal bd ? bd : null;
        if (vendorVerifiedAt == null || verified == null) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "Proof Form A requires vendor-verified value; desired_value is intent, not evidence"
            );
        }
        reconciliationReader.assertCanonicalReconciled(
                String.valueOf(item.get("cell_id")),
                String.valueOf(item.get("parameter")),
                verified,
                vendorVerifiedAt,
                (UUID) item.get("production_change_id"),
                item.get("required_canonical_checkpoint") == null
                        ? null
                        : String.valueOf(item.get("required_canonical_checkpoint"))
        );
        return itemLifecycle.transition(itemId, "RECONCILIATION_PENDING", "PROOF_FORM_A_OR_B");
    }

    @Transactional
    public String openObservation(UUID itemId) {
        return itemLifecycle.transition(itemId, "CANONICAL_RECONCILED", "OPEN_OBSERVATION");
    }

    @Transactional
    public String applyHealthyAndSafe(UUID itemId) {
        UUID campaignId = jdbc.queryForObject(
                "SELECT campaign_id FROM campaign_execution_item WHERE item_id = :id",
                new MapSqlParameterSource("id", itemId),
                UUID.class
        );
        observationService.assertCurrentHealthyAndSafe(campaignId);
        return itemLifecycle.transition(itemId, "OBSERVING", "INTERVAL_HEALTHY_SAFE");
    }

    @Transactional
    public String completeItem(UUID itemId) {
        return itemLifecycle.transition(itemId, "OBSERVATION_HEALTHY", "COMPLETE_ITEM");
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        return Instant.parse(value.toString());
    }
}
