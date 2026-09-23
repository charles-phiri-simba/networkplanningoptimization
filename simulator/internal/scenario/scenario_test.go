package scenario

import (
	"strings"
	"testing"
	"time"

	"github.com/charles-phiri-simba/networkplanningoptimization/simulator/internal/event"
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

func TestT1LegacyFixedTimestamps(t *testing.T) {
	events, err := Build(HighBlerLoad)
	if err != nil {
		t.Fatal(err)
	}
	want := []time.Time{
		DefaultFixedBaseTime,
		DefaultFixedBaseTime,
		DefaultFixedBaseTime.Add(5 * time.Minute),
		DefaultFixedBaseTime.Add(5 * time.Minute),
		DefaultFixedBaseTime.Add(10 * time.Minute),
		DefaultFixedBaseTime.Add(10 * time.Minute),
		DefaultFixedBaseTime.Add(15 * time.Minute),
		DefaultFixedBaseTime.Add(15 * time.Minute),
	}
	if len(events) != len(want) {
		t.Fatalf("len=%d", len(events))
	}
	for i, evt := range events {
		if !evt.EventTime.Equal(want[i]) {
			t.Fatalf("event[%d] eventTime=%s want=%s", i, evt.EventTime, want[i])
		}
	}
}

func TestT2LegacyEventIDs(t *testing.T) {
	events, err := Build(HighBlerLoad)
	if err != nil {
		t.Fatal(err)
	}
	want := []string{
		"high-bler-load-bler-dl-01",
		"high-bler-load-prb-dl-01",
		"high-bler-load-bler-dl-02",
		"high-bler-load-prb-dl-02",
		"high-bler-load-bler-dl-03",
		"high-bler-load-prb-dl-03",
		"high-bler-load-bler-dl-04",
		"high-bler-load-prb-dl-04",
	}
	got := eventIDs(events)
	if !stringEqual(got, want) {
		t.Fatalf("ids=%v want=%v", got, want)
	}
}

func TestT3NowAnchoredTiming(t *testing.T) {
	anchor := time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)
	events, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeNow,
		RunID:    "demo-20260923-001",
		Clock:    fixedClock(anchor),
	})
	if err != nil {
		t.Fatal(err)
	}
	wantTimes := []string{
		"2026-09-23T11:45:00Z",
		"2026-09-23T11:45:00Z",
		"2026-09-23T11:50:00Z",
		"2026-09-23T11:50:00Z",
		"2026-09-23T11:55:00Z",
		"2026-09-23T11:55:00Z",
		"2026-09-23T12:00:00Z",
		"2026-09-23T12:00:00Z",
	}
	if len(events) != 8 {
		t.Fatalf("len=%d", len(events))
	}
	var latest time.Time
	for i, evt := range events {
		got := evt.EventTime.UTC().Format(time.RFC3339)
		if got != wantTimes[i] {
			t.Fatalf("event[%d] eventTime=%s want=%s", i, got, wantTimes[i])
		}
		if evt.EventTime.After(anchor) {
			t.Fatalf("event[%d] after anchor", i)
		}
		if evt.EventTime.After(latest) {
			latest = evt.EventTime
		}
	}
	if !latest.Equal(anchor) {
		t.Fatalf("latest=%s want=%s", latest, anchor)
	}
}

func TestT4NowCapturesClockOnce(t *testing.T) {
	start := time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)
	calls := 0
	clk := func() time.Time {
		when := start.Add(time.Duration(calls) * time.Hour)
		calls++
		return when
	}
	events, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeNow,
		RunID:    "demo-clock-once",
		Clock:    clk,
	})
	if err != nil {
		t.Fatal(err)
	}
	if calls != 1 {
		t.Fatalf("clock calls=%d want=1", calls)
	}
	if !events[len(events)-1].EventTime.Equal(start) {
		t.Fatalf("latest=%s want first clock instant %s (later calls would have shifted T0)", events[len(events)-1].EventTime, start)
	}
}

func TestT5DifferentRunIDs(t *testing.T) {
	a, err := BuildWith(HighBlerLoad, Options{RunID: "run-a"})
	if err != nil {
		t.Fatal(err)
	}
	b, err := BuildWith(HighBlerLoad, Options{RunID: "run-b"})
	if err != nil {
		t.Fatal(err)
	}
	if stringEqual(eventIDs(a), eventIDs(b)) {
		t.Fatal("distinct run IDs produced identical event IDs")
	}
	for i := range a {
		if a[i].EventID == b[i].EventID {
			t.Fatalf("event[%d] IDs collided: %s", i, a[i].EventID)
		}
	}
}

func TestT6SameRunIDReplay(t *testing.T) {
	opts := Options{TimeMode: TimeModeNow, RunID: "demo-replay", Clock: fixedClock(time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC))}
	a, err := BuildWith(HighBlerLoad, opts)
	if err != nil {
		t.Fatal(err)
	}
	b, err := BuildWith(HighBlerLoad, opts)
	if err != nil {
		t.Fatal(err)
	}
	if !stringEqual(eventIDs(a), eventIDs(b)) {
		t.Fatalf("replay IDs differ: %v vs %v", eventIDs(a), eventIDs(b))
	}
}

func TestT7TimestampNotInIdentity(t *testing.T) {
	runID := "demo-no-clock-in-id"
	a, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeNow,
		RunID:    runID,
		Clock:    fixedClock(time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)),
	})
	if err != nil {
		t.Fatal(err)
	}
	b, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeNow,
		RunID:    runID,
		Clock:    fixedClock(time.Date(2026, 9, 24, 18, 30, 0, 0, time.UTC)),
	})
	if err != nil {
		t.Fatal(err)
	}
	if !stringEqual(eventIDs(a), eventIDs(b)) {
		t.Fatalf("event IDs changed with clock: %v vs %v", eventIDs(a), eventIDs(b))
	}
	if a[0].EventTime.Equal(b[0].EventTime) {
		t.Fatal("expected different eventTimes when clock changes")
	}
	for _, id := range eventIDs(a) {
		if strings.Contains(id, "2026-09-23") || strings.Contains(id, "20260923T") {
			t.Fatalf("event ID includes timestamp: %s", id)
		}
	}
}

