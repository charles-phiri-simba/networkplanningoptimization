package com.simba.snip.npo.productionchange.api;

import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {ProductionChangeController.class, ProductionTargetController.class})
public class ProductionChangeExceptionHandler {

    @ExceptionHandler(ProductionChangeException.class)
    public ResponseEntity<Map<String, String>> productionChange(ProductionChangeException ex) {
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

    private HttpStatus status(ProductionReasonCode reasonCode) {
        return switch (reasonCode) {
            case PRODUCTION_UNAUTHORIZED, PRODUCTION_SOD_VIOLATION, PRODUCTION_DISABLED, PRODUCTION_KILL_SWITCH_DENY ->
                    HttpStatus.FORBIDDEN;
            case PRODUCTION_TARGET_NOT_FOUND, PRODUCTION_GRANT_NOT_FOUND, PRODUCTION_GRANT_MISSING ->
                    HttpStatus.NOT_FOUND;
            case PRODUCTION_LEASE_CONFLICT, PRODUCTION_LEASE_UNAVAILABLE, PRODUCTION_GRANT_ACTIVE_CONFLICT,
                 PRODUCTION_GRANT_ALREADY_ISSUED, PRODUCTION_GRANT_ALREADY_CONSUMED, PRODUCTION_AUTHORIZATION_STALE,
                 PRODUCTION_AUTHORIZATION_MISSING, PRODUCTION_FINGERPRINT_STALE, PRODUCTION_FINGERPRINT_MISMATCH,
                 PRODUCTION_AUDIT_CHAIN_INVALID, PRODUCTION_RATE_LIMIT_EXCEEDED ->
                    HttpStatus.CONFLICT;
            case PRODUCTION_GATEWAY_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
