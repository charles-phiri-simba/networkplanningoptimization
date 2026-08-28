package com.simba.snip.npo.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(com.simba.snip.npo.domain.DomainNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(com.simba.snip.npo.domain.DomainNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", ex.getResourceType() + " not found",
                "id", ex.getResourceId()
        ));
    }

    @ExceptionHandler(com.simba.snip.npo.domain.DomainConflictException.class)
    public ResponseEntity<Map<String, String>> conflict(com.simba.snip.npo.domain.DomainConflictException ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        if (ex instanceof com.simba.snip.npo.domain.ImportBusyException busy) {
            if (busy.getActiveExecutionId() != null) {
                body.put("activeExecutionId", busy.getActiveExecutionId().toString());
            }
            if (busy.getFailureCode() != null) {
                body.put("failureCode", busy.getFailureCode());
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(com.simba.snip.npo.integration.security.ConnectorSecurityException.class)
    public ResponseEntity<Map<String, String>> connectorSecurity(
            com.simba.snip.npo.integration.security.ConnectorSecurityException ex
    ) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("failureCode", ex.failureCode().name());
        HttpStatus status = ex.failureCode()
                == com.simba.snip.npo.integration.ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED
                ? HttpStatus.FORBIDDEN
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(com.simba.snip.npo.domain.DomainValidationException.class)
    public ResponseEntity<Map<String, String>> badRequest(com.simba.snip.npo.domain.DomainValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", ex.getMessage()
        ));
    }
}
