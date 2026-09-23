package scenario

import (
	"strings"
	"testing"
	"time"
)

func TestT10InvalidBaseTime(t *testing.T) {
	_, err := Resolve(Options{TimeMode: TimeModeFixed, BaseTime: "not-a-time"})
	if err == nil {
		t.Fatal("expected malformed base-time to fail")
	}
	if !strings.Contains(err.Error(), "RFC3339") {
		t.Fatalf("error=%v", err)
	}
}

func TestT11MissingTimezone(t *testing.T) {
	_, err := Resolve(Options{TimeMode: TimeModeFixed, BaseTime: "2026-09-23T12:00:00"})
	if err == nil {
		t.Fatal("expected missing timezone to fail")
	}
	if !strings.Contains(err.Error(), "timezone") {
		t.Fatalf("error=%v", err)
	}
}

func TestT12NowWithoutRunID(t *testing.T) {
	_, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeNow,
		Clock:    fixedClock(time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)),
	})
	if err == nil {
		t.Fatal("expected now without run-id to fail")
	}
	if !strings.Contains(err.Error(), "run-id") {
		t.Fatalf("error=%v", err)
	}
}

func TestT13NowWithBaseTime(t *testing.T) {
	_, err := BuildWith(HighBlerLoad, Options{
		TimeMode: TimeModeNow,
		BaseTime: "2026-09-23T12:00:00Z",
		RunID:    "demo-invalid-combo",
		Clock:    fixedClock(time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)),
	})
	if err == nil {
		t.Fatal("expected now+base-time to fail")
	}
	if !strings.Contains(err.Error(), "base-time") {
		t.Fatalf("error=%v", err)
	}
}

func TestT14InvalidRunIDs(t *testing.T) {
	cases := []struct {
		name  string
		runID string
	}{
		{name: "whitespace", runID: "demo 001"},
		{name: "slash", runID: "demo/001"},
		{name: "double-hyphen", runID: "demo--001"},
		{name: "too-long", runID: strings.Repeat("a", 49)},
		{name: "invalid-first", runID: "-demo"},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			_, err := Resolve(Options{RunID: tc.runID})
			if err == nil {
				t.Fatalf("expected invalid run-id %q to fail", tc.runID)
			}
		})
	}
}

func TestT15EventIDLength(t *testing.T) {
	longLogical := strings.Repeat("x", MaxEventIDLength)
	id, err := ComposeEventID(HighBlerLoad, "", longLogical)
	if err == nil {
		t.Fatalf("expected length failure, got %q (len=%d)", id, len(id))
	}
	if id != "" {
		t.Fatalf("must not truncate or return id: %q", id)
	}
	if !strings.Contains(err.Error(), "128") {
		t.Fatalf("error=%v", err)
	}
	if strings.Contains(err.Error(), longLogical) && strings.Contains(err.Error(), "truncated") {
		t.Fatalf("unexpected truncation language: %v", err)
	}
}

func TestUnknownTimeMode(t *testing.T) {
	_, err := Resolve(Options{TimeMode: "wall-clock"})
	if err == nil || !strings.Contains(err.Error(), "unknown time-mode") {
		t.Fatalf("error=%v", err)
	}
}

func TestFutureFixedBaseTimeFailsClosed(t *testing.T) {
	now := time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)
	_, err := Resolve(Options{
		TimeMode: TimeModeFixed,
		BaseTime: "2026-09-23T12:00:02Z",
		Clock:    fixedClock(now),
	})
	if err == nil {
		t.Fatal("expected future base-time to fail")
	}
	_, err = Resolve(Options{
		TimeMode: TimeModeFixed,
		BaseTime: "2026-09-23T12:00:01Z",
		Clock:    fixedClock(now),
	})
	if err != nil {
		t.Fatalf("exactly 1s future should be accepted: %v", err)
	}
}

func TestComposeEventIDLegacyAndScoped(t *testing.T) {
	legacy, err := ComposeEventID(HighBlerLoad, "", "bler-dl-01")
	if err != nil || legacy != "high-bler-load-bler-dl-01" {
		t.Fatalf("legacy=%s err=%v", legacy, err)
	}
	scoped, err := ComposeEventID(HighBlerLoad, "demo-20260923-001", "bler-dl-01")
	if err != nil || scoped != "high-bler-load--demo-20260923-001--bler-dl-01" {
		t.Fatalf("scoped=%s err=%v", scoped, err)
	}
}

func TestDefaultResolveDoesNotNeedClock(t *testing.T) {
	calls := 0
	resolved, err := Resolve(Options{
		Clock: func() time.Time {
			calls++
			return time.Date(2026, 9, 23, 12, 0, 0, 0, time.UTC)
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	if calls != 0 {
		t.Fatalf("default FIXED consulted clock %d times", calls)
	}
	if resolved.TimeMode != TimeModeFixed || !resolved.T0.Equal(DefaultFixedBaseTime) {
		t.Fatalf("resolved=%+v", resolved)
	}
}
