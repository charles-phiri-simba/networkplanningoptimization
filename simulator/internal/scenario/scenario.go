package scenario

import (
	"fmt"
	"time"

	"github.com/charles-phiri-simba/networkplanningoptimization/simulator/internal/event"
)

const (
	HighBlerLoad  = "high-bler-load"
	HealthyStable = "healthy-stable"
	UnknownCell   = "unknown-cell"
)

var baseTime = time.Date(2026, 8, 24, 10, 0, 0, 0, time.UTC)

func Build(name string) ([]event.TelemetryEvent, error) {
	switch name {
	case HighBlerLoad:
		return highBlerLoad(), nil
	case HealthyStable:
		return healthyStable(), nil
	case UnknownCell:
		return unknownCell(), nil
	default:
		return nil, fmt.Errorf("unknown scenario: %s", name)
	}
}

func highBlerLoad() []event.TelemetryEvent {
	bler := []float64{0.04, 0.06, 0.09, 0.12}
	prb := []float64{0.60, 0.68, 0.77, 0.84}
	events := make([]event.TelemetryEvent, 0, 8)
	for i := 0; i < 4; i++ {
		ts := baseTime.Add(time.Duration(i) * 5 * time.Minute)
		events = append(events, event.NewCellKpi(
			fmt.Sprintf("high-bler-load-bler-dl-%02d", i+1),
			"CELL-001", "BLER_DL", bler[i], "ratio", ts,
		))
		events = append(events, event.NewCellKpi(
			fmt.Sprintf("high-bler-load-prb-dl-%02d", i+1),
			"CELL-001", "PRB_UTILIZATION_DL", prb[i], "ratio", ts,
		))
	}
	return events
}

func healthyStable() []event.TelemetryEvent {
	events := make([]event.TelemetryEvent, 0, 8)
	for i := 0; i < 4; i++ {
		ts := baseTime.Add(time.Duration(i) * 5 * time.Minute)
		events = append(events, event.NewCellKpi(
			fmt.Sprintf("healthy-stable-bler-dl-%02d", i+1),
			"CELL-002", "BLER_DL", 0.008, "ratio", ts,
		))
		events = append(events, event.NewCellKpi(
			fmt.Sprintf("healthy-stable-prb-dl-%02d", i+1),
			"CELL-002", "PRB_UTILIZATION_DL", 0.41, "ratio", ts,
		))
	}
	return events
}

func unknownCell() []event.TelemetryEvent {
	return []event.TelemetryEvent{
		event.NewCellKpi("unknown-cell-bler-dl-01", "CELL-MISSING", "BLER_DL", 0.12, "ratio", baseTime),
	}
}
