import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import { SiteMap } from '../features/map/SiteMap'
import type { CellDto, GnbDto, SiteDto } from '../types/network'
import { formatCoordinate } from '../utils/format'

export function NetworkPage() {
  const [sites, setSites] = useState<SiteDto[] | null>(null)
  const [cells, setCells] = useState<CellDto[] | null>(null)
  const [gnbs, setGnbs] = useState<GnbDto[] | null>(null)
  const [error, setError] = useState<unknown>(null)

  function load() {
    setError(null)
    Promise.all([snipApi.listSites(), snipApi.listCells(), snipApi.listGnbs()])
      .then(([nextSites, nextCells, nextGnbs]) => {
        setSites(nextSites)
        setCells(nextCells)
        setGnbs(nextGnbs)
      })
      .catch(setError)
  }

  useEffect(() => {
    load()
  }, [])

  if (error) {
    return <ErrorState error={error} onRetry={load} />
  }
  if (!sites || !cells || !gnbs) {
    return <LoadingState label="Loading network inventory…" />
  }

  return (
    <div className="workspace">
      <aside className="workspace-side">
        <h1>Network</h1>
        <dl className="summary">
          <div>
            <dt>Sites</dt>
            <dd>{sites.length}</dd>
          </div>
          <div>
            <dt>gNBs</dt>
            <dd>{gnbs.length}</dd>
          </div>
          <div>
            <dt>Cells</dt>
            <dd>{cells.length}</dd>
          </div>
        </dl>
        {sites.length === 0 ? (
          <EmptyState title="No sites" />
        ) : (
          <ul className="nav-list">
            {sites.map((site) => (
              <li key={site.siteId}>
                <Link to={`/network/sites/${encodeURIComponent(site.siteId)}`}>
                  <strong>{site.name}</strong>
                  <span className="muted">{site.siteId}</span>
                  <StatusBadge status={site.status} />
                </Link>
                <p className="muted">
                  {formatCoordinate(site.latitude)}, {formatCoordinate(site.longitude)}
                </p>
              </li>
            ))}
          </ul>
        )}
      </aside>
      <div className="workspace-map">
        <SiteMap sites={sites} />
      </div>
    </div>
  )
}
