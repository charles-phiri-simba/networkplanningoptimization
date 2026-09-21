package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Campaign-originated gateway preflight. Caller JSON cannot supply campaign
 * authority. Context is derived from durable CampaignHandoffId / production change.
 */
@Service
public class CampaignOriginGatewayPreflightService {

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignOriginGatewayPreflightService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void validate(ProductionNetworkChangeEntity change, UUID grantId) {
        String origin = change.getExecutionOrigin();
        if (origin == null || origin.isBlank()) {
            throw deny(ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, grantId, change);
        }
        if ("STANDALONE".equals(origin)) {
            if (change.getCampaignHandoffId() != null) {
                throw deny(ProductionReasonCode.STANDALONE_DOWNGRADE_DENIED, grantId, change);
            }
            return;
        }
        if (!"PRODUCTION_CAMPAIGN".equals(origin)) {
            throw deny(ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, grantId, change);
        }
        String handoffId = change.getCampaignHandoffId();
        if (handoffId == null || handoffId.isBlank()) {
            throw deny(ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH, grantId, change);
        }
        Map<String, Object> row;
        try {
            row = jdbc.queryForMap(
                    """
                    SELECT h.campaign_id, h.item_id, h.binding_digest, h.lineage_type,
                           b.cell_id, b.parameter, b.expected_value, b.desired_value, b.rollback_value, b.unit,
                           b.control_generation, b.release_generation, b.campaign_fence, b.campaign_fingerprint,
                           b.cohort_id, b.item_fingerprint, c.state AS campaign_state, c.abort_state, c.enabled,
                           c.fingerprint AS live_fingerprint, c.production_target_id,
                           cg.current_generation AS current_control,
                           rg.current_generation AS current_release,
                           l.fencing_token, l.status AS lease_status, l.expires_at,
                           cl.state AS closure_state,
                           s.state AS slot_state, s.holder_type, s.holder_item_id, s.reservation_id,
                           s.campaign_fencing_token AS slot_fence, s.campaign_control_generation AS slot_control,
                           i.state AS item_state, i.item_fingerprint AS live_item_fingerprint,
                           c.current_revision_number AS live_revision,
                           b.revision_number AS bound_revision,
                           rel.state AS release_state, rel.release_fingerprint AS live_release_fingerprint,
                           b.release_fingerprint AS bound_release_fingerprint
                      FROM campaign_execution_handoff h
                      JOIN campaign_execution_binding b ON b.binding_id = h.binding_id
                      JOIN production_change_campaign c ON c.campaign_id = h.campaign_id
                      JOIN campaign_control_generation cg ON cg.campaign_id = c.campaign_id
                      JOIN campaign_release_generation rg ON rg.campaign_id = c.campaign_id
                      JOIN campaign_lease l ON l.campaign_id = c.campaign_id
                      JOIN campaign_forward_progression_closure cl ON cl.campaign_id = c.campaign_id
                      JOIN campaign_mutation_slot s ON s.campaign_id = c.campaign_id
                      JOIN campaign_execution_item i ON i.item_id = h.item_id
                      LEFT JOIN campaign_cohort_release rel
                        ON rel.cohort_id = b.cohort_id AND rel.release_generation = b.release_generation
                     WHERE h.campaign_handoff_id = :id
                    """,
                    new MapSqlParameterSource("id", handoffId)
            );
        } catch (DataAccessException ex) {
            throw deny(ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH, grantId, change);
        }
        if (!change.getCellId().equals(String.valueOf(row.get("cell_id")))
                || !change.getParameter().equals(String.valueOf(row.get("parameter")))
                || compare(change.getExpectedValue(), row.get("expected_value")) != 0
                || compare(change.getDesiredValue(), row.get("desired_value")) != 0
                || compare(change.getRollbackExpectedValue() != null
                        ? change.getRollbackExpectedValue()
                        : change.getRollbackDesiredValue(), row.get("rollback_value")) != 0
                || !"dBm".equals(String.valueOf(row.get("unit")))) {
            throw deny(ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH, grantId, change);
        }
        if (!Boolean.TRUE.equals(row.get("enabled"))) {
            throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
        }
        String campaignState = String.valueOf(row.get("campaign_state"));
        if ("ABORTED".equals(campaignState)
                || "STALE".equals(campaignState)
                || "SUSPENDED".equals(campaignState)
                || "EXPIRED".equals(campaignState)
                || "ABORTED".equals(String.valueOf(row.get("abort_state")))) {
            throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
        }
        if (!String.valueOf(row.get("campaign_fingerprint")).equals(String.valueOf(row.get("live_fingerprint")))) {
            throw deny(ProductionReasonCode.PRODUCTION_FINGERPRINT_MISMATCH, grantId, change);
        }
        String lineage = String.valueOf(row.get("lineage_type"));
        String closure = String.valueOf(row.get("closure_state"));
        if ("FORWARD".equals(lineage) && !"OPEN".equals(closure)) {
            throw deny(ProductionReasonCode.FORWARD_PROGRESSION_CLOSED, grantId, change);
        }
        if ("RECOVERY".equals(lineage) && !"CLOSED".equals(closure)) {
            throw deny(ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE, grantId, change);
        }
        if (!"ACQUIRED".equals(String.valueOf(row.get("slot_state")))
                && !"HELD_MAY_HAVE_SENT".equals(String.valueOf(row.get("slot_state")))) {
            throw deny(ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE, grantId, change);
        }
        String expectedHolder = "FORWARD".equals(lineage) ? "FORWARD" : "RECOVERY";
        if (!expectedHolder.equals(String.valueOf(row.get("holder_type")))) {
            throw deny(ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE, grantId, change);
        }
        UUID holderItem = (UUID) row.get("holder_item_id");
        UUID boundItem = (UUID) row.get("item_id");
        if (holderItem == null || !holderItem.equals(boundItem)) {
            throw deny(ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE, grantId, change);
        }
        if (row.get("reservation_id") == null) {
            throw deny(ProductionReasonCode.SAFETY_RESERVATION_STALE, grantId, change);
        }
        Integer reservationOk = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_safety_budget_reservation
                 WHERE reservation_id = :id AND state = 'RESERVED' AND campaign_id = :campaignId
                """,
                new MapSqlParameterSource()
                        .addValue("id", row.get("reservation_id"))
                        .addValue("campaignId", row.get("campaign_id")),
                Integer.class
        );
        if (reservationOk == null || reservationOk != 1) {
            throw deny(ProductionReasonCode.SAFETY_RESERVATION_STALE, grantId, change);
        }
        long boundControl = ((Number) row.get("control_generation")).longValue();
        long currentControl = ((Number) row.get("current_control")).longValue();
        long boundRelease = ((Number) row.get("release_generation")).longValue();
        long currentRelease = ((Number) row.get("current_release")).longValue();
        long boundFence = ((Number) row.get("campaign_fence")).longValue();
        long currentFence = ((Number) row.get("fencing_token")).longValue();
        if (boundControl != currentControl) {
            throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
        }
        if (boundRelease != currentRelease) {
            throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
        }
        if (boundFence != currentFence) {
            throw deny(ProductionReasonCode.CAMPAIGN_LEASE_UNAVAILABLE, grantId, change);
        }
        String leaseStatus = String.valueOf(row.get("lease_status"));
        if (!"ACTIVE".equals(leaseStatus) && !"HELD".equals(leaseStatus)) {
            throw deny(ProductionReasonCode.CAMPAIGN_LEASE_UNAVAILABLE, grantId, change);
        }
        Object expiresAt = row.get("expires_at");
        Instant expiry = toInstant(expiresAt);
        if (expiry != null && expiry.isBefore(Instant.now())) {
            throw deny(ProductionReasonCode.CAMPAIGN_LEASE_UNAVAILABLE, grantId, change);
        }
        if ("ABORTED".equals(campaignState) || "ABORTED".equals(String.valueOf(row.get("abort_state")))) {
            throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
        }
        Object cohortId = row.get("cohort_id");
        if (cohortId != null) {
            Integer cohortKilled = jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM campaign_cohort
                     WHERE cohort_id = :id AND state IN ('STALE', 'SUSPENDED', 'BLOCKED')
                    """,
                    new MapSqlParameterSource("id", cohortId),
                    Integer.class
            );
            if (cohortKilled != null && cohortKilled > 0) {
                throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
            }
        }
        Integer targetKilled = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM production_network_target
                 WHERE target_id = :id AND (enabled = FALSE OR target_state IN ('SUSPENDED', 'DISABLED'))
                """,
                new MapSqlParameterSource("id", String.valueOf(row.get("production_target_id"))),
                Integer.class
        );
        if (targetKilled != null && targetKilled > 0) {
            throw deny(ProductionReasonCode.PRODUCTION_TARGET_DISABLED, grantId, change);
        }
        if (row.get("slot_control") != null && ((Number) row.get("slot_control")).longValue() != currentControl) {
            throw deny(ProductionReasonCode.MUTATION_SLOT_STALE, grantId, change);
        }
        if (row.get("slot_fence") != null && ((Number) row.get("slot_fence")).longValue() != currentFence) {
            throw deny(ProductionReasonCode.MUTATION_SLOT_STALE, grantId, change);
        }
        if (!"ACTIVE".equals(String.valueOf(row.get("release_state")))) {
            throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
        }
        if (!String.valueOf(row.get("bound_release_fingerprint"))
                .equals(String.valueOf(row.get("live_release_fingerprint")))) {
            throw deny(ProductionReasonCode.PRODUCTION_FINGERPRINT_MISMATCH, grantId, change);
        }
        if (row.get("live_revision") == null
                || row.get("bound_revision") == null
                || ((Number) row.get("live_revision")).intValue() != ((Number) row.get("bound_revision")).intValue()) {
            throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
        }
        if (!String.valueOf(row.get("item_fingerprint")).equals(String.valueOf(row.get("live_item_fingerprint")))) {
            throw deny(ProductionReasonCode.PRODUCTION_FINGERPRINT_MISMATCH, grantId, change);
        }
        String itemState = String.valueOf(row.get("item_state"));
        if (!java.util.Set.of("HANDED_OFF", "PRE_SEND").contains(itemState)) {
            throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
        }
        assertP17Current(String.valueOf(row.get("production_target_id")), grantId, change);
        if ("HANDED_OFF".equals(itemState)) {
            jdbc.update(
                    """
                    UPDATE campaign_execution_item
                       SET state = 'PRE_SEND', updated_at = NOW(), version = version + 1
                     WHERE item_id = :id AND state = 'HANDED_OFF'
                    """,
                    new MapSqlParameterSource("id", row.get("item_id"))
            );
        }
    }

    public void recordMayHaveSent(ProductionNetworkChangeEntity change) {
        if (change == null || !"PRODUCTION_CAMPAIGN".equals(change.getExecutionOrigin())) {
            return;
        }
        String handoffId = change.getCampaignHandoffId();
        if (handoffId == null) {
            return;
        }
        Map<String, Object> ids = jdbc.queryForMap(
                """
                SELECT h.item_id, h.campaign_id, i.revision_id, c.current_revision_number, i.cell_id, s.reservation_id
                  FROM campaign_execution_handoff h
                  JOIN campaign_execution_item i ON i.item_id = h.item_id
                  JOIN production_change_campaign c ON c.campaign_id = h.campaign_id
                  JOIN campaign_mutation_slot s ON s.campaign_id = h.campaign_id
                 WHERE h.campaign_handoff_id = :id
                """,
                new MapSqlParameterSource("id", handoffId)
        );
        jdbc.update(
                """
                UPDATE campaign_execution_item
                   SET state = 'MAY_HAVE_SENT', updated_at = NOW(), version = version + 1
                 WHERE item_id = :id AND state = 'PRE_SEND'
                """,
                new MapSqlParameterSource("id", ids.get("item_id"))
        );
        jdbc.update(
                """
                UPDATE campaign_mutation_slot
                   SET state = 'HELD_MAY_HAVE_SENT', version = version + 1
                 WHERE campaign_id = :id AND state = 'ACQUIRED'
                """,
                new MapSqlParameterSource("id", ids.get("campaign_id"))
        );
        jdbc.update(
                """
                UPDATE campaign_safety_budget_reservation
                   SET state = 'CONSUMED', updated_at = NOW(), version = version + 1
                 WHERE reservation_id = :id AND state = 'RESERVED'
                """,
                new MapSqlParameterSource("id", ids.get("reservation_id"))
        );
        jdbc.update(
                """
                INSERT INTO campaign_exposed_cell (campaign_id, revision_number, cell_id, first_accounted_at)
                VALUES (:campaignId, :rev, :cellId, NOW())
                ON CONFLICT (campaign_id, revision_number, cell_id) DO NOTHING
                """,
                new MapSqlParameterSource()
                        .addValue("campaignId", ids.get("campaign_id"))
                        .addValue("rev", ((Number) ids.get("current_revision_number")).intValue())
                        .addValue("cellId", ids.get("cell_id"))
        );
    }

    public void denyStandaloneDowngrade(ProductionNetworkChangeEntity change, UUID grantId, boolean treatAsStandalone) {
        if (treatAsStandalone && "PRODUCTION_CAMPAIGN".equals(change.getExecutionOrigin())) {
            throw deny(ProductionReasonCode.STANDALONE_DOWNGRADE_DENIED, grantId, change);
        }
    }

    private void assertP17Current(String targetId, UUID grantId, ProductionNetworkChangeEntity change) {
        try {
            Integer approved = jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM production_target_onboarding
                     WHERE production_target_id = :target
                       AND status = 'APPROVED'
                    """,
                    new MapSqlParameterSource("target", targetId),
                    Integer.class
            );
            if (approved == null || approved < 1) {
                throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
            }
            Integer revoked = jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM production_target_onboarding
                     WHERE production_target_id = :target
                       AND status IN ('REVOKED', 'SUSPENDED', 'INVALID')
                    """,
                    new MapSqlParameterSource("target", targetId),
                    Integer.class
            );
            if (revoked != null && revoked > 0) {
                throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
            }
        } catch (GatewayDeniedException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw deny(ProductionReasonCode.CAMPAIGN_STALE, grantId, change);
        }
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (value instanceof java.time.LocalDateTime ldt) {
            return ldt.toInstant(java.time.ZoneOffset.UTC);
        }
        return Instant.parse(value.toString());
    }

    private static GatewayDeniedException deny(
            ProductionReasonCode reason, UUID grantId, ProductionNetworkChangeEntity change
    ) {
        return GatewayDeniedException.deny(reason, grantId, change.getProductionChangeId());
    }

    private static int compare(BigDecimal left, Object right) {
        if (left == null || right == null) {
            return -1;
        }
        BigDecimal other = right instanceof BigDecimal bd ? bd : new BigDecimal(right.toString());
        return left.compareTo(other);
    }
}
