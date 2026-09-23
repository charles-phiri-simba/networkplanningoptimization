import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import { AskSnip } from '../features/ai/AskSnip'
import { AssuranceList } from '../features/assurance/AssuranceList'
import { ConfigurationPanel } from '../features/cell/ConfigurationPanel'
import { NeighbourPanel } from '../features/cell/NeighbourPanel'
import { KpiPanel } from '../features/telemetry/KpiPanel'
import type { AssuranceCaseDto } from '../types/assurance'
import type { CellContextDto } from '../types/network'
import { findTxPower } from '../features/optimization/txPower'
import { formatBandwidthMhz } from '../utils/format'

export function CellPage() {
  const { cellId = '' } = useParams()
  const [context, setContext] = useState<CellContextDto | null>(null)
  const [cases, setCases] = useState<AssuranceCaseDto[] | null>(null)
  const [contextError, setContextError] = useState<unknown>(null)
  const [assuranceError, setAssuranceError] = useState<unknown>(null)

  function loadContext() {
    setContextError(null)
    setContext(null)
    snipApi.getCellContext(cellId).then(setContext).catch(setContextError)
  }

  function loadAssurance() {
    setAssuranceError(null)
    setCases(null)
    snipApi.getAssuranceForCell(cellId).then(setCases).catch(setAssuranceError)
  }

  useEffect(() => {
    loadContext()
    loadAssurance()
  }, [cellId])

  if (contextError) {
    return <ErrorState error={contextError} onRetry={loadContext} />
  }
  if (!context) {
    return <LoadingState label="Loading cell workspace…" />
  }

  const { cell, gnb, site, provenance } = context
  const txPower = findTxPower(context.radioConfiguration)

  return (
    <div className="page">
      <p className="crumb">
        <Link to="/network">Network</Link> /{' '}
        <Link to={`/network/sites/${encodeURIComponent(site.siteId)}`}>{site.siteId}</Link> / {cell.cellId}
      </p>
      <header className="page-header">
        <div>
          <h1>{cell.name}</h1>
          <p className="muted">
            {cell.cellId} · {site.name} · {gnb.name}
          </p>
        </div>
        <StatusBadge status={cell.status} />
      </header>
      {txPower ? (
        <p>
          <Link
            className="btn"
            to={`/network/cells/${encodeURIComponent(cell.cellId)}/optimize`}
          >
            Propose txPower optimization
            {txPower.parameterValue ? ` (${txPower.parameterValue} ${txPower.unit ?? 'dBm'})` : ''}
          </Link>
        </p>
      ) : null}
      {provenance.synthetic ? (
        <p className="banner-demo">This cell context is synthetic / demo data ({provenance.source}).</p>
      ) : null}

      <dl className="kv">
        <div>
          <dt>Site</dt>
          <dd>{site.siteId}</dd>
        </div>
        <div>
          <dt>gNB</dt>
          <dd>{gnb.gnbId}</dd>
        </div>
        <div>
          <dt>Vendor</dt>
          <dd>{gnb.vendor}</dd>
        </div>
        <div>
          <dt>Model</dt>
          <dd>{gnb.model}</dd>
        </div>
        <div>
          <dt>Technology</dt>
          <dd>{cell.technology}</dd>
        </div>
        <div>
          <dt>Band</dt>
          <dd>{cell.band}</dd>
        </div>
        <div>
          <dt>ARFCN</dt>
          <dd>{cell.arfcn ?? '—'}</dd>
        </div>
        <div>
          <dt>PCI</dt>
          <dd>{cell.pci ?? '—'}</dd>
        </div>
        <div>
          <dt>Duplex</dt>
          <dd>{cell.duplexMode}</dd>
        </div>
        <div>
          <dt>Bandwidth</dt>
          <dd>{formatBandwidthMhz(cell.bandwidthMhz)}</dd>
        </div>
        <div>
          <dt>Provenance</dt>
          <dd>
            {provenance.source}
            {provenance.synthetic ? ' · synthetic/demo' : ''}
          </dd>
        </div>
      </dl>

      <ConfigurationPanel parameters={context.radioConfiguration} provenance={provenance} />
      <KpiPanel kpis={context.kpis} telemetry={context.telemetry} />
      <NeighbourPanel neighbours={context.neighbours} />

      <section className="panel" aria-labelledby="cell-assurance-heading">
        <header className="panel-header">
          <h2 id="cell-assurance-heading">Assurance</h2>
          <p className="muted">Findings for this cell. Recommended checks are not automatic actions.</p>
        </header>
        {assuranceError ? (
          <ErrorState error={assuranceError} onRetry={loadAssurance} />
        ) : !cases ? (
          <LoadingState label="Loading assurance…" />
        ) : (
          <AssuranceList cases={cases} />
        )}
      </section>

      <AskSnip cellId={cell.cellId} />
    </div>
  )
}
