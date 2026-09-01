package com.simba.snip.npo.changeexecution.api;

import com.simba.snip.npo.changeexecution.security.ChangeExecutionAuthorizer;
import com.simba.snip.npo.changeexecution.service.NetworkChangeExecutionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ChangeExecutionController {

    private final ChangeExecutionAuthorizer authorizer;
    private final NetworkChangeExecutionService executionService;
    private final ChangeExecutionQueryService queryService;

    public ChangeExecutionController(
            ChangeExecutionAuthorizer authorizer,
            NetworkChangeExecutionService executionService,
            ChangeExecutionQueryService queryService
    ) {
        this.authorizer = authorizer;
        this.executionService = executionService;
        this.queryService = queryService;
    }

    @PostMapping(
            value = "/api/v1/change-execution/executions",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ExecutionDetailDto create(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @RequestBody CreateExecutionRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireRequest();
        var created = executionService.requestExecution(
                request.planId(),
                request.executionTargetId(),
                "api-user"
        );
        return queryService.require(created.getId());
    }

    @GetMapping("/api/v1/change-execution/executions")
    public List<ExecutionSummaryDto> list(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireView();
        return queryService.list();
    }

    @GetMapping("/api/v1/change-execution/executions/{executionId}")
    public ExecutionDetailDto get(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireViewOrEvidence();
        return queryService.require(executionId);
    }

    @GetMapping("/api/v1/change-execution/executions/{executionId}/evidence")
    public Map<String, Object> evidence(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireViewOrEvidence();
        return queryService.evidence(executionId);
    }

    @PostMapping(
            value = "/api/v1/change-execution/executions/{executionId}/review",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ExecutionDetailDto review(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId,
            @RequestBody ReviewExecutionRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireReview();
        executionService.review(
                executionId,
                request.reviewer() == null ? "reviewer" : request.reviewer(),
                request.comment()
        );
        return queryService.require(executionId);
    }

    @PostMapping(
            value = "/api/v1/change-execution/executions/{executionId}/authorize",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ExecutionDetailDto authorize(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId,
            @RequestBody AuthorizeExecutionRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireAuthorize();
        executionService.authorize(
                executionId,
                request.authorizer() == null ? "authorizer" : request.authorizer()
        );
        return queryService.require(executionId);
    }

    @PostMapping("/api/v1/change-execution/executions/{executionId}/execute")
    public ExecutionDetailDto execute(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireAuthorize();
        executionService.execute(executionId);
        return queryService.require(executionId);
    }

    @PostMapping("/api/v1/change-execution/executions/{executionId}/verify")
    public ExecutionDetailDto verify(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireViewOrEvidence();
        executionService.verify(executionId);
        return queryService.require(executionId);
    }

    @PostMapping(
            value = "/api/v1/change-execution/executions/{executionId}/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ExecutionDetailDto cancel(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId,
            @RequestBody CancelExecutionRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireCancel();
        executionService.cancel(
                executionId,
                request.actor() == null ? "operator" : request.actor(),
                request.reason()
        );
        return queryService.require(executionId);
    }

    @PostMapping(
            value = "/api/v1/change-execution/executions/{executionId}/rollback/request",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ExecutionDetailDto rollbackRequest(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId,
            @RequestBody ReviewExecutionRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireRollbackRequest();
        executionService.requestRollback(
                executionId,
                request.reviewer() == null ? "operator" : request.reviewer()
        );
        return queryService.require(executionId);
    }

    @PostMapping(
            value = "/api/v1/change-execution/executions/{executionId}/rollback/review",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ExecutionDetailDto rollbackReview(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId,
            @RequestBody ReviewExecutionRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireRollbackReview();
        executionService.reviewRollback(
                executionId,
                request.reviewer() == null ? "reviewer" : request.reviewer(),
                request.comment()
        );
        return queryService.require(executionId);
    }

    @PostMapping(
            value = "/api/v1/change-execution/executions/{executionId}/rollback/authorize",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ExecutionDetailDto rollbackAuthorize(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId,
            @RequestBody AuthorizeExecutionRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireRollbackAuthorize();
        executionService.authorizeRollback(
                executionId,
                request.authorizer() == null ? "authorizer" : request.authorizer()
        );
        return queryService.require(executionId);
    }

    @PostMapping("/api/v1/change-execution/executions/{executionId}/rollback/execute")
    public ExecutionDetailDto rollbackExecute(
            @RequestHeader(value = ChangeExecutionAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID executionId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireRollbackAuthorize();
        executionService.executeRollback(executionId);
        return queryService.require(executionId);
    }
}
