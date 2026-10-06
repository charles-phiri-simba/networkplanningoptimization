import { describe, expect, it } from 'vitest'
import {
  acknowledgedCaseFixture,
  caseFixture,
  cellFixture,
  cellTwoFixture,
  infoCaseFixture,
  resolvedCaseFixture,
  siteFixture,
  unlocatedSiteFixture,
  unknownCellCaseFixture,
  warningCaseFixture,
} from '../../test/fixtures'
import { issueTypeLabel } from './issueLabels'
import {
  activeCases,
  cellsNeedingAttention,
  compareIssues,
  criticalActiveCount,
  filterIssues,
  isActiveAssuranceCase,
  networkOperationsSummary,
  severityRank,
  siteAttention,
  siteIdForCell,
  sortIssues,
  UNAVAILABLE,
} from './operationsModel'

const inventoryCells = [cellFixture, cellTwoFixture]
const mixedCases = [
  resolvedCaseFixture,
  infoCaseFixture,
  warningCaseFixture,
  acknowledgedCaseFixture,
  caseFixture,
]

describe('operationsModel', () => {
  it('treats OPEN cases as active', () => {
    expect(isActiveAssuranceCase(caseFixture)).toBe(true)
  })

  it('treats ACKNOWLEDGED cases as active', () => {
    expect(isActiveAssuranceCase(acknowledgedCaseFixture)).toBe(true)
  })

  it('treats RESOLVED cases as inactive', () => {
    expect(isActiveAssuranceCase(resolvedCaseFixture)).toBe(false)
    expect(activeCases(mixedCases).map((item) => item.id)).not.toContain(resolvedCaseFixture.id)
  })

  it('orders severity CRITICAL > MAJOR > WARNING > INFO', () => {
    expect(severityRank('CRITICAL')).toBeGreaterThan(severityRank('MAJOR'))
    expect(severityRank('MAJOR')).toBeGreaterThan(severityRank('WARNING'))
    expect(severityRank('WARNING')).toBeGreaterThan(severityRank('INFO'))
    const ordered = sortIssues([infoCaseFixture, warningCaseFixture, acknowledgedCaseFixture, caseFixture])
    expect(ordered.map((item) => item.severity)).toEqual(['CRITICAL', 'MAJOR', 'WARNING', 'INFO'])
  })

  it('breaks severity ties with later lastObservedAt first', () => {
    const earlier = { ...caseFixture, id: 'earlier', lastObservedAt: '2026-01-01T00:00:00Z' }
    const later = { ...caseFixture, id: 'later', lastObservedAt: '2026-01-02T00:00:00Z' }
    expect(compareIssues(later, earlier)).toBeLessThan(0)
    expect(sortIssues([earlier, later]).map((item) => item.id)).toEqual(['later', 'earlier'])
  })

  it('counts sites and cells from inventory lists', () => {
    const summary = networkOperationsSummary(
      [siteFixture, unlocatedSiteFixture],
      inventoryCells,
      mixedCases,
      { sites: true, cells: true, cases: true },
    )
    expect(summary.sites).toBe(2)
    expect(summary.cells).toBe(2)
  })

  it('counts active and critical-active cases from loaded Assurance', () => {
    const summary = networkOperationsSummary([siteFixture], inventoryCells, mixedCases, {
      sites: true,
      cells: true,
      cases: true,
    })
    expect(summary.activeCases).toBe(4)
    expect(summary.criticalActive).toBe(1)
    expect(criticalActiveCount(mixedCases)).toBe(1)
  })

  it('counts distinct cells needing attention', () => {
    expect(cellsNeedingAttention(mixedCases).sort()).toEqual(['CELL-001', 'CELL-002'])
    const summary = networkOperationsSummary([siteFixture], inventoryCells, mixedCases, {
      sites: true,
      cells: true,
      cases: true,
    })
    expect(summary.cellsNeedingAttention).toBe(2)
  })

  it('joins an affected cell to its site', () => {
    expect(siteIdForCell('CELL-001', inventoryCells)).toBe('SITE-001')
  })

  it('does not fabricate a site for an unknown cell', () => {
    expect(siteIdForCell('CELL-UNKNOWN', inventoryCells)).toBeNull()
    expect(siteAttention('SITE-001', inventoryCells, [unknownCellCaseFixture]).activeCount).toBe(0)
  })

  it('derives site attention from active cases on site cells', () => {
    const attention = siteAttention('SITE-001', inventoryCells, mixedCases)
    expect(attention.activeCount).toBe(4)
    expect(attention.cellIds.sort()).toEqual(['CELL-001', 'CELL-002'])
  })

  it('uses the highest active severity for a site', () => {
    expect(siteAttention('SITE-001', inventoryCells, mixedCases).highestSeverity).toBe('CRITICAL')
    expect(siteAttention('SITE-001', inventoryCells, [warningCaseFixture, acknowledgedCaseFixture]).highestSeverity).toBe(
      'MAJOR',
    )
  })

  it('excludes resolved cases from site map attention', () => {
    const attention = siteAttention('SITE-001', inventoryCells, [resolvedCaseFixture])
    expect(attention.activeCount).toBe(0)
    expect(attention.highestSeverity).toBeNull()
  })

  it('filters the active queue by status', () => {
    const openOnly = filterIssues(mixedCases, 'OPEN', 'ALL', null, inventoryCells)
    expect(openOnly.every((item) => item.status === 'OPEN')).toBe(true)
    expect(openOnly.map((item) => item.id)).not.toContain(acknowledgedCaseFixture.id)
    expect(openOnly.map((item) => item.id)).not.toContain(resolvedCaseFixture.id)
  })

  it('filters the active queue by severity', () => {
    const critical = filterIssues(mixedCases, 'ACTIVE', 'CRITICAL', null, inventoryCells)
    expect(critical).toEqual([caseFixture])
  })

  it('applies combined status, severity, and site filters', () => {
    const combined = filterIssues(mixedCases, 'OPEN', 'WARNING', 'SITE-001', inventoryCells)
    expect(combined).toEqual([warningCaseFixture])
  })

  it('returns zeros for an empty loaded Assurance collection', () => {
    const summary = networkOperationsSummary([siteFixture], inventoryCells, [], {
      sites: true,
      cells: true,
      cases: true,
    })
    expect(summary.activeCases).toBe(0)
    expect(summary.criticalActive).toBe(0)
    expect(summary.cellsNeedingAttention).toBe(0)
  })

  it('returns UNAVAILABLE for Assurance metrics when Assurance failed', () => {
    const summary = networkOperationsSummary([siteFixture], inventoryCells, [], {
      sites: true,
      cells: true,
      cases: false,
    })
    expect(summary.activeCases).toBe(UNAVAILABLE)
    expect(summary.criticalActive).toBe(UNAVAILABLE)
    expect(summary.cellsNeedingAttention).toBe(UNAVAILABLE)
    expect(summary.sites).toBe(1)
    expect(summary.cells).toBe(2)
  })

  it('returns UNAVAILABLE for inventory totals when inventory failed', () => {
    const summary = networkOperationsSummary([], [], mixedCases, {
      sites: false,
      cells: false,
      cases: true,
    })
    expect(summary.sites).toBe(UNAVAILABLE)
    expect(summary.cells).toBe(UNAVAILABLE)
    expect(summary.activeCases).toBe(4)
  })

  it('translates DEGRADING_RADIO_QUALITY for operators', () => {
    expect(issueTypeLabel('DEGRADING_RADIO_QUALITY')).toBe('Degrading radio quality')
  })
})
