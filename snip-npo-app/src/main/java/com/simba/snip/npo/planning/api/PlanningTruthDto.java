package com.simba.snip.npo.planning.api;

public record PlanningTruthDto(
        boolean independentCellLocal,
        boolean jointSiteSimulation,
        boolean crossCellEffectsModelled,
        boolean synthetic,
        String modelId,
        String confidence
) {
    public static PlanningTruthDto standard() {
        return new PlanningTruthDto(
                true,
                false,
                false,
                true,
                "snip.synthetic.cell-parameter.v1",
                "LOW"
        );
    }
}
