package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only CanonicalReconciliationReader over Phase 12 RadioConfigurationEntity
 * and synchronization_checkpoint. Phase 18 never writes canonical state.
 * Proof Form A is evaluated from durable rows, not caller booleans.
 */
@Service
public class CanonicalReconciliationReader {

    private final NamedParameterJdbcTemplate jdbc;

    public CanonicalReconciliationReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void assertCanonicalReconciled(
            String cellId,
            String parameter,
            BigDecimal vendorVerifiedValue,
            Instant vendorVerifiedAt,
            UUID productionChangeId,
            String requiredCheckpointId
    ) {
        if (jdbc == null) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "canonical reader unavailable"
            );
        }
        if (cellId == null || parameter == null || vendorVerifiedValue == null || vendorVerifiedAt == null) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "Proof Form A material is incomplete"
            );
        }
        if (!"txPower".equals(parameter)) {
            throw new CampaignException(
                    ProductionReasonCode.UNSUPPORTED_OBJECT_OR_PARAMETER,
                    "reconciliation is CELL/txPower only"
            );
        }
        Map<String, Object> canonical;
        try {
            canonical = jdbc.queryForMap(
                    """
                    SELECT c.cell_id,
                           rc.parameter_name,
                           rc.parameter_value,
                           rc.unit,
                           rc.effective_from
                      FROM cell c
                      JOIN radio_configuration rc ON rc.cell_id = c.id
                     WHERE c.cell_id = :cellId
                       AND rc.parameter_name = :parameter
                     ORDER BY rc.effective_from DESC
                     LIMIT 1
                    """,
                    new MapSqlParameterSource()
                            .addValue("cellId", cellId)
                            .addValue("parameter", parameter)
            );
        } catch (DataAccessException ex) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "canonical RadioConfigurationEntity is unavailable",
                    ex
            );
        }
        if (!cellId.equals(String.valueOf(canonical.get("cell_id")))) {
            throw new CampaignException(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, "wrong cell");
        }
        if (!parameter.equals(String.valueOf(canonical.get("parameter_name")))) {
            throw new CampaignException(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, "wrong parameter");
        }
        if (!"dBm".equals(String.valueOf(canonical.get("unit")))) {
            throw new CampaignException(ProductionReasonCode.UNSUPPORTED_PARAMETER_UNIT, "canonical unit is not dBm");
        }
        BigDecimal canonicalValue = new BigDecimal(String.valueOf(canonical.get("parameter_value")));
        if (canonicalValue.compareTo(vendorVerifiedValue) != 0) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "canonical value does not equal vendor-verified value"
            );
        }
        Instant effectiveFrom = toInstant(canonical.get("effective_from"));
        if (effectiveFrom != null && effectiveFrom.isBefore(vendorVerifiedAt)) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "same-value canonical state existing before execution cannot prove reconciliation"
            );
        }
        Map<String, Object> checkpoint;
        try {
            MapSqlParameterSource ckptParams = new MapSqlParameterSource();
            String sql;
            if (requiredCheckpointId != null && requiredCheckpointId.length() == 36) {
                sql = """
                        SELECT id::text AS checkpoint_id, status, last_observed_at, checkpoint_type, source_system
                          FROM synchronization_checkpoint
                         WHERE id = :id
                        """;
                ckptParams.addValue("id", UUID.fromString(requiredCheckpointId));
            } else {
                sql = """
                        SELECT id::text AS checkpoint_id, status, last_observed_at, checkpoint_type, source_system
                          FROM synchronization_checkpoint
                         WHERE id::text = :id OR checkpoint_value = :id
                         ORDER BY last_observed_at DESC NULLS LAST
                         LIMIT 1
                        """;
                ckptParams.addValue("id", requiredCheckpointId);
            }
            checkpoint = jdbc.queryForMap(sql, ckptParams);
        } catch (Exception ex) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "authoritative Phase 12 checkpoint is unavailable",
                    ex
            );
        }
        if (!"VALID".equals(String.valueOf(checkpoint.get("status")))) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "checkpoint status is not successful"
            );
        }
        Instant lastObservedAt = toInstant(checkpoint.get("last_observed_at"));
        if (lastObservedAt == null || lastObservedAt.isBefore(vendorVerifiedAt)) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "canonical lastObservedAt is before vendorVerifiedAt"
            );
        }
        if (productionChangeId != null) {
            Integer interference = jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM campaign_external_interference i
                     JOIN campaign_execution_item it ON it.item_id = i.item_id
                     WHERE it.production_change_id = :changeId
                    """,
                    new MapSqlParameterSource("changeId", productionChangeId),
                    Integer.class
            );
            if (interference != null && interference > 0) {
                throw new CampaignException(
                        ProductionReasonCode.EXTERNAL_INTERFERENCE_DETECTED,
                        "superseding interference denies Proof Form A"
                );
            }
        }
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
