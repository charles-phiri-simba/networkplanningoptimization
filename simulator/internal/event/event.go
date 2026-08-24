package event

import (
	"encoding/json"
	"fmt"
	"time"
)

const (
	TypeCellKpiObserved = "CELL_KPI_OBSERVED"
	SchemaV1            = "1.0"
	SourceSimulator     = "SNIP_SIMULATOR"
)

type TelemetryEvent struct {
	EventID       string    `json:"eventId"`
	EventType     string    `json:"eventType"`
	SchemaVersion string    `json:"schemaVersion"`
	Source        string    `json:"source"`
	CellID        string    `json:"cellId"`
	Metric        string    `json:"metric"`
	Value         float64   `json:"value"`
	Unit          string    `json:"unit"`
	EventTime     time.Time `json:"eventTime"`
	IngestedAt    time.Time `json:"ingestedAt"`
	Synthetic     bool      `json:"synthetic"`
}

func NewCellKpi(eventID, cellID, metric string, value float64, unit string, eventTime time.Time) TelemetryEvent {
	return TelemetryEvent{
		EventID:       eventID,
		EventType:     TypeCellKpiObserved,
		SchemaVersion: SchemaV1,
		Source:        SourceSimulator,
		CellID:        cellID,
		Metric:        metric,
		Value:         value,
		Unit:          unit,
		EventTime:     eventTime.UTC(),
		IngestedAt:    eventTime.UTC(),
		Synthetic:     true,
	}
}

func (e TelemetryEvent) KafkaKey() string {
	return e.CellID
}

func (e TelemetryEvent) Validate() error {
	if e.EventID == "" {
		return fmt.Errorf("eventId is required")
	}
	if e.EventType != TypeCellKpiObserved {
		return fmt.Errorf("unsupported eventType")
	}
	if e.SchemaVersion != SchemaV1 {
		return fmt.Errorf("unsupported schemaVersion")
	}
	if e.Source == "" || e.CellID == "" || e.Metric == "" || e.Unit == "" {
		return fmt.Errorf("required field missing")
	}
	if e.EventTime.IsZero() {
		return fmt.Errorf("eventTime is required")
	}
	if e.Unit == "ratio" && (e.Value < 0 || e.Value > 1) {
		return fmt.Errorf("ratio value out of range")
	}
	return nil
}

func (e TelemetryEvent) MarshalJSONCanonical() ([]byte, error) {
	if err := e.Validate(); err != nil {
		return nil, err
	}
	return json.Marshal(e)
}
