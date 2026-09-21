package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CampaignMutationSlotService {

    static final String LOCK_CAMPAIGN_SQL = """
            SELECT campaign_id FROM production_change_campaign
             WHERE campaign_id = :campaignId
               FOR UPDATE
            """;

    static final String LOCK_SQL = """
            SELECT state, holder_type, holder_item_id, reservation_id,
                   campaign_fencing_token, campaign_control_generation, version
              FROM campaign_mutation_slot
             WHERE campaign_id = :campaignId
               FOR UPDATE
            """;

    static final String ACQUIRE_SQL = """
            UPDATE campaign_mutation_slot
               SET state = 'ACQUIRED',
                   holder_type = :holderType,
                   holder_item_id = :itemId,
                   reservation_id = :reservationId,
                   campaign_fencing_token = :fence,
                   campaign_control_generation = :controlGeneration,
                   acquired_at = :acquiredAt,
                   version = version + 1
             WHERE campaign_id = :campaignId
               AND state = 'EMPTY'
               AND holder_type = 'NONE'
            """;

    static final String HOLD_MAY_HAVE_SENT_SQL = """
            UPDATE campaign_mutation_slot
               SET state = 'HELD_MAY_HAVE_SENT',
                   version = version + 1
             WHERE campaign_id = :campaignId
               AND state = 'ACQUIRED'
               AND holder_type = :holderType
            """;

    static final String RELEASE_SERIALIZATION_SQL = """
            UPDATE campaign_mutation_slot
               SET state = 'EMPTY',
                   holder_type = 'NONE',
                   holder_item_id = NULL,
                   reservation_id = NULL,
                   acquired_at = NULL,
                   version = version + 1
             WHERE campaign_id = :campaignId
               AND state = 'HELD_MAY_HAVE_SENT'
               AND holder_type = :holderType
            """;

    static final String REBIND_RECOVERY_SQL = """
            UPDATE campaign_mutation_slot
               SET holder_type = 'RECOVERY',
                   reservation_id = :reservationId,
                   campaign_fencing_token = :fence,
                   campaign_control_generation = :controlGeneration,
                   version = version + 1
             WHERE campaign_id = :campaignId
               AND holder_type = 'FORWARD'
               AND state IN ('ACQUIRED', 'HELD_MAY_HAVE_SENT')
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final CampaignSafetyService safetyService;
    private final ForwardProgressionClosureService closureService;

    public CampaignMutationSlotService(
            NamedParameterJdbcTemplate jdbc,
            CampaignSafetyService safetyService,
            ForwardProgressionClosureService closureService
    ) {
        this.jdbc = jdbc;
        this.safetyService = safetyService;
        this.closureService = closureService;
    }

    /**
     * Frozen atomic reservation + slot acquisition in one local transaction.
     */
    @Transactional
    public UUID acquireForward(
            UUID campaignId, UUID revisionId, UUID itemId, long fence, long controlGeneration
    ) {
        closureService.assertOpen(campaignId);
        lockCampaignThenSlot(campaignId);
        Long liveControl = jdbc.queryForObject(
                """
                SELECT current_generation FROM campaign_control_generation
                 WHERE campaign_id = :campaignId FOR UPDATE
                """,
                new MapSqlParameterSource("campaignId", campaignId),
                Long.class
        );
        if (liveControl == null || liveControl != controlGeneration) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_STALE,
                    "control generation is stale at acquisition"
            );
        }
        UUID reservationId = safetyService.reserve(campaignId, revisionId, itemId, "FORWARD");
        acquireLocked(campaignId, "FORWARD", itemId, reservationId, fence, controlGeneration);
        return reservationId;
    }

    @Transactional
    public UUID acquireRecovery(
            UUID campaignId, UUID revisionId, UUID itemId, long fence, long controlGeneration
    ) {
        closureService.assertClosed(campaignId);
        lockCampaignThenSlot(campaignId);
        UUID reservationId = safetyService.reserve(campaignId, revisionId, itemId, "RECOVERY");
        acquireLocked(campaignId, "RECOVERY", itemId, reservationId, fence, controlGeneration);
        return reservationId;
    }

    @Transactional
    public void acquire(UUID campaignId, String holderType, UUID itemId, UUID reservationId, long fence, long controlGeneration) {
        try {
            if ("RECOVERY".equals(holderType)) {
                closureService.assertClosed(campaignId);
            } else if ("FORWARD".equals(holderType)) {
                closureService.assertOpen(campaignId);
            }
            lockCampaignThenSlot(campaignId);
            acquireLocked(campaignId, holderType, itemId, reservationId, fence, controlGeneration);
        } catch (CampaignException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE,
                    "campaign mutation slot unavailable or lock timed out",
                    ex
            );
        }
    }

    private void acquireLocked(
            UUID campaignId, String holderType, UUID itemId, UUID reservationId, long fence, long controlGeneration
    ) {
        var row = jdbc.queryForMap(LOCK_SQL, new MapSqlParameterSource("campaignId", campaignId));
        String state = String.valueOf(row.get("state"));
        String holder = String.valueOf(row.get("holder_type"));
        if (!"EMPTY".equals(state) || !"NONE".equals(holder)) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE,
                    "combined campaign mutation slot is not empty"
            );
        }
        int updated = jdbc.update(
                ACQUIRE_SQL,
                new MapSqlParameterSource()
                        .addValue("campaignId", campaignId)
                        .addValue("holderType", holderType)
                        .addValue("itemId", itemId)
                        .addValue("reservationId", reservationId)
                        .addValue("fence", fence)
                        .addValue("controlGeneration", controlGeneration)
                        .addValue("acquiredAt", Timestamp.from(Instant.now()))
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE,
                    "concurrent combined mutation-slot acquisition lost"
            );
        }
    }

    @Transactional
    public void markMayHaveSent(UUID campaignId, String holderType) {
        lockCampaignThenSlot(campaignId);
        int updated = jdbc.update(
                HOLD_MAY_HAVE_SENT_SQL,
                new MapSqlParameterSource()
                        .addValue("campaignId", campaignId)
                        .addValue("holderType", holderType)
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_STALE,
                    "MAY_HAVE_SENT cannot be recorded against current slot state"
            );
        }
    }

    /**
     * CampaignMutationSerializationRelease. Distinct from ExternalMutationOutcomeResolution.
     * Predicates are read from locked durable rows; caller booleans are not authoritative.
     */
    @Transactional
    public void releaseSerialization(UUID campaignId, String holderType) {
        lockCampaignThenSlot(campaignId);
        var slot = jdbc.queryForMap(LOCK_SQL, new MapSqlParameterSource("campaignId", campaignId));
        if (!"HELD_MAY_HAVE_SENT".equals(String.valueOf(slot.get("state")))
                || !holderType.equals(String.valueOf(slot.get("holder_type")))) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE,
                    "serialization release requires HELD_MAY_HAVE_SENT holder " + holderType
            );
        }
        UUID itemId = (UUID) slot.get("holder_item_id");
        assertDurableReleasePredicates(campaignId, itemId, holderType);
        int updated = jdbc.update(
                RELEASE_SERIALIZATION_SQL,
                new MapSqlParameterSource()
                        .addValue("campaignId", campaignId)
                        .addValue("holderType", holderType)
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE,
                    "serialization capacity was not released"
            );
        }
    }

    private void assertDurableReleasePredicates(UUID campaignId, UUID itemId, String holderType) {
        String closure = jdbc.queryForObject(
                "SELECT state FROM campaign_forward_progression_closure WHERE campaign_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", campaignId),
                String.class
        );
        if ("FORWARD".equals(holderType) && !"OPEN".equals(closure)) {
            throw new CampaignException(
                    ProductionReasonCode.FORWARD_PROGRESSION_CLOSED,
                    "forward serialization release requires OPEN closure"
            );
        }
        String itemState = itemId == null ? null : jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", itemId),
                String.class
        );
        if (!"COMPLETED".equals(itemState)) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE,
                    "item is not terminally accountable for serialization release"
            );
        }
        Integer unresolved = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_execution_item
                 WHERE campaign_id = :id AND state IN ('OUTCOME_UNRESOLVED', 'RECOVERY_REQUIRED', 'MANUAL_INTERVENTION_REQUIRED')
                """,
                new MapSqlParameterSource("id", campaignId),
                Integer.class
        );
        if (unresolved != null && unresolved > 0) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_OUTCOME_UNRESOLVED,
                    "unresolved or recovery item blocks serialization release"
            );
        }
        Map<String, Object> observation;
        try {
            observation = jdbc.queryForMap(
                    """
                    SELECT network_observation_health, operational_safety_health, policy_versions
                      FROM campaign_observation_boundary
                     WHERE item_id = :itemId
                     ORDER BY created_at DESC
                     LIMIT 1
                     FOR UPDATE
                    """,
                    new MapSqlParameterSource("itemId", itemId)
            );
        } catch (DataAccessException ex) {
            throw new CampaignException(
                    ProductionReasonCode.OBSERVATION_NOT_HEALTHY,
                    "current observation boundary is required for serialization release",
                    ex
            );
        }
        if (!"HEALTHY".equals(String.valueOf(observation.get("network_observation_health")))) {
            throw new CampaignException(ProductionReasonCode.OBSERVATION_NOT_HEALTHY, "NetworkObservationHealth is not HEALTHY");
        }
        if (!"SAFE".equals(String.valueOf(observation.get("operational_safety_health")))) {
            throw new CampaignException(ProductionReasonCode.OBSERVATION_NOT_SAFE, "OperationalSafetyHealth is not SAFE");
        }
        Integer interference = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_external_interference
                 WHERE campaign_id = :id AND item_id = :itemId
                """,
                new MapSqlParameterSource().addValue("id", campaignId).addValue("itemId", itemId),
                Integer.class
        );
        if (interference != null && interference > 0) {
            throw new CampaignException(
                    ProductionReasonCode.EXTERNAL_INTERFERENCE_DETECTED,
                    "unresolved interference blocks serialization release"
            );
        }
    }

    @Transactional
    public void rebindHolderToRecovery(UUID campaignId, UUID reservationId, long fence, long controlGeneration) {
        lockCampaignThenSlot(campaignId);
        closureService.assertClosed(campaignId);
        int updated = jdbc.update(
                REBIND_RECOVERY_SQL,
                new MapSqlParameterSource()
                        .addValue("campaignId", campaignId)
                        .addValue("reservationId", reservationId)
                        .addValue("fence", fence)
                        .addValue("controlGeneration", controlGeneration)
        );
        if (updated != 1) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE,
                    "recovery holder rebind failed"
            );
        }
    }

    @Transactional(readOnly = true)
    public SlotSnapshot snapshot(UUID campaignId) {
        var row = jdbc.queryForMap(LOCK_SQL.replace(" FOR UPDATE", ""), new MapSqlParameterSource("campaignId", campaignId));
        return new SlotSnapshot(
                String.valueOf(row.get("state")),
                String.valueOf(row.get("holder_type")),
                toLong(row.get("campaign_fencing_token")),
                toLong(row.get("campaign_control_generation")),
                (UUID) row.get("holder_item_id"),
                (UUID) row.get("reservation_id")
        );
    }

    public int activeMutations(UUID campaignId) {
        SlotSnapshot snap = snapshot(campaignId);
        if ("EMPTY".equals(snap.state()) || "NONE".equals(snap.holderType())) {
            return 0;
        }
        return 1;
    }

    @Transactional
    public void assertCurrentFenceAndGeneration(UUID campaignId, long fence, long controlGeneration) {
        Long currentControl = jdbc.queryForObject(
                """
                SELECT current_generation FROM campaign_control_generation
                 WHERE campaign_id = :campaignId
                   FOR UPDATE
                """,
                new MapSqlParameterSource("campaignId", campaignId),
                Long.class
        );
        SlotSnapshot snap = snapshot(campaignId);
        if (currentControl == null
                || currentControl != controlGeneration
                || snap.controlGeneration() != currentControl
                || snap.fence() != fence) {
            throw new CampaignException(
                    ProductionReasonCode.MUTATION_SLOT_STALE,
                    "slot fence or control generation is stale"
            );
        }
    }

    private void lockCampaignThenSlot(UUID campaignId) {
        jdbc.queryForObject(LOCK_CAMPAIGN_SQL, new MapSqlParameterSource("campaignId", campaignId), UUID.class);
        jdbc.queryForMap(LOCK_SQL, new MapSqlParameterSource("campaignId", campaignId));
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    public record SlotSnapshot(
            String state, String holderType, long fence, long controlGeneration, UUID holderItemId, UUID reservationId
    ) {
        public String holderTypeNormalized() {
            return holderType == null ? "NONE" : holderType.toUpperCase(Locale.ROOT);
        }
    }
}
