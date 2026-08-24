package com.simba.snip.npo.telemetry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrendClassifierTest {

    @Test
    void fourSixNineTwelveIsIncreasing() {
        assertEquals(Trend.INCREASING, TrendClassifier.classify(List.of(4.0, 6.0, 9.0, 12.0)));
        assertEquals(Trend.INCREASING, TrendClassifier.classify(List.of(0.04, 0.06, 0.09, 0.12)));
    }

    @Test
    void twelveNineSixFourIsDecreasing() {
        assertEquals(Trend.DECREASING, TrendClassifier.classify(List.of(12.0, 9.0, 6.0, 4.0)));
    }

    @Test
    void equalValuesAreStable() {
        assertEquals(Trend.STABLE, TrendClassifier.classify(List.of(0.008, 0.008, 0.008, 0.008)));
    }

    @Test
    void fewerThanTwoObservationsIsInsufficient() {
        assertEquals(Trend.INSUFFICIENT_DATA, TrendClassifier.classify(List.of()));
        assertEquals(Trend.INSUFFICIENT_DATA, TrendClassifier.classify(List.of(0.12)));
        assertEquals(Trend.INSUFFICIENT_DATA, TrendClassifier.classify(null));
    }
}
