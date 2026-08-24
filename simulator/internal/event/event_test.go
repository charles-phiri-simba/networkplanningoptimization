package event

import (
	"encoding/json"
	"testing"
	"time"
)

func TestCanonicalJSONHasRequiredFields(t *testing.T) {
	ts := time.Date(2026, 8, 24, 10, 15, 0, 0, time.UTC)
	evt := NewCellKpi("evt-123", "CELL-001", "BLER_DL", 0.12, "ratio", ts)
	if err := evt.Validate(); err != nil {
		t.Fatal(err)
	}
	if evt.KafkaKey() != "CELL-001" {
		t.Fatalf("key=%s", evt.KafkaKey())
	}
	raw, err := evt.MarshalJSONCanonical()
	if err != nil {
		t.Fatal(err)
	}
	var parsed map[string]any
	if err := json.Unmarshal(raw, &parsed); err != nil {
		t.Fatal(err)
	}
	for _, field := range []string{"eventId", "eventType", "schemaVersion", "source", "cellId", "metric", "value", "unit", "eventTime", "ingestedAt", "synthetic"} {
		if _, ok := parsed[field]; !ok {
			t.Fatalf("missing %s", field)
		}
	}
	if parsed["eventType"] != TypeCellKpiObserved || parsed["schemaVersion"] != SchemaV1 {
		t.Fatalf("contract mismatch: %v", parsed)
	}
}

func TestMissingEventIDIsInvalid(t *testing.T) {
	evt := NewCellKpi("", "CELL-001", "BLER_DL", 0.12, "ratio", time.Now().UTC())
	if err := evt.Validate(); err == nil {
		t.Fatal("expected error")
	}
}
