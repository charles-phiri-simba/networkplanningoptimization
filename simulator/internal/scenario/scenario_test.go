package scenario

import (
	"testing"
)

func TestHighBlerLoadIsDeterministic(t *testing.T) {
	a, err := Build(HighBlerLoad)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := Build(HighBlerLoad)
	if len(a) != 8 || len(b) != 8 {
		t.Fatalf("len a=%d b=%d", len(a), len(b))
	}
	bler := []float64{}
	prb := []float64{}
	for i, evt := range a {
		if err := evt.Validate(); err != nil {
			t.Fatal(err)
		}
		if evt.CellID != "CELL-001" || evt.KafkaKey() != "CELL-001" {
			t.Fatalf("cell %s", evt.CellID)
		}
		if evt.EventID != b[i].EventID {
			t.Fatal("event ids are not deterministic")
		}
		switch evt.Metric {
		case "BLER_DL":
			bler = append(bler, evt.Value)
		case "PRB_UTILIZATION_DL":
			prb = append(prb, evt.Value)
		}
	}
	wantBler := []float64{0.04, 0.06, 0.09, 0.12}
	wantPrb := []float64{0.60, 0.68, 0.77, 0.84}
	if !equal(bler, wantBler) || !equal(prb, wantPrb) {
		t.Fatalf("bler=%v prb=%v", bler, prb)
	}
}

func TestHealthyStableAndUnknownCell(t *testing.T) {
	healthy, err := Build(HealthyStable)
	if err != nil {
		t.Fatal(err)
	}
	if len(healthy) != 8 {
		t.Fatalf("len=%d", len(healthy))
	}
	for _, evt := range healthy {
		if evt.CellID != "CELL-002" || evt.Value != 0.008 && evt.Metric == "BLER_DL" {
			if evt.Metric == "BLER_DL" && evt.Value != 0.008 {
				t.Fatalf("bler=%v", evt.Value)
			}
		}
	}
	unknown, err := Build(UnknownCell)
	if err != nil {
		t.Fatal(err)
	}
	if len(unknown) != 1 || unknown[0].CellID != "CELL-MISSING" {
		t.Fatalf("unknown=%v", unknown)
	}
}

func TestUnknownScenario(t *testing.T) {
	if _, err := Build("nope"); err == nil {
		t.Fatal("expected error")
	}
}

func equal(a, b []float64) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}
