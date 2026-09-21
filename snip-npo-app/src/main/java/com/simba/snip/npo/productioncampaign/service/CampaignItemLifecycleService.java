package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Frozen item transition matrix (specification §18). Unspecified transitions DENY.
 * No generic state setter.
 */
@Service
public class CampaignItemLifecycleService {

    static final Map<String, String> ALLOWED = frozenMatrix();

    public static String destinationFor(String from, String trigger) {
        return ALLOWED.get(from + "|" + trigger);
    }

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignItemLifecycleService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static Map<String, String> frozenMatrix() {
        Map<String, String> matrix = new LinkedHashMap<>();
        matrix.put("PLANNED|CURRENT_EXACT_COHORT_RELEASE", "RELEASED");
        matrix.put("RELEASED|INDIVIDUAL_ELIGIBILITY", "ELIGIBLE");
        matrix.put("ELIGIBLE|START_HANDOFF", "HANDOFF_PENDING");
        matrix.put("HANDOFF_PENDING|P16_LINEAGE_CORRELATED", "HANDED_OFF");
        matrix.put("HANDED_OFF|PRE_SEND_READY", "PRE_SEND");
        matrix.put("PRE_SEND|GATEWAY_SEND_BOUNDARY", "MAY_HAVE_SENT");
        matrix.put("PRE_SEND|AUTHORITATIVE_NO_SEND", "NOT_SENT");
        matrix.put("PRE_SEND|AUTHORITATIVE_PRE_SEND_REJECTION", "VENDOR_REJECTED");
        matrix.put("HANDED_OFF|AUTHORITATIVE_PRE_SEND_REJECTION", "VENDOR_REJECTED");
        matrix.put("HANDOFF_PENDING|AUTHORITATIVE_NO_SEND", "NOT_SENT");
        matrix.put("MAY_HAVE_SENT|ACCEPTANCE_EVIDENCE", "VENDOR_ACCEPTED");
        matrix.put("MAY_HAVE_SENT|ACCEPTANCE_UNAVAILABLE_VERIFY_REQUIRED", "VERIFYING");
        matrix.put("MAY_HAVE_SENT|OUTCOME_UNESTABLISHABLE", "OUTCOME_UNRESOLVED");
        matrix.put("VENDOR_ACCEPTED|START_VERIFY", "VERIFYING");
        matrix.put("VERIFYING|DIRECT_DESIRED_READBACK", "PRODUCTION_VERIFIED");
        matrix.put("VERIFYING|EXPECTED_PRECHANGE", "NOT_SENT");
        matrix.put("VERIFYING|THIRD_UNEXPECTED_STATE", "MANUAL_INTERVENTION_REQUIRED");
        matrix.put("VERIFYING|GOVERNED_FAILURE", "VERIFICATION_FAILED");
        matrix.put("VERIFYING|GOVERNED_FAILURE_RECOVERY", "RECOVERY_REQUIRED");
        matrix.put("VERIFYING|SEND_UNRESOLVED", "OUTCOME_UNRESOLVED");
        matrix.put("PRODUCTION_VERIFIED|BEGIN_RECONCILE", "RECONCILIATION_PENDING");
        matrix.put("RECONCILIATION_PENDING|PROOF_FORM_A_OR_B", "CANONICAL_RECONCILED");
        matrix.put("CANONICAL_RECONCILED|OPEN_OBSERVATION", "OBSERVING");
        matrix.put("OBSERVING|INTERVAL_HEALTHY_SAFE", "OBSERVATION_HEALTHY");
        matrix.put("OBSERVATION_HEALTHY|COMPLETE_ITEM", "COMPLETED");
        matrix.put("ELIGIBLE|BLOCKING_CONDITION", "BLOCKED");
        matrix.put("RELEASED|BLOCKING_CONDITION", "BLOCKED");
        matrix.put("HANDED_OFF|BLOCKING_CONDITION", "BLOCKED");
        matrix.put("PRE_SEND|MATERIAL_INVALIDATION", "STALE");
        matrix.put("ELIGIBLE|MATERIAL_INVALIDATION", "STALE");
        matrix.put("RELEASED|OPERATOR_PAUSE", "SUSPENDED");
        matrix.put("ELIGIBLE|OPERATOR_PAUSE", "SUSPENDED");
        matrix.put("ELIGIBLE|SAFETY_SUSPEND", "SUSPENDED");
        return Map.copyOf(matrix);
    }

    @Transactional
    public String transition(UUID itemId, String from, String trigger) {
        String to = ALLOWED.get(from + "|" + trigger);
        if (to == null) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "item transition not permitted: " + from + " / " + trigger
            );
        }
        int updated = jdbc.update(
                """
                UPDATE campaign_execution_item
                   SET state = :to, updated_at = :now, version = version + 1
                 WHERE item_id = :id AND state = :from
                """,
                new MapSqlParameterSource()
                        .addValue("id", itemId)
                        .addValue("from", from)
                        .addValue("to", to)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "item state did not match expected from-state " + from
            );
        }
        return to;
    }

    public String transitionFromCurrent(UUID itemId, String trigger) {
        return transition(itemId, currentState(itemId), trigger);
    }

    public String currentState(UUID itemId) {
        String state = jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = :id",
                new MapSqlParameterSource("id", itemId),
                String.class
        );
        if (state == null) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "item not found");
        }
        return state;
    }
}
