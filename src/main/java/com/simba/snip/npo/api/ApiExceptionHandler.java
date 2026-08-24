package com.simba.snip.npo.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(com.simba.snip.npo.domain.DomainValidationException.class)
    public ResponseEntity<Map<String, String>> badRequest(com.simba.snip.npo.domain.DomainValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", ex.getMessage()
        ));
    }
}
