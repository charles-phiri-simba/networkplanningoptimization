package com.simba.snip.npo.twin;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellParameterSimulationModelTest {

    private final CellParameterSimulationModel model = new CellParameterSimulationModel();

    @Test
    void twoDecibelReductionIsDeterministic() {
        CellParameterSimulationModel.ModelOutput first = predict(46, 44);
        CellParameterSimulationModel.ModelOutput second = predict(46, 44);
        assertEquals(first.metrics(), second.metrics());
        assertEquals(0.126, metric(first, "BLER_DL").candidateValue());
        assertEquals(0.8232, metric(first, "PRB_UTILIZATION_DL").candidateValue());
        assertEquals(40.32, metric(first, "THROUGHPUT_DL").candidateValue());
        assertEquals(SimulationConfidence.LOW, first.confidence());
        assertEquals(5, first.limitations().size());
        assertTrue(first.assumptions().get(0).contains("not vendor-calibrated"));
        assertEquals("snip.synthetic.cell-parameter.v1", first.metadata().modelId());
        assertEquals("1.0", first.metadata().modelVersion());
        assertEquals("RULE_BASED", first.metadata().modelType());
    }

    @Test
    void fourDecibelReductionHasLargerBlerIncrease() {
        MetricComparison two = metric(predict(46, 44), "BLER_DL");
        MetricComparison four = metric(predict(46, 42), "BLER_DL");
        assertEquals(0.132, four.candidateValue());
        assertTrue(four.delta() > two.delta());
    }

    @Test
    void missingRequiredKpisAreModelFailure() {
        assertThrows(com.simba.snip.npo.domain.DomainConflictException.class, () -> model.predict(
                new CellParameterSimulationModel.SimulationInput(
                        BigDecimal.valueOf(40), BigDecimal.valueOf(38), null, 0.5, null, null)));
    }

    private CellParameterSimulationModel.ModelOutput predict(int current, int proposed) {
        return model.predict(new CellParameterSimulationModel.SimulationInput(
                BigDecimal.valueOf(current),
                BigDecimal.valueOf(proposed),
                0.12,
                0.84,
                42.0,
                "INCREASING"
        ));
    }

    private static MetricComparison metric(CellParameterSimulationModel.ModelOutput output, String name) {
        List<MetricComparison> matches = output.metrics().stream().filter(m -> name.equals(m.metric())).toList();
        assertEquals(1, matches.size());
        return matches.get(0);
    }
}
