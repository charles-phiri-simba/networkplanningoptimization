import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import { SiteMap } from '../features/map/SiteMap'
import { AssuranceIssueQueue } from '../features/operations/AssuranceIssueQueue'
import { OperationalSummary, SiteAttentionNote } from '../features/operations/OperationalSummary'
import { UNAVAILABLE } from '../features/operations/operationsModel'
import type { AssuranceCaseDto } from '../types/assurance'
import type { CellDto, GnbDto, SiteDto } from '../types/network'
import { formatCoordinate } from '../utils/format'

interface Partition<T> {
  data: T | null
  error: unknown
  settled: boolean
}

function idlePartition<T>(): Partition<T> {
  return { data: null, error: null, settled: false }
}

export function NetworkPage() {
  const [sites, setSites] = useState<Partition<SiteDto[]>>(idlePartition)
  const [cells, setCells] = useState<Partition<CellDto[]>>(idlePartition)
  const [gnbs, setGnbs] = useState<Partition<GnbDto[]>>(idlePartition)
  const [cases, setCases] = useState<Partition<AssuranceCaseDto[]>>(idlePartition)

  function loadSites() {
    setSites(idlePartition())
    snipApi
      .listSites()
      .then((data) => setSites({ data, error: null, settled: true }))
      .catch((error) => setSites({ data: null, error, settled: true }))
  }

  function loadCells() {
    setCells(idlePartition())
    snipApi
      .listCells()
      .then((data) => setCells({ data, error: null, settled: true }))
      .catch((error) => setCells({ data: null, error, settled: true }))
  }

  function loadGnbs() {
    setGnbs(idlePartition())
    snipApi
      .listGnbs()
      .then((data) => setGnbs({ data, error: null, settled: true }))
      .catch((error) => setGnbs({ data: null, error, settled: true }))
  }

  function loadCases() {
    setCases(idlePartition())
    snipApi
      .listAssuranceCases()
      .then((data) => setCases({ data, error: null, settled: true }))
      .catch((error) => setCases({ data: null, error, settled: true }))
  }

  function load() {
    loadSites()
    loadCells()
    loadGnbs()
    loadCases()
  }

  useEffect(() => {
    load()
  }, [])

  const anySettled = sites.settled || cells.settled || gnbs.settled || cases.settled
  const coreFailed = sites.settled && cells.settled && cases.settled && sites.error && cells.error && cases.error

  if (!anySettled) {
    return <LoadingState label="Loading network operations…" />
  }

  if (coreFailed) {
    return <ErrorState error={sites.error ?? cells.error ?? cases.error} onRetry={load} />
  }

  const siteRows = sites.data ?? []
  const cellRows = cells.data ?? []
  const caseRows = cases.data ?? []

  return (
    <div className="workspace workspace-operations">
      <aside className="workspace-side">
        <h1>Network operations</h1>
        <p className="muted">Operational summary of inventory and active Assurance findings.</p>
        <OperationalSummary
          sites={siteRows}
          cells={cellRows}
          cases={caseRows}
          loaded={{ sites: Boolean(sites.data), cells: Boolean(cells.data), cases: Boolean(cases.data) }}
          settled={{ sites: sites.settled, cells: cells.settled, cases: cases.settled }}
          gnbCount={gnbs.settled ? (gnbs.data ? gnbs.data.length : UNAVAILABLE) : null}
        />
        {sites.error ? <ErrorState error={sites.error} onRetry={loadSites} /> : null}
        {cells.error ? <ErrorState error={cells.error} onRetry={loadCells} /> : null}
        {sites.data ? (
          sites.data.length === 0 ? (
            <EmptyState title="No sites" />
          ) : (
            <ul className="nav-list">
              {sites.data.map((site) => (
                <li key={site.siteId}>
                  <Link to={`/network/sites/${encodeURIComponent(site.siteId)}`}>
                    <strong>{site.name}</strong>
                    <span className="muted">{site.siteId}</span>
                    <StatusBadge status={site.status} />
                  </Link>
                  <p className="muted">
                    {formatCoordinate(site.latitude)}, {formatCoordinate(site.longitude)}
                  </p>
                  <SiteAttentionNote
                    siteId={site.siteId}
                    cells={cellRows}
                    cases={caseRows}
                    cellsLoaded={Boolean(cells.data)}
                    casesLoaded={Boolean(cases.data)}
                  />
                </li>
              ))}
            </ul>
          )
        ) : null}
      </aside>
      <div className="workspace-map">
        {sites.data ? (
          <SiteMap sites={sites.data} cells={cells.data} cases={cases.data} />
        ) : sites.error ? (
          <div className="map-empty" role="status">
            Site map unavailable.
          </div>
        ) : (
          <div className="map-empty" role="status">
            Loading map…
          </div>
        )}
      </div>
      <div className="workspace-queue">
        <AssuranceIssueQueue
          cases={cases.data}
          cells={cells.data}
          sites={siteRows}
          casesError={cases.error}
          onRetryCases={loadCases}
        />
      </div>
    </div>
  )
}
