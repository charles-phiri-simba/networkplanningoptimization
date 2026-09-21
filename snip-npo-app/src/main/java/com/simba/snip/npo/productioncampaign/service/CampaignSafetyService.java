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
import java.util.UUID;

@Service
public class CampaignSafetyService {

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignSafetyService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public UUID reserve(UUID campaignId, UUID revisionId, UUID itemId, String budgetType) {
        UUID reservationId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO campaign_safety_budget_reservation
                    (reservation_id, campaign_id, revision_id, item_id, budget_type, state, created_at, updated_at, version)
                VALUES
                    (:id, :campaignId, :revisionId, :itemId, :budgetType, 'RESERVED', :now, :now, 0)
                """,
                new MapSqlParameterSource()
                        .addValue("id", reservationId)
                        .addValue("campaignId", campaignId)
                        .addValue("revisionId", revisionId)
                        .addValue("itemId", itemId)
                        .addValue("budgetType", budgetType)
                        .addValue("now", Timestamp.from(Instant.now()))
        );
        return reservationId;
    }

    /**
     * Atomic MAY_HAVE_SENT accounting: consume reservation once, increment the
     * applicable operation counter, and insert unique-cell exposure.
     */
    @Transactional
    public void consumeMayHaveSent(
            UUID campaignId,
            int revisionNumber,
            UUID reservationId,
            String budgetType,
            String cellId,
            boolean firstSendForReservation
    ) {
        try {
            int consumed = jdbc.update(
                    """
                    UPDATE campaign_safety_budget_reservation
                       SET state = 'CONSUMED', updated_at = :now, version = version + 1
                     WHERE reservation_id = :id
                       AND state = 'RESERVED'
                       AND budget_type = :budgetType
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", reservationId)
                            .addValue("budgetType", budgetType)
                            .addValue("now", Timestamp.from(Instant.now()))
            );
            if (consumed != 1) {
                if (firstSendForReservation) {
                    throw new CampaignException(
                            ProductionReasonCode.SAFETY_EXPOSURE_ALREADY_ACCOUNTED,
                            "reservation already consumed or stale"
                    );
                }
                return;
            }
            String column = "FORWARD".equals(budgetType)
                    ? "forward_mutation_exposure"
                    : "recovery_mutation_exposure";
            jdbc.update(
                    "UPDATE campaign_safety_exposure SET " + column + " = " + column + " + 1, version = version + 1 WHERE campaign_id = :campaignId",
                    new MapSqlParameterSource("campaignId", campaignId)
            );
            int inserted = jdbc.update(
                    """
                    INSERT INTO campaign_exposed_cell (campaign_id, revision_number, cell_id, first_accounted_at)
                    VALUES (:campaignId, :revisionNumber, :cellId, :now)
                    ON CONFLICT (campaign_id, revision_number, cell_id) DO NOTHING
                    """,
                    new MapSqlParameterSource()
                            .addValue("campaignId", campaignId)
                            .addValue("revisionNumber", revisionNumber)
                            .addValue("cellId", cellId)
                            .addValue("now", Timestamp.from(Instant.now()))
            );
            if (inserted == 1) {
                jdbc.update(
                        """
                        UPDATE campaign_safety_exposure
                           SET distinct_cells_exposed = (
                               SELECT COUNT(*) FROM campaign_exposed_cell
                                WHERE campaign_id = :campaignId AND revision_number = :revisionNumber
                           ),
                           version = version + 1
                         WHERE campaign_id = :campaignId
                        """,
                        new MapSqlParameterSource()
                                .addValue("campaignId", campaignId)
                                .addValue("revisionNumber", revisionNumber)
                );
            }
        } catch (CampaignException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new CampaignException(
                    ProductionReasonCode.SAFETY_RESERVATION_STALE,
                    "safety exposure consume failed closed",
                    ex
            );
        }
    }

    public int distinctCellsExposed(UUID campaignId, int revisionNumber) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM campaign_exposed_cell
                 WHERE campaign_id = :campaignId AND revision_number = :revisionNumber
                """,
                new MapSqlParameterSource()
                        .addValue("campaignId", campaignId)
                        .addValue("revisionNumber", revisionNumber),
                Integer.class
        );
        return count == null ? 0 : count;
    }
}
