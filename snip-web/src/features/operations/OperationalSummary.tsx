import type { AssuranceCaseDto } from '../../types/assurance'
import type { CellDto, SiteDto } from '../../types/network'
import {
  attentionPhrase,
  networkOperationsSummary,
  siteAttention,
  siteIdForCell,
  UNAVAILABLE,
} from './operationsModel'
import { issueSeverityLabel } from './issueLabels'

function displayCount(value: number | typeof UNAVAILABLE, settled: boolean): string {
  if (!settled) {
    return '—'
  }
  return value === UNAVAILABLE ? 'Unavailable' : String(value)
}

export function OperationalSummary({
  sites,
  cells,
  cases,
  loaded,
  settled,
  gnbCount,
}: {
  sites: SiteDto[]
  cells: CellDto[]
  cases: AssuranceCaseDto[]
  loaded: { sites: boolean; cells: boolean; cases: boolean }
  settled: { sites: boolean; cells: boolean; cases: boolean }
  gnbCount: number | typeof UNAVAILABLE | null
}) {
  const summary = networkOperationsSummary(sites, cells, cases, loaded)
  return (
    <dl className="summary ops-summary">
      <div>
        <dt>Sites</dt>
        <dd>{displayCount(summary.sites, settled.sites)}</dd>
      </div>
      {gnbCount !== null ? (
        <div>
          <dt>gNBs</dt>
          <dd>{displayCount(gnbCount, true)}</dd>
        </div>
      ) : null}
      <div>
        <dt>Cells</dt>
        <dd>{displayCount(summary.cells, settled.cells)}</dd>
      </div>
      <div>
        <dt>Active Assurance cases</dt>
        <dd>{displayCount(summary.activeCases, settled.cases)}</dd>
      </div>
      <div>
        <dt>Critical active cases</dt>
        <dd>{displayCount(summary.criticalActive, settled.cases)}</dd>
      </div>
      <div>
        <dt>Cells needing attention</dt>
        <dd>{displayCount(summary.cellsNeedingAttention, settled.cases)}</dd>
      </div>
    </dl>
  )
}

export function SiteAttentionNote({
  siteId,
  cells,
  cases,
  cellsLoaded,
  casesLoaded,
}: {
  siteId: string
  cells: CellDto[]
  cases: AssuranceCaseDto[]
  cellsLoaded: boolean
  casesLoaded: boolean
}) {
  if (!casesLoaded || !cellsLoaded) {
    return <p className="muted">Assurance attention unavailable</p>
  }
  const attention = siteAttention(siteId, cells, cases)
  return (
    <p className="muted">
      {attentionPhrase(attention.highestSeverity)}
      {attention.activeCount > 0
        ? ` · ${attention.activeCount} active · ${issueSeverityLabel(attention.highestSeverity ?? '')}`
        : ''}
    </p>
  )
}

export function siteLabelForCell(
  cellId: string,
  cells: CellDto[] | null,
  sites: SiteDto[],
): string {
  if (!cells) {
    return 'Unavailable'
  }
  const siteId = siteIdForCell(cellId, cells)
  if (!siteId) {
    return 'Unknown'
  }
  return sites.find((site) => site.siteId === siteId)?.name ?? siteId
}
