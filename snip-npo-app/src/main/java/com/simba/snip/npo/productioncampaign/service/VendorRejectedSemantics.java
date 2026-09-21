package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class VendorRejectedSemantics {

    private final NamedParameterJdbcTemplate jdbc;

    public VendorRejectedSemantics() {
        this.jdbc = null;
    }

    @Autowired
    public VendorRejectedSemantics(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void apply(boolean mayHaveSentAlready, boolean authoritativePreSendRejection) {
        if (!mayHaveSentAlready && authoritativePreSendRejection) {
            return;
        }
        if (mayHaveSentAlready) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_OUTCOME_UNRESOLVED,
                    "VENDOR_REJECTED after MAY_HAVE_SENT cannot manufacture NOT_SENT, return exposure, or release serialization"
            );
        }
        throw new CampaignException(
                ProductionReasonCode.PRODUCTION_VENDOR_REJECTION,
                "vendor rejection without authoritative no-send proof remains unresolved"
        );
    }

    @Transactional
    public void applyDurable(UUID campaignId, UUID itemId, boolean authoritativePreSendRejection) {
        Map<String, Object> item = jdbc.queryForMap(
                "SELECT state FROM campaign_execution_item WHERE item_id = :id",
                new MapSqlParameterSource("id", itemId)
        );
        Map<String, Object> slot = jdbc.queryForMap(
                "SELECT state, holder_type FROM campaign_mutation_slot WHERE campaign_id = :id",
                new MapSqlParameterSource("id", campaignId)
        );
        String itemState = String.valueOf(item.get("state"));
        String slotState = String.valueOf(slot.get("state"));
        boolean mayHaveSent = "MAY_HAVE_SENT".equals(itemState)
                || "HELD_MAY_HAVE_SENT".equals(slotState)
                || "VENDOR_ACCEPTED".equals(itemState)
                || "VERIFYING".equals(itemState);
        if (mayHaveSent) {
            apply(true, authoritativePreSendRejection);
        }
        apply(false, authoritativePreSendRejection);
        if (authoritativePreSendRejection) {
            String from = itemState;
            String trigger = "PRE_SEND".equals(from)
                    ? "AUTHORITATIVE_PRE_SEND_REJECTION"
                    : "HANDED_OFF".equals(from)
                    ? "AUTHORITATIVE_PRE_SEND_REJECTION"
                    : "AUTHORITATIVE_NO_SEND";
            String to = CampaignItemLifecycleService.destinationFor(from, trigger);
            if (to == null) {
                throw new CampaignException(
                        ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                        "authoritative pre-send rejection did not match a no-send item state"
                );
            }
            int updated = jdbc.update(
                    """
                    UPDATE campaign_execution_item
                       SET state = :to, version = version + 1
                     WHERE item_id = :id AND state = :from
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", itemId)
                            .addValue("from", from)
                            .addValue("to", to)
            );
            if (updated != 1) {
                throw new CampaignException(
                        ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                        "authoritative pre-send rejection did not match a no-send item state"
                );
            }
        }
    }
}
