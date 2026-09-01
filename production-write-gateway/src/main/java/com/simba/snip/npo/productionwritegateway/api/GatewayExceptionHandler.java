package com.simba.snip.npo.productionwritegateway.api;

import com.simba.snip.npo.productionchange.protocol.GatewayExecuteResponse;
import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(GatewayDeniedException.class)
    public ResponseEntity<GatewayExecuteResponse> denied(GatewayDeniedException ex) {
        GatewayExecuteResponse body = new GatewayExecuteResponse(
                ex.productionChangeId(),
                ex.grantId(),
                ex.attemptId(),
                ex.productionChangeStatus(),
                ex.attemptStatus(),
                ex.mutationOutcome() == null ? MutationOutcome.NOT_SENT : ex.mutationOutcome(),
                ex.reasonCode() == null ? ProductionReasonCode.PRODUCTION_INVALID_REQUEST.name() : ex.reasonCode().name(),
                0
        );
        HttpStatus status = ex.reasonCode() == ProductionReasonCode.PRODUCTION_UNAUTHORIZED
                ? HttpStatus.FORBIDDEN
                : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GatewayExecuteResponse> unreadable(HttpMessageNotReadableException ex) {
        GatewayExecuteResponse body = new GatewayExecuteResponse(
                null,
                null,
                null,
                null,
                null,
                MutationOutcome.NOT_SENT,
                ProductionReasonCode.PRODUCTION_INVALID_REQUEST.name(),
                0
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
