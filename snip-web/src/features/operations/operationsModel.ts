import type { AssuranceCaseDto, AssuranceEvidenceDto } from '../../types/assurance'
import type { CellDto, SiteDto } from '../../types/network'

export const UNAVAILABLE = 'UNAVAILABLE' as const
export type CountOrUnavailable = number | typeof UNAVAILABLE

export const ACTIVE_STATUSES = ['OPEN', 'ACKNOWLEDGED'] as const

export type StatusFilter = 'ACTIVE' | 'OPEN' | 'ACKNOWLEDGED'
export type SeverityFilter = 'ALL' | 'CRITICAL' | 'MAJOR' | 'WARNING' | 'INFO'
export type SortKey = 'SEVERITY' | 'LAST_OBSERVED'

const SEVERITY_RANK: Record<string, number> = {
  CRITICAL: 4,
  MAJOR: 3,
  WARNING: 2,
  INFO: 1,
}

export function isActiveAssuranceCase(item: AssuranceCaseDto): boolean {
  return item.affectedEntityType === 'CELL' && (item.status === 'OPEN' || item.status === 'ACKNOWLEDGED')
}

export function severityRank(severity: string | null | undefined): number {
  if (!severity) {
    return 0
  }
  return SEVERITY_RANK[severity.toUpperCase()] ?? 0
}

function observedMillis(value: string | null | undefined): number {
  if (!value) {
    return Number.NEGATIVE_INFINITY
  }
  const time = Date.parse(value)
  return Number.isNaN(time) ? Number.NEGATIVE_INFINITY : time
}

export function compareIssues(left: AssuranceCaseDto, right: AssuranceCaseDto): number {
  const severity = severityRank(right.severity) - severityRank(left.severity)
  if (severity !== 0) {
    return severity
  }
  const time = observedMillis(right.lastObservedAt) - observedMillis(left.lastObservedAt)
  if (time !== 0) {
    return time
  }
  return left.id.localeCompare(right.id)
}

export function activeCases(cases: AssuranceCaseDto[]): AssuranceCaseDto[] {
  return cases.filter(isActiveAssuranceCase)
}

export function cellsNeedingAttention(cases: AssuranceCaseDto[]): string[] {
  return [...new Set(activeCases(cases).map((item) => item.affectedEntityId))]
}

export function criticalActiveCount(cases: AssuranceCaseDto[]): number {
  return activeCases(cases).filter((item) => item.severity === 'CRITICAL').length
}

export function siteIdForCell(cellId: string, cells: CellDto[]): string | null {
  return cells.find((cell) => cell.cellId === cellId)?.siteId ?? null
}

export function siteAttention(
  siteId: string,
  cells: CellDto[],
  cases: AssuranceCaseDto[],
): {
  activeCount: number
  cellIds: string[]
  highestSeverity: string | null
} {
  const siteCellIds = new Set(cells.filter((cell) => cell.siteId === siteId).map((cell) => cell.cellId))
  const relevant = activeCases(cases).filter((item) => siteCellIds.has(item.affectedEntityId))
  const cellIds = [...new Set(relevant.map((item) => item.affectedEntityId))]
  let highestSeverity: string | null = null
  let highestRank = 0
  for (const item of relevant) {
    const rank = severityRank(item.severity)
    if (rank > highestRank) {
      highestRank = rank
      highestSeverity = item.severity
    }
  }
  return {
    activeCount: relevant.length,
    cellIds,
    highestSeverity,
  }
}

export function networkOperationsSummary(
  sites: SiteDto[],
  cells: CellDto[],
  cases: AssuranceCaseDto[],
  loaded: { sites: boolean; cells: boolean; cases: boolean },
): {
  sites: CountOrUnavailable
  cells: CountOrUnavailable
  activeCases: CountOrUnavailable
  criticalActive: CountOrUnavailable
  cellsNeedingAttention: CountOrUnavailable
} {
  return {
    sites: loaded.sites ? sites.length : UNAVAILABLE,
    cells: loaded.cells ? cells.length : UNAVAILABLE,
    activeCases: loaded.cases ? activeCases(cases).length : UNAVAILABLE,
    criticalActive: loaded.cases ? criticalActiveCount(cases) : UNAVAILABLE,
    cellsNeedingAttention: loaded.cases ? cellsNeedingAttention(cases).length : UNAVAILABLE,
  }
}

export function filterIssues(
  cases: AssuranceCaseDto[],
  status: StatusFilter,
  severity: SeverityFilter,
  siteId: string | null,
  cells: CellDto[] | null,
): AssuranceCaseDto[] {
  return cases.filter((item) => {
    if (!isActiveAssuranceCase(item)) {
      return false
    }
    if (status === 'OPEN' && item.status !== 'OPEN') {
      return false
    }
    if (status === 'ACKNOWLEDGED' && item.status !== 'ACKNOWLEDGED') {
      return false
    }
    if (severity !== 'ALL' && item.severity !== severity) {
      return false
    }
    if (siteId) {
      if (!cells) {
        return false
      }
      return siteIdForCell(item.affectedEntityId, cells) === siteId
    }
    return true
  })
}

export function sortIssues(cases: AssuranceCaseDto[], sort: SortKey = 'SEVERITY'): AssuranceCaseDto[] {
  const copy = [...cases]
  if (sort === 'LAST_OBSERVED') {
    copy.sort((left, right) => {
      const time = observedMillis(right.lastObservedAt) - observedMillis(left.lastObservedAt)
      if (time !== 0) {
        return time
      }
      return compareIssues(left, right)
    })
    return copy
  }
  copy.sort(compareIssues)
  return copy
}

export function evidencePreview(item: AssuranceCaseDto): AssuranceEvidenceDto | null {
  return item.evidence.find((row) => row.metric) ?? item.evidence[0] ?? null
}

export function attentionPhrase(highestSeverity: string | null): string {
  if (highestSeverity === 'CRITICAL') {
    return 'Critical Assurance finding'
  }
  if (highestSeverity) {
    return 'Needs attention'
  }
  return 'No active Assurance findings'
}
