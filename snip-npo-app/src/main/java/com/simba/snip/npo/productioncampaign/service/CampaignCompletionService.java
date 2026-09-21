package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CampaignCompletionService {

    private static final List<String> SUCCESSFUL_FORWARD = List.of("COMPLETED");

    private static final List<String> BLOCKING = List.of(
            "OUTCOME_UNRESOLVED",
            "RECOVERY_REQUIRED",
            "MANUAL_INTERVENTION_REQUIRED"
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final CanonicalReconciliationReader reconciliationReader;

    public CampaignCompletionService(NamedParameterJdbcTemplate jdbc, CanonicalReconciliationReader reconciliationReader) {
        this.jdbc = jdbc;
        this.reconciliationReader = reconciliationReader;
    }

    @Transactional
    public void complete(UUID campaignId) {
        Integer blocking = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_execution_item
                 WHERE campaign_id = :id AND state IN (:blocking)
                """,
                new MapSqlParameterSource()
                        .addValue("id", campaignId)
                        .addValue("blocking", BLOCKING),
                Integer.class
        );
        if (blocking != null && blocking > 0) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE,
                    "campaign cannot complete while items remain unresolved, recovery-required, or manual"
            );
        }
        Integer authorized = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_execution_item i
                 JOIN production_change_campaign c ON c.campaign_id = i.campaign_id
                 JOIN campaign_revision r ON r.campaign_id = c.campaign_id
                  AND r.revision_number = c.current_revision_number
                 WHERE i.campaign_id = :id AND i.revision_id = r.revision_id
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        Integer completed = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_execution_item i
                 JOIN production_change_campaign c ON c.campaign_id = i.campaign_id
                 JOIN campaign_revision r ON r.campaign_id = c.campaign_id
                  AND r.revision_number = c.current_revision_number
                 WHERE i.campaign_id = :id
                   AND i.revision_id = r.revision_id
                   AND i.state IN (:ok)
                """,
                new MapSqlParameterSource()
                        .addValue("id", campaignId)
                        .addValue("ok", SUCCESSFUL_FORWARD),
                Integer.class
        );
        if (authorized == null || authorized == 0 || completed == null || !authorized.equals(completed)) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE,
                    "every authorized item must be COMPLETED exactly once with frozen forward evidence"
            );
        }
        Integer duplicates = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM (
                    SELECT item_id FROM campaign_execution_item
                     WHERE campaign_id = :id
                     GROUP BY item_id HAVING COUNT(*) > 1
                ) d
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if (duplicates != null && duplicates > 0) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE,
                    "duplicate item accounting is forbidden"
            );
        }
        Integer missingEvidence = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_execution_item i
                 WHERE i.campaign_id = :id
                   AND (
                        NOT EXISTS (
                            SELECT 1 FROM campaign_observation_boundary b
                             WHERE b.item_id = i.item_id
                               AND b.network_observation_health = 'HEALTHY'
                               AND b.operational_safety_health = 'SAFE'
                        )
                        OR EXISTS (
                            SELECT 1 FROM campaign_external_interference x
                             WHERE x.item_id = i.item_id
                        )
                   )
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if (missingEvidence != null && missingEvidence > 0) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE,
                    "COMPLETED requires production verification, reconciliation, HEALTHY+SAFE observation, and no interference"
            );
        }
        java.util.List<Map<String, Object>> items = jdbc.query(
                """
                SELECT i.item_id, i.cell_id, i.parameter, i.desired_value, i.production_change_id,
                       b.vendor_verified_at, b.verified_value, b.required_canonical_checkpoint
                  FROM campaign_execution_item i
                  JOIN campaign_observation_boundary b ON b.item_id = i.item_id
                 WHERE i.campaign_id = :id AND i.state = 'COMPLETED'
                """,
                new MapSqlParameterSource("id", campaignId),
                (rs, rowNum) -> {
                    java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("cell_id", rs.getString("cell_id"));
                    row.put("parameter", rs.getString("parameter"));
                    row.put("desired_value", rs.getBigDecimal("desired_value"));
                    row.put("production_change_id", rs.getObject("production_change_id"));
                    row.put("vendor_verified_at", rs.getTimestamp("vendor_verified_at"));
                    row.put("verified_value", rs.getBigDecimal("verified_value"));
                    row.put("required_canonical_checkpoint", rs.getString("required_canonical_checkpoint"));
                    return row;
                }
        );
        if (items.size() != authorized) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE,
                    "every COMPLETED item requires a bound observation boundary for Proof Form A"
            );
        }
        for (Map<String, Object> item : items) {
            java.sql.Timestamp vendorTs = (java.sql.Timestamp) item.get("vendor_verified_at");
            java.math.BigDecimal verified = (java.math.BigDecimal) item.get("verified_value");
            if (vendorTs == null || verified == null) {
                throw new CampaignException(
                        ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                        "Proof Form A material missing on completion"
                );
            }
            reconciliationReader.assertCanonicalReconciled(
                    String.valueOf(item.get("cell_id")),
                    String.valueOf(item.get("parameter")),
                    verified,
                    vendorTs.toInstant(),
                    (UUID) item.get("production_change_id"),
                    (String) item.get("required_canonical_checkpoint")
            );
        }
    }
}
