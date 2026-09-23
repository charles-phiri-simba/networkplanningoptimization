package scenario

import (
	"fmt"
	"regexp"
	"strings"
	"time"
)

const (
	TimeModeFixed = "fixed"
	TimeModeNow   = "now"

	MaxEventIDLength = 128
	MaxRunIDLength   = 48
	FutureTolerance  = time.Second
)

// DefaultFixedBaseTime is the historical high-bler-load T0. Build(name) uses this
// unless FIXED mode is given an explicit RFC3339 base time.
var DefaultFixedBaseTime = time.Date(2026, 8, 24, 10, 0, 0, 0, time.UTC)

// Clock returns the current time. Tests inject a fixed instant; the CLI uses the
// system clock. A single call is taken for NOW scenario construction.
type Clock func() time.Time

// Options are runtime controls. Scenarios own metrics, values, offsets, cells,
// and logical observation IDs. Options own the time anchor and run identity.
type Options struct {
	TimeMode string
	BaseTime string
	RunID    string
	Clock    Clock
}

// Resolved is the validated form of Options. T0 is set for FIXED. Anchor is set
// for NOW (UTC, truncated to seconds, captured once).
type Resolved struct {
	TimeMode string
	RunID    string
	T0       time.Time
	Anchor   time.Time
}

var runIDPattern = regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9._-]{0,47}$`)

func (o Options) now() time.Time {
	if o.Clock != nil {
		return o.Clock()
	}
	return time.Now()
}

// Resolve validates time mode, base time, and run ID without constructing
// events or contacting Kafka.
func Resolve(opts Options) (Resolved, error) {
	mode := strings.ToLower(strings.TrimSpace(opts.TimeMode))
	if mode == "" {
		mode = TimeModeFixed
	}
	if mode != TimeModeFixed && mode != TimeModeNow {
		return Resolved{}, fmt.Errorf("unknown time-mode %q (allowed: fixed, now)", opts.TimeMode)
	}
	if err := validateRunID(opts.RunID); err != nil {
		return Resolved{}, err
	}

	if mode == TimeModeNow {
		if opts.RunID == "" {
			return Resolved{}, fmt.Errorf("time-mode now requires -run-id / SNIP_RUN_ID")
		}
		if opts.BaseTime != "" {
			return Resolved{}, fmt.Errorf("base-time cannot be combined with time-mode now")
		}
		anchor := opts.now().UTC().Truncate(time.Second)
		return Resolved{TimeMode: mode, RunID: opts.RunID, Anchor: anchor}, nil
	}

	if opts.BaseTime == "" {
		return Resolved{TimeMode: mode, RunID: opts.RunID, T0: DefaultFixedBaseTime}, nil
	}
	t0, err := parseBaseTime(opts.BaseTime)
	if err != nil {
		return Resolved{}, err
	}
	now := opts.now().UTC()
	if t0.After(now.Add(FutureTolerance)) {
		return Resolved{}, fmt.Errorf(
			"base-time %s is more than 1s in the future (validation clock %s)",
			t0.Format(time.RFC3339),
			now.Format(time.RFC3339),
		)
	}
	return Resolved{TimeMode: mode, RunID: opts.RunID, T0: t0}, nil
}

func validateRunID(id string) error {
	if id == "" {
		return nil
	}
	if strings.ContainsAny(id, " \t\n\r") {
		return fmt.Errorf("run-id must not contain whitespace: %q", id)
	}
	if strings.Contains(id, "/") {
		return fmt.Errorf("run-id must not contain '/': %q", id)
	}
	if strings.Contains(id, "--") {
		return fmt.Errorf("run-id must not contain '--': %q", id)
	}
	if !runIDPattern.MatchString(id) {
		return fmt.Errorf("run-id must match ^[A-Za-z0-9][A-Za-z0-9._-]{0,47}$ (max %d chars): %q", MaxRunIDLength, id)
	}
	return nil
}

func parseBaseTime(raw string) (time.Time, error) {
	if raw == "" {
		return time.Time{}, fmt.Errorf("base-time is required when supplied")
	}
	if t, err := time.Parse(time.RFC3339, raw); err == nil {
		return t.UTC(), nil
	}
	if t, err := time.Parse(time.RFC3339Nano, raw); err == nil {
		return t.UTC(), nil
	}
	if _, err := time.Parse("2006-01-02T15:04:05", raw); err == nil {
		return time.Time{}, fmt.Errorf("base-time must include an explicit timezone offset (e.g. Z or +02:00): %q", raw)
	}
	return time.Time{}, fmt.Errorf("base-time is not valid RFC3339: %q", raw)
}

// ComposeEventID builds the backend event ID. Legacy IDs (no run ID) stay
// {scenario}-{logical-id}. Run-scoped IDs are {scenario}--{run-id}--{logical-id}.
// The NOW clock anchor is never part of the identity.
func ComposeEventID(scenarioName, runID, logicalID string) (string, error) {
	if scenarioName == "" {
		return "", fmt.Errorf("scenario name is required for event id")
	}
	if logicalID == "" {
		return "", fmt.Errorf("logical observation id is required for event id")
	}
	var id string
	if runID == "" {
		id = scenarioName + "-" + logicalID
	} else {
		id = scenarioName + "--" + runID + "--" + logicalID
	}
	if len(id) > MaxEventIDLength {
		return "", fmt.Errorf("event id exceeds %d characters (len=%d); not truncated", MaxEventIDLength, len(id))
	}
	return id, nil
}

func maxOffset(defs []logicalObservation) time.Duration {
	var max time.Duration
	for _, d := range defs {
		if d.Offset > max {
			max = d.Offset
		}
	}
	return max
}
