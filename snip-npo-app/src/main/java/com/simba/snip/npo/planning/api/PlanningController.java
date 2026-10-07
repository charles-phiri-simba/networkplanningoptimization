package com.simba.snip.npo.planning.api;

import com.simba.snip.npo.planning.service.PlanningComparisonService;
import com.simba.snip.npo.planning.service.PlanningEvaluationService;
import com.simba.snip.npo.planning.service.PlanningPrerequisiteService;
import com.simba.snip.npo.planning.service.PlanningScenarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {

    private final PlanningScenarioService scenarioService;
    private final PlanningPrerequisiteService prerequisiteService;
    private final PlanningEvaluationService evaluationService;
    private final PlanningComparisonService comparisonService;

    public PlanningController(
            PlanningScenarioService scenarioService,
            PlanningPrerequisiteService prerequisiteService,
            PlanningEvaluationService evaluationService,
            PlanningComparisonService comparisonService
    ) {
        this.scenarioService = scenarioService;
        this.prerequisiteService = prerequisiteService;
        this.evaluationService = evaluationService;
        this.comparisonService = comparisonService;
    }

    @PostMapping(path = "/scenarios", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlanningScenarioDetailDto> create(@RequestBody CreatePlanningScenarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scenarioService.create(request));
    }

    @GetMapping("/scenarios")
    public List<PlanningScenarioSummaryDto> list() {
        return scenarioService.list();
    }

    @GetMapping("/scenarios/{id}")
    public PlanningScenarioDetailDto get(@PathVariable UUID id) {
        return scenarioService.get(id);
    }

    @PatchMapping(path = "/scenarios/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PlanningScenarioDetailDto replace(@PathVariable UUID id, @RequestBody ReplacePlanningScenarioRequest request) {
        return scenarioService.replace(id, request);
    }

    @GetMapping("/scenarios/{id}/prerequisites")
    public Map<String, Object> prerequisites(@PathVariable UUID id) {
        return Map.of(
                "scenarioId", id,
                "cells", prerequisiteService.prerequisites(id)
        );
    }

    @PostMapping(path = "/scenarios/{id}/evaluations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlanningEvaluationDto> evaluate(
            @PathVariable UUID id,
            @RequestBody EvaluatePlanningScenarioRequest request
    ) {
        PlanningEvaluationService.EvaluateResult result = evaluationService.evaluate(id, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.body());
    }

    @GetMapping("/scenarios/{id}/evaluations/{evaluationId}")
    public PlanningEvaluationDto evaluation(@PathVariable UUID id, @PathVariable UUID evaluationId) {
        return evaluationService.get(id, evaluationId);
    }

    @GetMapping("/scenarios/{id}/comparison")
    public PlanningComparisonDto comparison(@PathVariable UUID id) {
        return comparisonService.compare(id);
    }
}
