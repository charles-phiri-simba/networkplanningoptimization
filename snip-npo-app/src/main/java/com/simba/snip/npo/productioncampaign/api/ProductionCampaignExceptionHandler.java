package com.simba.snip.npo.productioncampaign.api;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ProductionCampaignController.class)
public class ProductionCampaignExceptionHandler {

    @ExceptionHandler(CampaignException.class)
    public ResponseEntity<Map<String, String>> campaign(CampaignException ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("reasonCode", ex.reasonCode().name());
        return ResponseEntity.status(status(ex.reasonCode())).body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> unknownProperty(Exception ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", "request contains unknown or invalid fields");
        body.put("reasonCode", ProductionReasonCode.PRODUCTION_INVALID_REQUEST.name());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private static HttpStatus status(ProductionReasonCode reasonCode) {
        return switch (reasonCode) {
            case PRODUCTION_UNAUTHORIZED, PRODUCTION_SOD_VIOLATION, UNTRUSTED_ACTOR_SOURCE,
                 UNAUTHENTICATED_HUMAN_ACTOR, SYSTEM_TRANSITION_DENIED -> HttpStatus.FORBIDDEN;
            case CAMPAIGN_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case MUTATION_SLOT_UNAVAILABLE, CAMPAIGN_LEASE_UNAVAILABLE, GOVERNANCE_IDEMPOTENCY_CONFLICT,
                 HANDOFF_IDEMPOTENCY_CONFLICT, DUPLICATE_CAMPAIGN_LINEAGE -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
