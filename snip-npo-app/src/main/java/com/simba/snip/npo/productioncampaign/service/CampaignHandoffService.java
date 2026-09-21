package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.dao.DuplicateKeyException;
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
public class CampaignHandoffService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CampaignHandoffIdFactory handoffIdFactory;
    private final CampaignMutationSlotService slotService;
    private final ForwardProgressionClosureService closureService;
    private final CampaignAuditService auditService;

    public CampaignHandoffService(
            NamedParameterJdbcTemplate jdbc,
            CampaignHandoffIdFactory handoffIdFactory,
            CampaignMutationSlotService slotService,
            ForwardProgressionClosureService closureService,
            CampaignAuditService auditService
    ) {
        this.jdbc = jdbc;
        this.handoffIdFactory = handoffIdFactory;
        this.slotService = slotService;
        this.closureService = closureService;
        this.auditService = auditService;
    }

    @Transactional
    public String createOrReturnForwardHandoff(
            UUID campaignId,
            UUID revisionId,
            int revisionNumber,
            UUID itemId,
            UUID cohortId,
            long controlGeneration,
            long releaseGeneration,
            long fence,
            String campaignFingerprint,
            String releaseFingerprint,
            String itemFingerprint,
            String targetId,
            UUID phase14PlanId,
            String phase14PlanFingerprint,
            UUID phase15ExecutionId,
            String phase15ExecutionFingerprint,
            String cellId,
            BigDecimal expected,
            BigDecimal desired,
            BigDecimal rollback
    ) {
        closureService.assertOpen(campaignId);
        String campaignState = jdbc.queryForObject(
                "SELECT state FROM production_change_campaign WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId),
                String.class
        );
        if ("SUSPENDED".equals(campaignState)
                || "STALE".equals(campaignState)
                || "ABORTED".equals(campaignState)
                || "EXPIRED".equals(campaignState)) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_STALE,
                    "invalidated campaign cannot start a new handoff"
            );
        }
        jdbc.queryForObject(
                "SELECT campaign_id FROM production_change_campaign WHERE campaign_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", campaignId),
                UUID.class
        );
        Integer sequence = jdbc.queryForObject(
                "SELECT item_sequence FROM campaign_execution_item WHERE item_id = :id",
                new MapSqlParameterSource("id", itemId),
                Integer.class
        );
        if (sequence == null) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "item missing for binding");
        }
        Map<String, Object> itemRow = jdbc.queryForMap(
                """
                SELECT item_fingerprint, phase14_plan_id, phase14_plan_fingerprint,
                       phase15_execution_id, phase15_execution_fingerprint
                  FROM campaign_execution_item
                 WHERE item_id = :id
                """,
                new MapSqlParameterSource("id", itemId)
        );
        String authoritativeItemFingerprint = String.valueOf(itemRow.get("item_fingerprint"));
        UUID authoritativeP14 = (UUID) itemRow.get("phase14_plan_id");
        String authoritativeP14Fp = itemRow.get("phase14_plan_fingerprint") == null
                ? null
                : String.valueOf(itemRow.get("phase14_plan_fingerprint"));
        UUID authoritativeP15 = (UUID) itemRow.get("phase15_execution_id");
        String authoritativeP15Fp = itemRow.get("phase15_execution_fingerprint") == null
                ? null
                : String.valueOf(itemRow.get("phase15_execution_fingerprint"));
        if (itemFingerprint != null && !itemFingerprint.equals(authoritativeItemFingerprint)) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH,
                    "item fingerprint is not current"
            );
        }
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("campaignId", campaignId.toString());
        binding.put("revisionNumber", revisionNumber);
        binding.put("campaignFingerprint", campaignFingerprint);
        binding.put("controlGeneration", controlGeneration);
        binding.put("cohortId", cohortId.toString());
        binding.put("releaseFingerprint", releaseFingerprint);
        binding.put("releaseGeneration", releaseGeneration);
        binding.put("itemId", itemId.toString());
        binding.put("itemSequence", sequence);
        binding.put("itemFingerprint", authoritativeItemFingerprint);
        binding.put("campaignFence", fence);
        binding.put("targetId", targetId);
        binding.put("phase14PlanId", authoritativeP14 == null ? null : authoritativeP14.toString());
        binding.put("phase14PlanFingerprint", authoritativeP14Fp);
        binding.put("phase15ExecutionId", authoritativeP15 == null ? null : authoritativeP15.toString());
        binding.put("phase15ExecutionFingerprint", authoritativeP15Fp);
        binding.put("cellId", cellId);
        binding.put("parameter", "txPower");
        binding.put("expected", CampaignHandoffIdFactory.canonicalDecimal(expected));
        binding.put("desired", CampaignHandoffIdFactory.canonicalDecimal(desired));
        binding.put("rollback", CampaignHandoffIdFactory.canonicalDecimal(rollback));
        binding.put("unit", "dBm");
        String digest = handoffIdFactory.bindingDigest(binding);
        String handoffId = handoffIdFactory.create(campaignId, revisionNumber, itemId, digest);
        String existing = jdbc.query(
                """
                SELECT campaign_handoff_id, binding_digest FROM campaign_execution_handoff
                 WHERE item_id = :itemId AND lineage_type = 'FORWARD'
                """,
                new MapSqlParameterSource("itemId", itemId),
                rs -> rs.next() ? rs.getString("campaign_handoff_id") + "|" + rs.getString("binding_digest") : null
        );
        if (existing != null) {
            String existingId = existing.substring(0, existing.indexOf('|'));
            String existingDigest = existing.substring(existing.indexOf('|') + 1);
            if (!digest.equals(existingDigest) || !handoffId.equals(existingId)) {
                throw new CampaignException(
                        ProductionReasonCode.HANDOFF_IDEMPOTENCY_CONFLICT,
                        "same CampaignHandoffId with different digest/identity"
                );
            }
            return existingId;
        }
        UUID bindingId = UUID.randomUUID();
        slotService.acquireForward(campaignId, revisionId, itemId, fence, controlGeneration);
        try {
            jdbc.update(
                    """
                    INSERT INTO campaign_execution_binding (
                        binding_id, campaign_id, revision_id, revision_number, campaign_fingerprint,
                        control_generation, cohort_id, release_fingerprint, release_generation,
                        item_id, item_sequence, item_fingerprint, campaign_fence, production_target_id,
                        phase14_plan_id, phase14_plan_fingerprint, phase15_execution_id, phase15_execution_fingerprint,
                        cell_id, parameter, expected_value, desired_value, rollback_value, unit,
                        binding_digest, created_at)
                    VALUES (
                        :bindingId, :campaignId, :revisionId, :revisionNumber, :campaignFingerprint,
                        :controlGeneration, :cohortId, :releaseFingerprint, :releaseGeneration,
                        :itemId, :itemSequence, :itemFingerprint, :fence, :targetId,
                        :p14, :p14fp, :p15, :p15fp,
                        :cellId, 'txPower', :expected, :desired, :rollback, 'dBm',
                        :digest, :now)
                    """,
                    new MapSqlParameterSource()
                            .addValue("bindingId", bindingId)
                            .addValue("campaignId", campaignId)
                            .addValue("revisionId", revisionId)
                            .addValue("revisionNumber", revisionNumber)
                            .addValue("campaignFingerprint", campaignFingerprint)
                            .addValue("controlGeneration", controlGeneration)
                            .addValue("cohortId", cohortId)
                            .addValue("releaseFingerprint", releaseFingerprint)
                            .addValue("releaseGeneration", releaseGeneration)
                            .addValue("itemId", itemId)
                            .addValue("itemSequence", sequence)
                            .addValue("itemFingerprint", authoritativeItemFingerprint)
                            .addValue("fence", fence)
                            .addValue("targetId", targetId)
                            .addValue("p14", authoritativeP14)
                            .addValue("p14fp", authoritativeP14Fp)
                            .addValue("p15", authoritativeP15)
                            .addValue("p15fp", authoritativeP15Fp)
                            .addValue("cellId", cellId)
                            .addValue("expected", expected)
                            .addValue("desired", desired)
                            .addValue("rollback", rollback)
                            .addValue("digest", digest)
                            .addValue("now", Timestamp.from(Instant.now()))
            );
            jdbc.update(
                    """
                    INSERT INTO campaign_execution_handoff
                        (campaign_handoff_id, campaign_id, revision_id, item_id, binding_id, binding_digest, lineage_type, created_at)
                    VALUES (:id, :campaignId, :revisionId, :itemId, :bindingId, :digest, 'FORWARD', :now)
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", handoffId)
                            .addValue("campaignId", campaignId)
                            .addValue("revisionId", revisionId)
                            .addValue("itemId", itemId)
                            .addValue("bindingId", bindingId)
                            .addValue("digest", digest)
                            .addValue("now", Timestamp.from(Instant.now()))
            );
            auditService.append(campaignId, "ITEM_HANDOFF_PENDING", "SYSTEM", Map.of(
                    "itemId", itemId.toString(),
                    "campaignHandoffId", handoffId
            ));
            return handoffId;
        } catch (DuplicateKeyException ex) {
            return jdbc.queryForObject(
                    """
                    SELECT campaign_handoff_id FROM campaign_execution_handoff
                     WHERE item_id = :itemId AND lineage_type = 'FORWARD'
                    """,
                    new MapSqlParameterSource("itemId", itemId),
                    String.class
            );
        }
    }

    /**
     * Recovery lineage reuses the immutable CampaignExecutionBinding and a distinct
     * CampaignHandoffId. Requires ForwardProgressionClosure CLOSED. No grant mint.
     */
    @Transactional
    public String createOrReturnRecoveryHandoff(UUID campaignId, UUID itemId) {
        closureService.assertClosed(campaignId);
        String existing = jdbc.query(
                """
                SELECT campaign_handoff_id FROM campaign_execution_handoff
                 WHERE item_id = :itemId AND lineage_type = 'RECOVERY'
                """,
                new MapSqlParameterSource("itemId", itemId),
                rs -> rs.next() ? rs.getString(1) : null
        );
        if (existing != null) {
            return existing;
        }
        Map<String, Object> binding = jdbc.queryForMap(
                """
                SELECT b.binding_id, b.binding_digest, b.revision_number, h.campaign_handoff_id AS forward_handoff
                  FROM campaign_execution_binding b
                  JOIN campaign_execution_handoff h ON h.binding_id = b.binding_id AND h.lineage_type = 'FORWARD'
                 WHERE b.item_id = :itemId AND b.campaign_id = :campaignId
                """,
                new MapSqlParameterSource().addValue("itemId", itemId).addValue("campaignId", campaignId)
        );
        Map<String, Object> material = new LinkedHashMap<>();
        material.put("campaignId", campaignId.toString());
        material.put("campaignRevision", String.valueOf(((Number) binding.get("revision_number")).intValue()));
        material.put("itemId", itemId.toString());
        material.put("bindingDigest", String.valueOf(binding.get("binding_digest")).toLowerCase());
        material.put("lineageType", "RECOVERY");
        String recoveryHandoffId = com.simba.snip.npo.productionchange.protocol.Sha256Hex.hash(
                com.simba.snip.npo.productionchange.protocol.CanonicalJson.serialize(material)
        );
        jdbc.update(
                """
                INSERT INTO campaign_execution_handoff
                    (campaign_handoff_id, campaign_id, revision_id, item_id, binding_id, binding_digest, lineage_type, created_at)
                SELECT :id, campaign_id, revision_id, item_id, binding_id, binding_digest, 'RECOVERY', :now
                  FROM campaign_execution_binding
                 WHERE item_id = :itemId
                """,
                new MapSqlParameterSource()
                        .addValue("id", recoveryHandoffId)
                        .addValue("itemId", itemId)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
        auditService.append(campaignId, "RECOVERY_HANDOFF_PENDING", "SYSTEM", Map.of(
                "itemId", itemId.toString(),
                "campaignHandoffId", recoveryHandoffId
        ));
        return recoveryHandoffId;
    }

    public void denyStandaloneDowngrade(String executionOrigin) {
        if (CampaignConstantsOrigin.isCampaign(executionOrigin)) {
            throw new CampaignException(
                    ProductionReasonCode.STANDALONE_DOWNGRADE_DENIED,
                    "campaign-bound production change cannot execute as standalone"
            );
        }
    }

    private static final class CampaignConstantsOrigin {
        private static boolean isCampaign(String origin) {
            return "PRODUCTION_CAMPAIGN".equals(origin);
        }
    }
}