func TestT8ValidExplicitBaseTime(t *testing.T) {
	t0 := time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)
	events, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeFixed,
		BaseTime: "2026-09-23T12:00:00Z",
		Clock:    fixedClock(t0),
	})
	if err != nil {
		t.Fatal(err)
	}
	if !events[0].EventTime.Equal(t0) {
		t.Fatalf("T0=%s want=%s", events[0].EventTime, t0)
	}
	if !events[6].EventTime.Equal(t0.Add(15 * time.Minute)) {
		t.Fatalf("last BLER=%s", events[6].EventTime)
	}
	if events[0].EventID != "high-bler-load-bler-dl-01" {
		t.Fatalf("legacy ID changed: %s", events[0].EventID)
	}
}

func TestT9TimezoneNormalization(t *testing.T) {
	clock := fixedClock(time.Date(2026, 9, 23, 14, 0, 0, 0, time.UTC))
	events, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeFixed,
		BaseTime: "2026-09-23T14:00:00+02:00",
		Clock:    clock,
	})
	if err != nil {
		t.Fatal(err)
	}
	want := time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)
	if !events[0].EventTime.Equal(want) {
		t.Fatalf("normalized T0=%s want=%s", events[0].EventTime, want)
	}
	if events[0].EventTime.Location() != time.UTC {
		t.Fatalf("location=%v", events[0].EventTime.Location())
	}
}

func TestT16SyntheticProvenance(t *testing.T) {
	sets := [][]event.TelemetryEvent{}
	legacy, err := Build(HighBlerLoad)
	if err != nil {
		t.Fatal(err)
	}
	sets = append(sets, legacy)
	nowEvents, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeNow,
		RunID:    "demo-provenance",
		Clock:    fixedClock(time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)),
	})
	if err != nil {
		t.Fatal(err)
	}
	sets = append(sets, nowEvents)
	for _, events := range sets {
		for _, evt := range events {
			if !evt.Synthetic {
				t.Fatalf("synthetic=false eventId=%s", evt.EventID)
			}
			if evt.Source != event.SourceSimulator {
				t.Fatalf("source=%s eventId=%s", evt.Source, evt.EventID)
			}
		}
	}
}

func TestT17NowFreshnessProperty(t *testing.T) {
	clockNow := time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)
	events, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeNow,
		RunID:    "demo-fresh",
		Clock:    fixedClock(clockNow),
	})
	if err != nil {
		t.Fatal(err)
	}
	floor := clockNow.Add(-168 * time.Hour)
	for _, evt := range events {
		if evt.EventTime.Before(floor) || evt.EventTime.After(clockNow) {
			t.Fatalf("eventTime %s outside [%s, %s]", evt.EventTime, floor, clockNow)
		}
	}
}

func TestT18ExistingScenarios(t *testing.T) {
	healthy, err := Build(HealthyStable)
	if err != nil {
		t.Fatal(err)
	}
	if len(healthy) != 8 {
		t.Fatalf("len=%d", len(healthy))
	}
	if healthy[0].EventID != "healthy-stable-bler-dl-01" || healthy[0].CellID != "CELL-002" {
		t.Fatalf("healthy[0]=%+v", healthy[0])
	}
	if !healthy[0].EventTime.Equal(DefaultFixedBaseTime) {
		t.Fatalf("healthy T0=%s", healthy[0].EventTime)
	}
	if healthy[1].Value != 0.41 || healthy[0].Value != 0.008 {
		t.Fatalf("healthy values bler=%v prb=%v", healthy[0].Value, healthy[1].Value)
	}

	unknown, err := Build(UnknownCell)
	if err != nil {
		t.Fatal(err)
	}
	if len(unknown) != 1 || unknown[0].CellID != "CELL-MISSING" {
		t.Fatalf("unknown=%v", unknown)
	}
	if unknown[0].EventID != "unknown-cell-bler-dl-01" {
		t.Fatalf("unknown id=%s", unknown[0].EventID)
	}
	if !unknown[0].EventTime.Equal(DefaultFixedBaseTime) {
		t.Fatalf("unknown T0=%s", unknown[0].EventTime)
	}
}

func TestHealthyStableAndUnknownCell(t *testing.T) {
	TestT18ExistingScenarios(t)
}

func TestUnknownScenario(t *testing.T) {
	if _, err := Build("nope"); err == nil {
		t.Fatal("expected error")
	}
}

func TestOptionalNowContractDemo(t *testing.T) {
	events, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeNow,
		RunID:    "demo-20260923-001",
		Clock:    fixedClock(time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)),
	})
	if err != nil {
		t.Fatal(err)
	}
	for _, evt := range events {
		t.Logf("eventId=%s eventTime=%s synthetic=%v source=%s metric=%s value=%v",
			evt.EventID, evt.EventTime.UTC().Format(time.RFC3339), evt.Synthetic, evt.Source, evt.Metric, evt.Value)
	}
}

func fixedClock(t time.Time) Clock {
	return func() time.Time { return t }
}

func eventIDs(events []event.TelemetryEvent) []string {
	ids := make([]string, len(events))
	for i, evt := range events {
		ids[i] = evt.EventID
	}
	return ids
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

func stringEqual(a, b []string) bool {
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
