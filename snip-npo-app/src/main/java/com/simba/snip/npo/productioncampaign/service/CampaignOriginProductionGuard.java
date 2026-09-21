package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Makes inherited P16 rollback/grant issuance campaign-aware when origin is
 * PRODUCTION_CAMPAIGN. Standalone P16 semantics are unchanged. Not a grant writer.
 */
@Service
public class CampaignOriginProductionGuard {

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignOriginProductionGuard(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void assertRollbackAuthorizationPermitted(ProductionNetworkChangeEntity change) {
        if (!isCampaignOrigin(change)) {
            return;
        }
        Map<String, Object> lineage = lineage(change);
        if (!"CLOSED".equals(String.valueOf(lineage.get("closure")))) {
            throw new ProductionChangeException(
                    ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE,
                    "campaign-associated rollback authorization requires ForwardProgressionClosure=CLOSED"
            );
        }
        if (!"AUTHORIZED".equals(String.valueOf(lineage.get("recovery")))) {
            throw new ProductionChangeException(
                    ProductionReasonCode.INVALID_RECOVERY_TRANSITION,
                    "campaign-associated rollback authorization requires campaign recovery AUTHORIZED"
            );
        }
    }

    public void assertGrantIssuePermitted(ProductionNetworkChangeEntity change, GrantType grantType) {
        if (!isCampaignOrigin(change)) {
            return;
        }
        if (grantType == GrantType.ROLLBACK) {
            assertRollbackAuthorizationPermitted(change);
            return;
        }
        Map<String, Object> lineage = lineage(change);
        String campaignState = String.valueOf(lineage.get("campaign_state"));
        if ("SUSPENDED".equals(campaignState)
                || "STALE".equals(campaignState)
                || "ABORTED".equals(campaignState)
                || "ABORTED".equals(String.valueOf(lineage.get("abort_state")))) {
            throw new ProductionChangeException(
                    ProductionReasonCode.CAMPAIGN_STALE,
                    "campaign-originated forward grant is denied after campaign invalidation"
            );
        }
    }

    private static boolean isCampaignOrigin(ProductionNetworkChangeEntity change) {
        return change != null && "PRODUCTION_CAMPAIGN".equals(change.getExecutionOrigin());
    }

    private Map<String, Object> lineage(ProductionNetworkChangeEntity change) {
        String handoffId = change.getCampaignHandoffId();
        try {
            if (handoffId != null && !handoffId.isBlank()) {
                return jdbc.queryForMap(
                        """
                        SELECT cl.state AS closure,
                               COALESCE(r.state, 'NOT_REQUIRED') AS recovery,
                               c.state AS campaign_state,
                               c.abort_state
                          FROM campaign_execution_handoff h
                          JOIN production_change_campaign c ON c.campaign_id = h.campaign_id
                          JOIN campaign_forward_progression_closure cl ON cl.campaign_id = c.campaign_id
                          LEFT JOIN campaign_recovery r ON r.campaign_id = c.campaign_id
                         WHERE h.campaign_handoff_id = :id
                        """,
                        new MapSqlParameterSource("id", handoffId)
                );
            }
            UUID changeId = change.getProductionChangeId();
            return jdbc.queryForMap(
                    """
                    SELECT cl.state AS closure,
                           COALESCE(r.state, 'NOT_REQUIRED') AS recovery,
                           c.state AS campaign_state,
                           c.abort_state
                      FROM campaign_execution_item i
                      JOIN production_change_campaign c ON c.campaign_id = i.campaign_id
                      JOIN campaign_forward_progression_closure cl ON cl.campaign_id = c.campaign_id
                      LEFT JOIN campaign_recovery r ON r.campaign_id = c.campaign_id
                     WHERE i.production_change_id = :id
                    """,
                    new MapSqlParameterSource("id", changeId)
            );
        } catch (Exception ex) {
            throw new ProductionChangeException(
                    ProductionReasonCode.CAMPAIGN_BINDING_MISMATCH,
                    "campaign lineage is required for campaign-originated production change",
                    ex
            );
        }
    }
}
