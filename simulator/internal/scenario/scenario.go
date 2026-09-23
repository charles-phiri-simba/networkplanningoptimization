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

// logicalObservation is scenario-owned: cell, metric, value, relative offset,
// and logical identity. Runtime options supply the time anchor and run ID.
type logicalObservation struct {
	LogicalID string
	CellID    string
	Metric    string
	Value     float64
	Unit      string
	Offset    time.Duration
}

// Build produces a scenario with the historical FIXED default (T0 =
// 2026-08-24T10:00:00Z, legacy event IDs). It does not consult the wall clock.
func Build(name string) ([]event.TelemetryEvent, error) {
	return BuildWith(name, Options{})
}

// BuildWith applies validated runtime options. Validation failures return
// before any event is constructed.
func BuildWith(name string, opts Options) ([]event.TelemetryEvent, error) {
	resolved, err := Resolve(opts)
	if err != nil {
		return nil, err
	}
	defs, err := observations(name)
	if err != nil {
		return nil, err
	}
	t0 := resolved.T0
	if resolved.TimeMode == TimeModeNow {
		t0 = resolved.Anchor.Add(-maxOffset(defs))
	}
	events := make([]event.TelemetryEvent, 0, len(defs))
	for _, d := range defs {
		id, err := ComposeEventID(name, resolved.RunID, d.LogicalID)
		if err != nil {
			return nil, err
		}
		ts := t0.Add(d.Offset)
		if resolved.TimeMode == TimeModeNow && ts.After(resolved.Anchor) {
			return nil, fmt.Errorf("NOW eventTime %s is after anchor %s", ts.Format(time.RFC3339), resolved.Anchor.Format(time.RFC3339))
		}
		events = append(events, event.NewCellKpi(id, d.CellID, d.Metric, d.Value, d.Unit, ts))
	}
	return events, nil
}

func observations(name string) ([]logicalObservation, error) {
	switch name {
	case HighBlerLoad:
		return highBlerLoadObservations(), nil
	case HealthyStable:
		return healthyStableObservations(), nil
	case UnknownCell:
		return unknownCellObservations(), nil
	default:
		return nil, fmt.Errorf("unknown scenario: %s", name)
	}
}

func highBlerLoadObservations() []logicalObservation {
	bler := []float64{0.04, 0.06, 0.09, 0.12}
	prb := []float64{0.60, 0.68, 0.77, 0.84}
	out := make([]logicalObservation, 0, 8)
	for i := 0; i < 4; i++ {
		off := time.Duration(i) * 5 * time.Minute
		out = append(out, logicalObservation{
			LogicalID: fmt.Sprintf("bler-dl-%02d", i+1),
			CellID:    "CELL-001",
			Metric:    "BLER_DL",
			Value:     bler[i],
			Unit:      "ratio",
			Offset:    off,
		})
		out = append(out, logicalObservation{
			LogicalID: fmt.Sprintf("prb-dl-%02d", i+1),
			CellID:    "CELL-001",
			Metric:    "PRB_UTILIZATION_DL",
			Value:     prb[i],
			Unit:      "ratio",
			Offset:    off,
		})
	}
	return out
}

func healthyStableObservations() []logicalObservation {
	out := make([]logicalObservation, 0, 8)
	for i := 0; i < 4; i++ {
		off := time.Duration(i) * 5 * time.Minute
		out = append(out, logicalObservation{
			LogicalID: fmt.Sprintf("bler-dl-%02d", i+1),
			CellID:    "CELL-002",
			Metric:    "BLER_DL",
			Value:     0.008,
			Unit:      "ratio",
			Offset:    off,
		})
		out = append(out, logicalObservation{
			LogicalID: fmt.Sprintf("prb-dl-%02d", i+1),
			CellID:    "CELL-002",
			Metric:    "PRB_UTILIZATION_DL",
			Value:     0.41,
			Unit:      "ratio",
			Offset:    off,
		})
	}
	return out
}

func unknownCellObservations() []logicalObservation {
	return []logicalObservation{{
		LogicalID: "bler-dl-01",
		CellID:    "CELL-MISSING",
		Metric:    "BLER_DL",
		Value:     0.12,
		Unit:      "ratio",
		Offset:    0,
	}}
}
