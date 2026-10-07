package com.simba.snip.npo.planning.service;

import com.simba.snip.npo.planning.persist.PlanningAlternativeEntity;
import com.simba.snip.npo.planning.persist.PlanningCellIntentEntity;
import com.simba.snip.npo.planning.persist.PlanningEvaluationEntity;
import com.simba.snip.npo.planning.persist.PlanningScenarioCellEntity;
import com.simba.snip.npo.planning.persist.PlanningScenarioEntity;

import java.util.List;

public record PlanningScenarioGraph(
        PlanningScenarioEntity scenario,
        List<PlanningScenarioCellEntity> cells,
        List<PlanningAlternativeEntity> alternatives,
        List<PlanningCellIntentEntity> intents,
        List<PlanningEvaluationEntity> evaluations
) {
}
