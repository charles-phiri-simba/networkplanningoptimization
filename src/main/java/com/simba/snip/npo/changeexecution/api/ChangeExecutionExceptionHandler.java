package com.simba.snip.npo.changeexecution.api;

import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ChangeExecutionController.class)
public class ChangeExecutionExceptionHandler {

    @ExceptionHandler(ChangeExecutionException.class)
    public ResponseEntity<Map<String, String>> changeExecution(ChangeExecutionException ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("failureCode", ex.failureCode().name());
        HttpStatus status = switch (ex.failureCode()) {
            case EXECUTION_REVIEW_FORBIDDEN, EXECUTION_AUTHORIZATION_FORBIDDEN, EXECUTION_CANCEL_FORBIDDEN,
                 EXECUTION_VIEW_FORBIDDEN, CHANGE_EXECUTION_DISABLED -> HttpStatus.FORBIDDEN;
            case EXECUTION_CONFLICT, CONCURRENT_EXECUTION_CONFLICT, EXECUTION_AUTHORIZATION_STALE,
                 EXECUTION_AUTHORIZATION_MISSING, EXECUTION_CURRENT_VALUE_MISMATCH, EXECUTION_PLAN_NOT_READY,
                 EXECUTION_PLAN_EXPIRED, EXECUTION_PLAN_INVALIDATED, EXECUTION_ALREADY_TERMINAL,
                 EXECUTION_LEASE_UNAVAILABLE, EXECUTION_FENCING_TOKEN_STALE,
                 INVALID_EXECUTION_STATE, EXECUTION_WINDOW_CLOSED, ROLLBACK_AUTHORIZATION_MISSING,
                 ROLLBACK_AUTHORIZATION_STALE, ROLLBACK_CURRENT_VALUE_MISMATCH -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(body);
    }
}
