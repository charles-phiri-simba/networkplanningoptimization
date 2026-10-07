import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import { issueSeverityLabel } from '../features/operations/issueLabels'
import { SiteCellComparison } from '../features/network/SiteCellComparison'
import { attentionPhrase, siteAttention } from '../features/operations/operationsModel'
import type { AssuranceCaseDto } from '../types/assurance'
import type { CellDto, GnbDto, SiteDto } from '../types/network'
import { planningCreateHref } from '../features/planning/planningGuards'
import { formatCoordinate } from '../utils/format'

export function SitePage() {
  const { siteId = '' } = useParams()
  const [site, setSite] = useState<SiteDto | null>(null)
  const [cells, setCells] = useState<CellDto[] | null>(null)
  const [gnbs, setGnbs] = useState<GnbDto[] | null>(null)
  const [cases, setCases] = useState<AssuranceCaseDto[] | null>(null)
  const [casesError, setCasesError] = useState<unknown>(null)
  const [error, setError] = useState<unknown>(null)

  function loadInventory() {
    setError(null)
    Promise.all([snipApi.getSite(siteId), snipApi.listCells(), snipApi.listGnbs()])
      .then(([nextSite, nextCells, nextGnbs]) => {
        setSite(nextSite)
        setCells(nextCells)
        setGnbs(nextGnbs)
      })
      .catch(setError)
  }

  function loadCases() {
    setCasesError(null)
    setCases(null)
    snipApi.listAssuranceCases().then(setCases).catch(setCasesError)
  }

  function load() {
    loadInventory()
    loadCases()
  }

  useEffect(() => {
    load()
  }, [siteId])

  const siteCells = useMemo(
    () => (cells ?? []).filter((cell) => cell.siteId === siteId),
    [cells, siteId],
  )
  const siteGnbs = useMemo(
    () => (gnbs ?? []).filter((gnb) => gnb.siteId === siteId),
    [gnbs, siteId],
  )
  const attention = useMemo(
    () => (cases ? siteAttention(siteId, siteCells, cases) : null),
    [cases, siteCells, siteId],
  )

  if (error) {
    return <ErrorState error={error} onRetry={load} />
  }
  if (!site || !cells || !gnbs) {
    return <LoadingState label="Loading site…" />
  }

  return (
    <div className="page">
      <p className="crumb">
        <Link to="/network">Network</Link> / {site.siteId}
      </p>
      <header className="page-header">
        <div>
          <h1>{site.name}</h1>
          <p className="muted">{site.siteId}</p>
        </div>
        <StatusBadge status={site.status} />
      </header>
      <p>
        <Link className="btn" to={planningCreateHref(siteCells.map((cell) => cell.cellId))}>
          Create what-if scenario
        </Link>
      </p>
      <dl className="kv">
        <div>
          <dt>Latitude</dt>
          <dd>{formatCoordinate(site.latitude)}</dd>
        </div>
        <div>
          <dt>Longitude</dt>
          <dd>{formatCoordinate(site.longitude)}</dd>
        </div>
        <div>
          <dt>Inventory status</dt>
          <dd>{site.status}</dd>
        </div>
        <div>
          <dt>Cells</dt>
          <dd>{siteCells.length}</dd>
        </div>
        <div>
          <dt>Cells needing attention</dt>
          <dd>
            {casesError ? 'Unavailable' : attention ? attention.cellIds.length : '—'}
          </dd>
        </div>
        <div>
          <dt>Active Assurance cases</dt>
          <dd>
            {casesError ? 'Unavailable' : attention ? attention.activeCount : '—'}
          </dd>
        </div>
        <div>
          <dt>Highest active severity</dt>
          <dd>
            {casesError
              ? 'Unavailable'
              : attention
                ? attention.highestSeverity
                  ? `${attentionPhrase(attention.highestSeverity)} (${issueSeverityLabel(attention.highestSeverity)})`
                  : attentionPhrase(null)
                : '—'}
          </dd>
        </div>
      </dl>

      <section className="panel" aria-labelledby="gnb-heading">
        <h2 id="gnb-heading">Associated gNBs</h2>
        {siteGnbs.length === 0 ? (
          <EmptyState title="No gNBs at this site" />
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">gNB</th>
                <th scope="col">Vendor</th>
                <th scope="col">Model</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              {siteGnbs.map((gnb) => (
                <tr key={gnb.gnbId}>
                  <td>
                    {gnb.name} <span className="muted">{gnb.gnbId}</span>
                  </td>
                  <td>{gnb.vendor}</td>
                  <td>{gnb.model}</td>
                  <td>
                    <StatusBadge status={gnb.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel" aria-labelledby="cell-heading">
        <h2 id="cell-heading">Associated cells</h2>
        {siteCells.length === 0 ? (
          <EmptyState title="No cells at this site" />
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Cell</th>
                <th scope="col">Technology</th>
                <th scope="col">Band</th>
                <th scope="col">Status</th>
                <th scope="col" />
              </tr>
            </thead>
            <tbody>
              {siteCells.map((cell) => (
                <tr key={cell.cellId}>
                  <td>
                    {cell.name} <span className="muted">{cell.cellId}</span>
                  </td>
                  <td>{cell.technology}</td>
                  <td>{cell.band}</td>
                  <td>
                    <StatusBadge status={cell.status} />
                  </td>
                  <td>
                    <Link to={`/network/cells/${encodeURIComponent(cell.cellId)}`}>Open cell</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <SiteCellComparison
        siteId={site.siteId}
        cells={siteCells}
        cases={cases}
        casesUnavailable={Boolean(casesError)}
      />
    </div>
  )
}
