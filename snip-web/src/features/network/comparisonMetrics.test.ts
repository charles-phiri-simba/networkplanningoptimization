import { describe, expect, it } from 'vitest'
import { caseFixture, contextFixture, resolvedCaseFixture, warningCaseFixture } from '../../test/fixtures'
import {
  UNAVAILABLE_LABEL,
  contextIsSynthetic,
  currentMetric,
  formatMetricDisplay,
  highestActiveSeverityForCell,
  relatedAssuranceLabel,
  txPowerDisplay,
} from './comparisonMetrics'

describe('comparison metrics', () => {
  it('reads txPower and BLER_DL from context', () => {
    expect(txPowerDisplay(contextFixture)).toBe('46 dBm')
    expect(currentMetric(contextFixture, 'BLER_DL')?.value).toBe(0.12)
    expect(formatMetricDisplay(contextFixture, 'PRB_UTILIZATION_DL')).toBe(UNAVAILABLE_LABEL)
    expect(formatMetricDisplay(contextFixture, 'PRB_UTILIZATION_DL')).not.toBe('0')
  })

  it('does not treat missing context as zero', () => {
    expect(txPowerDisplay(null)).toBe(UNAVAILABLE_LABEL)
    expect(formatMetricDisplay(undefined, 'BLER_DL')).toBe(UNAVAILABLE_LABEL)
  })

  it('labels synthetic comparison observations', () => {
    expect(contextIsSynthetic(contextFixture)).toBe(true)
  })

  it('uses PI4 active semantics and severity order', () => {
    expect(highestActiveSeverityForCell([caseFixture, warningCaseFixture, resolvedCaseFixture], 'CELL-001')).toBe(
      'CRITICAL',
    )
    expect(highestActiveSeverityForCell([resolvedCaseFixture], 'CELL-001')).toBeNull()
    expect(relatedAssuranceLabel('CRITICAL')).toBe('Critical Assurance finding')
    expect(relatedAssuranceLabel('MAJOR')).toBe('Major Assurance finding')
    expect(relatedAssuranceLabel(null)).toBe('No active Assurance finding')
    expect(relatedAssuranceLabel(null)).not.toMatch(/health/i)
  })
})
