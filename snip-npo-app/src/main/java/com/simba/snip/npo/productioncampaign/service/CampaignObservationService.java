package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.CanonicalJson;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CampaignObservationService {

    public enum NetworkObservationHealth { HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN, STALE }

    public enum OperationalSafetyHealth { SAFE, UNSAFE, UNKNOWN, STALE }

    public record ObservationEvidence(
            Instant vendorVerifiedAt,
            Instant observationStart,
            Instant minimumMeasurementTime,
            String source,
            String watermarkType,
            String watermarkValue,
            Long maxIngestionLagMs,
            String requiredCanonicalCheckpoint,
            NetworkObservationHealth networkHealth,
            OperationalSafetyHealth safetyHealth,
            boolean historicalOnly
    ) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ExternalInterferenceService interferenceService;

    /**
     * Retained for unit tests that exercise observation predicates without a JDBC context.
     * Production Spring wiring uses the autowired constructor only.
     */
    public CampaignObservationService() {
        this.jdbc = null;
        this.interferenceService = null;
    }

    @Autowired
    public CampaignObservationService(NamedParameterJdbcTemplate jdbc, ExternalInterferenceService interferenceService) {
        this.jdbc = jdbc;
        this.interferenceService = interferenceService;
    }

    public void assertProgressionAdmissible(ObservationEvidence evidence) {
        if (evidence == null
                || evidence.source() == null || evidence.source().isBlank()
                || evidence.minimumMeasurementTime() == null
                || evidence.watermarkType() == null
                || evidence.requiredCanonicalCheckpoint() == null) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "ingestion time alone is not sufficient observation provenance"
            );
        }
        if (evidence.vendorVerifiedAt() != null
                && evidence.observationStart() != null
                && evidence.observationStart().isBefore(evidence.vendorVerifiedAt())) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "observationStart must be >= vendorVerifiedAt"
            );
        }
        if (evidence.historicalOnly()) {
            throw new CampaignException(
                    ProductionReasonCode.OBSERVATION_NOT_HEALTHY,
                    "historical HEALTHY/SAFE is not future admissibility"
            );
        }
        if (evidence.networkHealth() != NetworkObservationHealth.HEALTHY) {
            throw new CampaignException(
                    ProductionReasonCode.OBSERVATION_NOT_HEALTHY,
                    "progression requires NetworkObservationHealth=HEALTHY"
            );
        }
        if (evidence.safetyHealth() != OperationalSafetyHealth.SAFE) {
            throw new CampaignException(
                    ProductionReasonCode.OBSERVATION_NOT_SAFE,
                    "progression requires OperationalSafetyHealth=SAFE"
            );
        }
    }

    @Transactional
    public UUID persistBoundary(
            UUID campaignId,
            UUID revisionId,
            UUID cohortId,
            UUID itemId,
            UUID productionChangeId,
            String cellId,
            String parameter,
            BigDecimal verifiedValue,
            Instant vendorVerifiedAt,
            Instant observationStart,
            Instant observationEnd,
            String source,
            Instant minimumMeasurementTime,
            String watermarkType,
            String watermarkValue,
            Long maxIngestionLagMs,
            String requiredCanonicalCheckpoint,
            String healthPolicyVersion,
            String observationPolicyVersion,
            NetworkObservationHealth networkHealth,
            OperationalSafetyHealth safetyHealth
    ) {
        if (observationStart == null || vendorVerifiedAt == null || observationStart.isBefore(vendorVerifiedAt)) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "observationStart must be >= vendorVerifiedAt"
            );
        }
        if (!"SOURCE_NATIVE_WATERMARK".equals(watermarkType) && !"PHASE12_CHECKPOINT".equals(watermarkType)) {
            throw new CampaignException(
                    ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE,
                    "watermark type must be SOURCE_NATIVE_WATERMARK or PHASE12_CHECKPOINT"
            );
        }
        Map<String, Object> digestMaterial = new LinkedHashMap<>();
        digestMaterial.put("campaignId", campaignId.toString());
        digestMaterial.put("revisionId", revisionId.toString());
        digestMaterial.put("cohortId", cohortId.toString());
        digestMaterial.put("itemId", itemId.toString());
        digestMaterial.put("productionChangeId", productionChangeId == null ? null : productionChangeId.toString());
        digestMaterial.put("cellId", cellId);
        digestMaterial.put("parameter", parameter);
        digestMaterial.put("verifiedValue", CampaignHandoffIdFactory.canonicalDecimal(verifiedValue));
        digestMaterial.put("vendorVerifiedAt", vendorVerifiedAt.toString());
        digestMaterial.put("observationStart", observationStart.toString());
        digestMaterial.put("observationEnd", observationEnd == null ? null : observationEnd.toString());
        digestMaterial.put("source", source);
        digestMaterial.put("minimumMeasurementTime", minimumMeasurementTime == null ? null : minimumMeasurementTime.toString());
        digestMaterial.put("watermarkType", watermarkType);
        digestMaterial.put("watermarkValue", watermarkValue);
        digestMaterial.put("maxIngestionLagMs", maxIngestionLagMs);
        digestMaterial.put("requiredCanonicalCheckpoint", requiredCanonicalCheckpoint);
        digestMaterial.put("healthPolicyVersion", healthPolicyVersion);
        digestMaterial.put("observationPolicyVersion", observationPolicyVersion);
        String digest = Sha256Hex.hash(CanonicalJson.serialize(digestMaterial));
        UUID boundaryId = UUID.randomUUID();
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("health", healthPolicyVersion);
        policy.put("observation", observationPolicyVersion);
        jdbc.update(
                """
                INSERT INTO campaign_observation_boundary (
                    boundary_id, campaign_id, revision_id, cohort_id, item_id, production_change_id,
                    cell_id, parameter, verified_value, vendor_verified_at, observation_start, observation_end,
                    source, minimum_measurement_time, watermark_type, watermark_value, max_ingestion_lag_ms,
                    required_canonical_checkpoint, policy_versions, network_observation_health,
                    operational_safety_health, boundary_digest, created_at)
                VALUES (
                    :id, :campaignId, :revisionId, :cohortId, :itemId, :changeId,
                    :cellId, :parameter, :verified, :vendorAt, :start, :end,
                    :source, :minMeas, :wmType, :wmValue, :lag,
                    :ckpt, :policies, :netHealth, :safetyHealth, :digest, :now)
                """,
                new MapSqlParameterSource()
                        .addValue("id", boundaryId)
                        .addValue("campaignId", campaignId)
                        .addValue("revisionId", revisionId)
                        .addValue("cohortId", cohortId)
                        .addValue("itemId", itemId)
                        .addValue("changeId", productionChangeId)
                        .addValue("cellId", cellId)
                        .addValue("parameter", parameter)
                        .addValue("verified", verifiedValue)
                        .addValue("vendorAt", Timestamp.from(vendorVerifiedAt))
                        .addValue("start", Timestamp.from(observationStart))
                        .addValue("end", observationEnd == null ? null : Timestamp.from(observationEnd))
                        .addValue("source", source)
                        .addValue("minMeas", minimumMeasurementTime == null ? null : Timestamp.from(minimumMeasurementTime))
                        .addValue("wmType", watermarkType)
                        .addValue("wmValue", watermarkValue)
                        .addValue("lag", maxIngestionLagMs)
                        .addValue("ckpt", requiredCanonicalCheckpoint)
                        .addValue("policies", CanonicalJson.serialize(policy))
                        .addValue("netHealth", networkHealth.name())
                        .addValue("safetyHealth", safetyHealth.name())
                        .addValue("digest", digest)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
        return boundaryId;
    }

    public void assertCurrentHealthyAndSafe(UUID campaignId) {
        Map<String, Object> row;
        try {
            row = jdbc.queryForMap(
                    """
                    SELECT network_observation_health, operational_safety_health, policy_versions,
                           vendor_verified_at, observation_start, item_id, cell_id, parameter
                      FROM campaign_observation_boundary
                     WHERE campaign_id = :id
                     ORDER BY created_at DESC
                     LIMIT 1
                    """,
                    new MapSqlParameterSource("id", campaignId)
            );
        } catch (Exception ex) {
            throw new CampaignException(
                    ProductionReasonCode.OBSERVATION_NOT_HEALTHY,
                    "no current PostChangeObservationBoundary",
                    ex
            );
        }
        String net = String.valueOf(row.get("network_observation_health"));
        String safety = String.valueOf(row.get("operational_safety_health"));
        if ("UNKNOWN".equals(net) || "STALE".equals(net) || "DEGRADED".equals(net) || "UNHEALTHY".equals(net)) {
            throw new CampaignException(ProductionReasonCode.OBSERVATION_NOT_HEALTHY, "current network observation is " + net);
        }
        if ("UNKNOWN".equals(safety) || "STALE".equals(safety) || "UNSAFE".equals(safety)) {
            throw new CampaignException(ProductionReasonCode.OBSERVATION_NOT_SAFE, "current operational safety is " + safety);
        }
        if (!"HEALTHY".equals(net) || !"SAFE".equals(safety)) {
            throw new CampaignException(ProductionReasonCode.OBSERVATION_NOT_HEALTHY, "progression requires HEALTHY and SAFE");
        }
        String policies = String.valueOf(row.get("policy_versions"));
        if (policies == null || policies.isBlank() || "{}".equals(policies)) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_STALE, "observation/health policy versions are stale");
        }
        interferenceService.detectForActiveBoundary(
                campaignId,
                (UUID) row.get("item_id"),
                String.valueOf(row.get("cell_id")),
                String.valueOf(row.get("parameter"))
        );
    }
}
