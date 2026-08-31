package com.simba.snip.npo.changeplanning.api;

import com.simba.snip.npo.changeplanning.authorization.ChangePlanAuthorizer;
import com.simba.snip.npo.changeplanning.model.ExecutionReadinessResult;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.service.ChangePlanGovernanceService;
import com.simba.snip.npo.changeplanning.service.ChangePlanReadinessService;
import com.simba.snip.npo.changeplanning.service.NetworkChangePlanService;
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
public class ChangePlanningController {

    private final ChangePlanAuthorizer authorizer;
    private final NetworkChangePlanService planService;
    private final ChangePlanGovernanceService governanceService;
    private final ChangePlanReadinessService readinessService;
    private final ChangePlanQueryService queryService;

    public ChangePlanningController(
            ChangePlanAuthorizer authorizer,
            NetworkChangePlanService planService,
            ChangePlanGovernanceService governanceService,
            ChangePlanReadinessService readinessService,
            ChangePlanQueryService queryService
    ) {
        this.authorizer = authorizer;
        this.planService = planService;
        this.governanceService = governanceService;
        this.readinessService = readinessService;
        this.queryService = queryService;
    }

    @PostMapping(
            value = "/api/v1/change-planning/plans",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ChangePlanDetailDto create(
            @RequestHeader(value = ChangePlanAuthorizer.HEADER, required = false) String permission,
            @RequestBody CreateChangePlanRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireCreate();
        NetworkChangePlanEntity plan = planService.createPlan(request.proposalId(), "api-user");
        return queryService.require(plan.getId());
    }

    @GetMapping("/api/v1/change-planning/plans")
    public List<ChangePlanSummaryDto> list(
            @RequestHeader(value = ChangePlanAuthorizer.HEADER, required = false) String permission
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireView();
        return queryService.list();
    }

    @GetMapping("/api/v1/change-planning/plans/{planId}")
    public ChangePlanDetailDto get(
            @RequestHeader(value = ChangePlanAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID planId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireViewOrReview();
        return queryService.require(planId);
    }

    @GetMapping("/api/v1/change-planning/plans/{planId}/evidence")
    public Map<String, Object> evidence(
            @RequestHeader(value = ChangePlanAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID planId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireViewOrReview();
        return queryService.evidence(planId);
    }

    @PostMapping(
            value = "/api/v1/change-planning/plans/{planId}/review",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ChangePlanDetailDto review(
            @RequestHeader(value = ChangePlanAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID planId,
            @RequestBody ReviewChangePlanRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireReview();
        governanceService.review(
                planId,
                request.reviewer() == null ? "reviewer" : request.reviewer(),
                request.comment()
        );
        return queryService.require(planId);
    }

    @PostMapping(
            value = "/api/v1/change-planning/plans/{planId}/authorize",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ChangePlanDetailDto authorize(
            @RequestHeader(value = ChangePlanAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID planId,
            @RequestBody AuthorizeChangePlanRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireAuthorize();
        governanceService.authorize(
                planId,
                request.authorizer() == null ? "authorizer" : request.authorizer()
        );
        return queryService.require(planId);
    }

    @PostMapping(
            value = "/api/v1/change-planning/plans/{planId}/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ChangePlanDetailDto cancel(
            @RequestHeader(value = ChangePlanAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID planId,
            @RequestBody CancelChangePlanRequest request
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireCancel();
        governanceService.cancel(
                planId,
                request.actor() == null ? "canceller" : request.actor(),
                request.reason()
        );
        return queryService.require(planId);
    }

    @PostMapping("/api/v1/change-planning/plans/{planId}/readiness")
    public ChangePlanDetailDto readiness(
            @RequestHeader(value = ChangePlanAuthorizer.HEADER, required = false) String permission,
            @PathVariable UUID planId
    ) {
        authorizer.bindRequestPermission(permission);
        authorizer.requireAuthorize();
        ChangePlanReadinessService.ReadinessOutcome outcome = readinessService.evaluate(planId);
        ChangePlanDetailDto detail = queryService.require(planId);
        if (outcome.result() != ExecutionReadinessResult.READY) {
            return detail;
        }
        return detail;
    }
}
