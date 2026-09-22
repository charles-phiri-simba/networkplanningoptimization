import { describe, expect, it } from 'vitest'
import type { KpiObservationDto, KpiSeriesDto } from '../../types/network'
import { telemetryChartPoints } from './telemetrySeries'

function observation(overrides: Partial<KpiObservationDto> = {}): KpiObservationDto {
  return {
    metric: 'BLER_DL',
    value: 0.1,
    unit: 'ratio',
    observedAt: '2026-01-01T00:00:00Z',
    eventTime: '2026-01-01T00:00:00Z',
    ingestedAt: '2026-01-01T00:00:00Z',
    eventId: 'e1',
    source: 'DEMO_SEED',
    synthetic: true,
    ...overrides,
  }
}

describe('telemetryChartPoints', () => {
  it('does not append current when history already includes it', () => {
    const current = observation({ value: 0.12, eventId: 'e2', observedAt: '2026-01-01T00:10:00Z' })
    const series: KpiSeriesDto = {
      metric: 'BLER_DL',
      current,
      history: [observation({ value: 0.08, eventId: 'e1' }), current],
      trend: 'DEGRADING',
    }
    expect(telemetryChartPoints(series)).toEqual(series.history)
    expect(telemetryChartPoints(series)).toHaveLength(2)
  })

  it('uses current when history is empty', () => {
    const current = observation()
    const series: KpiSeriesDto = {
      metric: 'BLER_DL',
      current,
      history: [],
      trend: 'STABLE',
    }
    expect(telemetryChartPoints(series)).toEqual([current])
  })
})
