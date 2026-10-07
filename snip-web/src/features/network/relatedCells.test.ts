import { describe, expect, it } from 'vitest'
import type { CellDto } from '../../types/network'
import {
  RELATED_CELL_CONTEXT_LIMIT,
  Relationship,
  knownNeighbourTargets,
  loadBoundedCellContexts,
  relationshipLabel,
  relationshipsForCell,
  selectRelatedCellIds,
  selectSiteComparisonCellIds,
} from './relatedCells'

function cell(cellId: string, gnbId: string, siteId: string): CellDto {
  return {
    cellId,
    name: cellId,
    gnbId,
    siteId,
    technology: 'NR',
    band: 'n78',
    arfcn: 1,
    pci: 1,
    bandwidthMhz: 40,
    duplexMode: 'TDD',
    status: 'ACTIVE',
  }
}

const selected = cell('CELL-001', 'GNB-001', 'SITE-001')
const sameGnb = cell('CELL-002', 'GNB-001', 'SITE-001')
const sameSiteOtherGnb = cell('CELL-010', 'GNB-010', 'SITE-001')
const crossSiteNeighbour = cell('CELL-003', 'GNB-002', 'SITE-002')
const far = cell('CELL-099', 'GNB-099', 'SITE-099')

describe('related cell derivation', () => {
  it('identifies same-site, same-gNB, neighbour, and cross-site neighbour', () => {
    const outgoing = new Set(['CELL-002', 'CELL-003'])
    expect(relationshipsForCell(selected, selected, outgoing)).toEqual([Relationship.SELECTED])
    expect(relationshipsForCell(sameGnb, selected, outgoing)).toEqual([
      Relationship.SAME_GNB,
      Relationship.SAME_SITE,
      Relationship.CONFIGURED_NEIGHBOUR,
    ])
    expect(relationshipsForCell(sameSiteOtherGnb, selected, outgoing)).toEqual([Relationship.SAME_SITE])
    expect(relationshipsForCell(crossSiteNeighbour, selected, outgoing)).toEqual([
      Relationship.CONFIGURED_NEIGHBOUR,
      Relationship.CROSS_SITE_NEIGHBOUR,
    ])
    expect(relationshipsForCell(far, selected, outgoing)).toEqual([])
  })

  it('does not invent a reverse neighbour', () => {
    const outgoingFromSelected = new Set(['CELL-003'])
    expect(relationshipsForCell(selected, crossSiteNeighbour, outgoingFromSelected)).not.toContain(
      Relationship.CONFIGURED_NEIGHBOUR,
    )
  })

  it('uses truthful relationship labels without interference wording', () => {
    expect(relationshipLabel(Relationship.CONFIGURED_NEIGHBOUR)).toBe('Configured neighbour')
    expect(relationshipLabel(Relationship.CROSS_SITE_NEIGHBOUR)).toBe('Cross-site neighbour')
    const labels = Object.values(Relationship).map(relationshipLabel).join(' ')
    expect(labels).not.toMatch(/interference|coverage overlap|handover quality/i)
  })
})

describe('related cell selection', () => {
  it('includes the selected cell and inventory-known neighbours only', () => {
    const result = selectRelatedCellIds(
      'CELL-001',
      [selected, sameGnb, crossSiteNeighbour, far],
      ['CELL-003', 'CELL-UNKNOWN'],
    )
    expect(result.ids[0]).toBe('CELL-001')
    expect(result.ids).toContain('CELL-002')
    expect(result.ids).toContain('CELL-003')
    expect(result.ids).not.toContain('CELL-UNKNOWN')
    expect(result.ids).not.toContain('CELL-099')
  })

  it('prioritizes same-gNB before remaining same-site before neighbours', () => {
    const extraSameGnb = cell('CELL-004', 'GNB-001', 'SITE-001')
    const result = selectRelatedCellIds(
      'CELL-001',
      [selected, sameSiteOtherGnb, extraSameGnb, crossSiteNeighbour],
      ['CELL-003'],
    )
    expect(result.ids).toEqual(['CELL-001', 'CELL-004', 'CELL-010', 'CELL-003'])
  })

  it('deduplicates cells that are both same-gNB and neighbours', () => {
    const result = selectRelatedCellIds('CELL-001', [selected, sameGnb], ['CELL-002', 'CELL-002'])
    expect(result.ids.filter((id) => id === 'CELL-002')).toHaveLength(1)
    expect(result.ids).toEqual(['CELL-001', 'CELL-002'])
  })

  it('caps context loading at 12 including the selected cell', () => {
    const many = Array.from({ length: 20 }, (_, index) =>
      cell(`CELL-${String(index + 1).padStart(3, '0')}`, 'GNB-001', 'SITE-001'),
    )
    const result = selectRelatedCellIds('CELL-001', many, ['CELL-015', 'CELL-020'])
    expect(result.ids).toHaveLength(RELATED_CELL_CONTEXT_LIMIT)
    expect(result.ids[0]).toBe('CELL-001')
    expect(result.truncated).toBe(true)
    expect(result.unboundedCount).toBeGreaterThan(12)
    expect(result.ids).not.toContain('CELL-015')
  })

  it('excludes unknown neighbour targets from context loading', () => {
    expect(knownNeighbourTargets([{ targetCellId: 'CELL-002', relationType: 'INTRA_FREQUENCY', status: 'ACTIVE' }, { targetCellId: 'CELL-UNKNOWN', relationType: 'INTER_FREQUENCY', status: 'ACTIVE' }], [selected, sameGnb])).toEqual([
      'CELL-002',
    ])
  })

  it('bounds site comparison to site cells only', () => {
    const result = selectSiteComparisonCellIds('SITE-001', [selected, sameGnb, crossSiteNeighbour, far])
    expect(result.ids).toEqual(['CELL-001', 'CELL-002'])
    expect(result.truncated).toBe(false)
  })

  it('keeps successful contexts when one related context fails', async () => {
    const result = await loadBoundedCellContexts(['CELL-001', 'CELL-002', 'CELL-003'], async (id) => {
      if (id === 'CELL-002') {
        throw new Error('context failed')
      }
      return { cell: { cellId: id } } as never
    })
    expect(result.contexts['CELL-001']).toBeTruthy()
    expect(result.contexts['CELL-003']).toBeTruthy()
    expect(result.contexts['CELL-002']).toBeUndefined()
    expect(result.failures['CELL-002']).toBe(true)
    expect(result.failures['CELL-001']).toBeUndefined()
  })
})
