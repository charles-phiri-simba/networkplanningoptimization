import { describe, expect, it } from 'vitest'
import {
  optimizeHref,
  planningCreateHref,
  suggestedPlanningCells,
  validatePlanningDraft,
  validateTxPower,
} from './planningGuards'

describe('planningGuards', () => {
  it('caps suggested cells at 4', () => {
    expect(suggestedPlanningCells(['CELL-001', 'CELL-002', 'CELL-003', 'CELL-004', 'CELL-005'])).toEqual([
      'CELL-001',
      'CELL-002',
      'CELL-003',
      'CELL-004',
    ])
  })

  it('builds create and optimize hrefs', () => {
    expect(planningCreateHref(['CELL-001', 'CELL-002'])).toBe('/planning/new?cells=CELL-001,CELL-002')
    expect(optimizeHref('CELL-001')).toBe('/network/cells/CELL-001/optimize')
  })

  it('validates txPower range and draft bounds', () => {
    expect(validateTxPower(19)).toMatch(/20/)
    expect(validateTxPower(51)).toMatch(/50/)
    expect(validateTxPower(44)).toBeNull()
    expect(
      validatePlanningDraft({
        cells: ['CELL-001'],
        alternativeCount: 1,
        intentCount: 1,
        maxIntentsInOneAlternative: 1,
      }),
    ).toBeNull()
    expect(
      validatePlanningDraft({
        cells: ['A', 'B', 'C', 'D', 'E'],
        alternativeCount: 1,
        intentCount: 1,
        maxIntentsInOneAlternative: 1,
      }),
    ).toMatch(/4 cells/)
  })
})
