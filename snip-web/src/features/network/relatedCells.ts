import type { CellContextDto, CellDto, NeighbourDto } from '../../types/network'

export const RELATED_CELL_CONTEXT_LIMIT = 12

export const Relationship = {
  SELECTED: 'SELECTED',
  SAME_GNB: 'SAME_GNB',
  SAME_SITE: 'SAME_SITE',
  CONFIGURED_NEIGHBOUR: 'CONFIGURED_NEIGHBOUR',
  CROSS_SITE_NEIGHBOUR: 'CROSS_SITE_NEIGHBOUR',
} as const

export type RelationshipKind = (typeof Relationship)[keyof typeof Relationship]

const RELATIONSHIP_LABELS: Record<RelationshipKind, string> = {
  SELECTED: 'Selected cell',
  SAME_GNB: 'Same gNB',
  SAME_SITE: 'Same site',
  CONFIGURED_NEIGHBOUR: 'Configured neighbour',
  CROSS_SITE_NEIGHBOUR: 'Cross-site neighbour',
}

const RELATIONSHIP_ORDER: RelationshipKind[] = [
  Relationship.SELECTED,
  Relationship.SAME_GNB,
  Relationship.SAME_SITE,
  Relationship.CONFIGURED_NEIGHBOUR,
  Relationship.CROSS_SITE_NEIGHBOUR,
]

export function relationshipLabel(kind: RelationshipKind): string {
  return RELATIONSHIP_LABELS[kind]
}

export function knownNeighbourTargets(
  neighbours: NeighbourDto[],
  cells: CellDto[],
): string[] {
  const inventory = new Set(cells.map((cell) => cell.cellId))
  return [...new Set(neighbours.map((neighbour) => neighbour.targetCellId))].filter((id) =>
    inventory.has(id),
  )
}

export function relationshipsForCell(
  cell: CellDto,
  selected: CellDto,
  outgoingNeighbourIds: ReadonlySet<string>,
): RelationshipKind[] {
  const labels: RelationshipKind[] = []
  if (cell.cellId === selected.cellId) {
    labels.push(Relationship.SELECTED)
    return labels
  }
  if (cell.gnbId === selected.gnbId) {
    labels.push(Relationship.SAME_GNB)
  }
  if (cell.siteId === selected.siteId) {
    labels.push(Relationship.SAME_SITE)
  }
  if (outgoingNeighbourIds.has(cell.cellId)) {
    labels.push(Relationship.CONFIGURED_NEIGHBOUR)
    if (cell.siteId !== selected.siteId) {
      labels.push(Relationship.CROSS_SITE_NEIGHBOUR)
    }
  }
  return RELATIONSHIP_ORDER.filter((kind) => labels.includes(kind))
}

export function selectRelatedCellIds(
  selectedCellId: string,
  cells: CellDto[],
  neighbourTargetIds: string[],
  limit = RELATED_CELL_CONTEXT_LIMIT,
): { ids: string[]; truncated: boolean; unboundedCount: number } {
  const byId = new Map(cells.map((cell) => [cell.cellId, cell]))
  const selected = byId.get(selectedCellId)
  const selectedGnb = selected?.gnbId
  const selectedSite = selected?.siteId
  const others = cells
    .filter((cell) => cell.cellId !== selectedCellId)
    .slice()
    .sort((left, right) => left.cellId.localeCompare(right.cellId))

  const unbounded: string[] = []
  const unboundedSeen = new Set<string>()
  function consider(id: string) {
    if (!id || unboundedSeen.has(id)) {
      return
    }
    unboundedSeen.add(id)
    unbounded.push(id)
  }

  consider(selectedCellId)
  for (const cell of others) {
    if (selectedGnb && cell.gnbId === selectedGnb) {
      consider(cell.cellId)
    }
  }
  for (const cell of others) {
    if (selectedSite && cell.siteId === selectedSite) {
      consider(cell.cellId)
    }
  }
  for (const id of [...new Set(neighbourTargetIds)].sort((left, right) => left.localeCompare(right))) {
    if (byId.has(id)) {
      consider(id)
    }
  }

  return {
    ids: unbounded.slice(0, limit),
    truncated: unbounded.length > limit,
    unboundedCount: unbounded.length,
  }
}

export function selectSiteComparisonCellIds(
  siteId: string,
  cells: CellDto[],
  limit = RELATED_CELL_CONTEXT_LIMIT,
): { ids: string[]; truncated: boolean; unboundedCount: number } {
  const ids = cells
    .filter((cell) => cell.siteId === siteId)
    .map((cell) => cell.cellId)
    .sort((left, right) => left.localeCompare(right))
  return {
    ids: ids.slice(0, limit),
    truncated: ids.length > limit,
    unboundedCount: ids.length,
  }
}

export async function loadBoundedCellContexts(
  ids: string[],
  load: (id: string) => Promise<CellContextDto>,
): Promise<{ contexts: Record<string, CellContextDto>; failures: Record<string, boolean> }> {
  const results = await Promise.all(
    ids.map((id) =>
      load(id)
        .then((context) => ({ id, context, failed: false as const }))
        .catch(() => ({ id, context: null, failed: true as const })),
    ),
  )
  const contexts: Record<string, CellContextDto> = {}
  const failures: Record<string, boolean> = {}
  for (const result of results) {
    if (result.failed || !result.context) {
      failures[result.id] = true
    } else {
      contexts[result.id] = result.context
    }
  }
  return { contexts, failures }
}
